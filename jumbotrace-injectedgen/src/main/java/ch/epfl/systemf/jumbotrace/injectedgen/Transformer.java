package ch.epfl.systemf.jumbotrace.injectedgen;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.javaparser.ast.type.PrimitiveType.*;

/**
 * Performs the specialization of methods annotated with @Specialize
 * <p>
 * Replicates the methods to generate one copy for each of the topmost types in the type hierarchy
 */
public final class Transformer extends ModifierVisitor<Void> {

    private static final String TARGET_ANNOTATION_NAME = "Specialize";
    private static final String RESTRICT_TO_NUM_TYPES_KEY = "numericOnly";
    private static final String MODIFIED_METH_ANNOTATION_NAME = "Specialized";
    private static final String MODIFIED_METH_ANNOT_FLD_KEY = "typeName";

    /**
     * The numeric primitive types of the Java type system
     */
    private static final List<Type> NUMERIC_TOPMOST_TYPES = List.of(
            intType(), shortType(), longType(), floatType(), doubleType(), charType(), byteType()
    );

    /**
     * The topmost types of the Java types hierachy
     */
    private static final List<Type> TOPMOST_TYPES = Stream.concat(
            NUMERIC_TOPMOST_TYPES.stream(),
            Stream.of(booleanType(), StaticJavaParser.parseType("java.lang.Object"))
    ).toList();

    private static final String RAW_PACKAGE_NAME = "raw";
    private static final String PROCESSED_PACKAGE_NAME = "processed";
    private static final String IMPORT_TO_REMOVE = "ch.epfl.systemf.jumbotrace.injected.annot.Specialize";
    private static final String IMPORT_NAME_TO_ADD = "Specialized";
    private static final String OUTPUT_PRINT_STREAM_NAME = "PRINT_STREAM";

    private final boolean testMode;

    public Transformer(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public Visitable visit(CompilationUnit n, Void arg) {
        super.visit(n, arg);
        replacePackageName(n);
        replaceAnnotationImport(n);
        return n;
    }

    @Override
    public Visitable visit(ClassOrInterfaceDeclaration classOrInterfaceDecl, Void arg) {
        super.visit(classOrInterfaceDecl, arg);
        var newMembersList = new NodeList<BodyDeclaration<?>>();
        for (var member : classOrInterfaceDecl.getMembers()) {
            if (member instanceof MethodDeclaration methodDeclaration
                    && methodDeclaration.getParameters().stream().anyMatch(param -> param.isAnnotationPresent(TARGET_ANNOTATION_NAME))
            ) {
                var maybeAnnot = checkAndDeleteTargetAnnotation(methodDeclaration.getAnnotations());
                var specializeRetType = maybeAnnot != null;
                var restrictToNumericTypes = specializeRetType &&
                        maybeAnnot instanceof NormalAnnotationExpr annot &&
                        annot.getPairs().stream().anyMatch(
                                p -> p.getName().getIdentifier().equals(RESTRICT_TO_NUM_TYPES_KEY) &&
                                        p.getValue() instanceof BooleanLiteralExpr literalExpr && literalExpr.getValue()
                        );
                var replicated = replicate(methodDeclaration, specializeRetType, restrictToNumericTypes);
                newMembersList.addAll(replicated);
            } else {
                newMembersList.add(member);
            }
        }
        classOrInterfaceDecl.setMembers(newMembersList);
        return classOrInterfaceDecl;
    }

    @Override
    public Visitable visit(FieldDeclaration n, Void arg) {
        super.visit(n, arg);
        if (testMode && n.getVariables().size() == 1 && n.getVariables().get(0).getName().getIdentifier().equals(OUTPUT_PRINT_STREAM_NAME)) {
            n.getVariables().get(0).setInitializer(new NullLiteralExpr());
        }
        return n;
    }

    private List<MethodDeclaration> replicate(MethodDeclaration methodDeclaration, boolean specializeReturnType, boolean restrictToNumericTypes) {
        var specializedMethods = new LinkedList<MethodDeclaration>();
        specializedMethods.add(new CommentMethodDeclaration("<editor-fold desc=\"" + methodDeclaration.getName().getIdentifier() + "\">"));
        var types = restrictToNumericTypes ? NUMERIC_TOPMOST_TYPES : TOPMOST_TYPES;
        for (var type : types) {
            specializedMethods.add(copyWithType(methodDeclaration, type, specializeReturnType));
        }
        specializedMethods.add(new CommentMethodDeclaration("</editor-fold>"));
        return specializedMethods;
    }

    private MethodDeclaration copyWithType(MethodDeclaration methodDeclaration, Type type, boolean specializeReturnType) {
        var copy = methodDeclaration.clone();
        if (specializeReturnType) {
            copy.setType(type);
        }
        copy.getParameters().forEach(parameter -> {
            var mustReplicate = checkAndDeleteTargetAnnotation(parameter.getAnnotations()) != null;
            if (mustReplicate) {
                parameter.setType(type);
            }
        });
        copy.getAnnotations().add(new NormalAnnotationExpr(
                new Name(MODIFIED_METH_ANNOTATION_NAME),
                new NodeList<>(new MemberValuePair(MODIFIED_METH_ANNOT_FLD_KEY, new StringLiteralExpr(type.toString())))
        ));
        return copy;
    }

    private AnnotationExpr checkAndDeleteTargetAnnotation(NodeList<AnnotationExpr> annotations) {
        AnnotationExpr found = null;
        for (var annot : annotations) {
            switch (annot.getName().getIdentifier()) {
                case Transformer.MODIFIED_METH_ANNOTATION_NAME ->
                        throw new AssertionError("\"@" + Transformer.MODIFIED_METH_ANNOTATION_NAME + "\" should not be used in input files");
                case Transformer.TARGET_ANNOTATION_NAME -> {
                    if (found == null) {
                        found = annot;
                    } else {
                        throw new AssertionError("\"@" + Transformer.TARGET_ANNOTATION_NAME + "\" should not be repeated");
                    }
                }
            }
        }
        if (found != null) {
            annotations.remove(found);
        }
        return found;
    }

    private static void replacePackageName(CompilationUnit n) {
        n.getPackageDeclaration().ifPresent(packageDeclaration -> {
            var name = packageDeclaration.getName();
            if (!name.getIdentifier().equals(RAW_PACKAGE_NAME)) {
                throw new AssertionError();
            }
            name.setIdentifier(PROCESSED_PACKAGE_NAME);
        });
    }

    private static void replaceAnnotationImport(CompilationUnit n) {
        for (var imp : n.getImports()) {
            if (imp.getName().toString().equals(IMPORT_TO_REMOVE)) {
                imp.getName().setIdentifier(IMPORT_NAME_TO_ADD);
                return;
            }
        }
        throw new AssertionError("import to be replaced not found");
    }

    // Hack: "adapter" to insert a comment inside a list of method declarations
    // Use with care
    private static final class CommentMethodDeclaration extends MethodDeclaration {
        private final Comment comment;

        CommentMethodDeclaration(String comment) {
            this.comment = new LineComment(comment);
        }

        @Override
        public <A> void accept(VoidVisitor<A> v, A arg) {
            comment.accept(v, arg);
        }
    }

}
