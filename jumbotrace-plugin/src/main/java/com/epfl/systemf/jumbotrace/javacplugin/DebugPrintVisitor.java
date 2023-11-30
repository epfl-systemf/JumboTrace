package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import java.io.PrintStream;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DebugPrintVisitor extends TreeTranslator {   // possibly exists better than TreeTranslator for this purpose
    private static final PrintStream s = System.out;

    private void log(JCTree tree) {
        if (!(tree instanceof JCTree.JCMethodInvocation methodInvocation)){  // TODO remove
            return;
        }
//        println("-- START ------------------------------------");
        println(limitLength(tree.toString()));
//        println("-- METHOD INFOS --");
        if (methodInvocation.meth instanceof JCTree.JCFieldAccess fldAccess){
            System.out.println(fldAccess.selected.type.tsym.isDynamic());
        }
//        printAllFields(methodInvocation.meth);
//        println("-- END --------------------------------------");
        println("---------------------------------------------");
    }

    private void printAllFields(Object object) {
        var fields = object.getClass().getFields();
        for (var fld : fields) {
            fld.setAccessible(true);
            try {
                var value = fld.get(object);
                println(fld.getName() + " '" + limitLength(Objects.toString(value)) + "'");
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void println(Object o) {
        s.println(format(Objects.toString(o)));
    }

    private String format(String str) {
        return str.lines().collect(Collectors.joining());
    }

    private String limitLength(String str) {
        var limit = 40;
        return (str.length() < limit)
                ? str
                : (str.substring(0, limit) + "...");
    }

    @Override
    public void visitTopLevel(JCTree.JCCompilationUnit that) {
        log(that);
        super.visitTopLevel(that);
    }

    @Override
    public void visitPackageDef(JCTree.JCPackageDecl that) {
        log(that);
        super.visitPackageDef(that);
    }

    @Override
    public void visitImport(JCTree.JCImport that) {
        log(that);
        super.visitImport(that);
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl that) {
        log(that);
        super.visitClassDef(that);
    }

    @Override
    public void visitMethodDef(JCTree.JCMethodDecl that) {
        log(that);
        super.visitMethodDef(that);
    }

    @Override
    public void visitVarDef(JCTree.JCVariableDecl that) {
        log(that);
        super.visitVarDef(that);
    }

    @Override
    public void visitSkip(JCTree.JCSkip that) {
        log(that);
        super.visitSkip(that);
    }

    @Override
    public void visitBlock(JCTree.JCBlock that) {
        log(that);
        super.visitBlock(that);
    }

    @Override
    public void visitDoLoop(JCTree.JCDoWhileLoop that) {
        log(that);
        super.visitDoLoop(that);
    }

    @Override
    public void visitWhileLoop(JCTree.JCWhileLoop that) {
        log(that);
        super.visitWhileLoop(that);
    }

    @Override
    public void visitForLoop(JCTree.JCForLoop that) {
        log(that);
        super.visitForLoop(that);
    }

    @Override
    public void visitForeachLoop(JCTree.JCEnhancedForLoop that) {
        log(that);
        super.visitForeachLoop(that);
    }

    @Override
    public void visitLabelled(JCTree.JCLabeledStatement that) {
        log(that);
        super.visitLabelled(that);
    }

    @Override
    public void visitSwitch(JCTree.JCSwitch that) {
        log(that);
        super.visitSwitch(that);
    }

    @Override
    public void visitCase(JCTree.JCCase that) {
        log(that);
        super.visitCase(that);
    }

    @Override
    public void visitSwitchExpression(JCTree.JCSwitchExpression that) {
        log(that);
        super.visitSwitchExpression(that);
    }

    @Override
    public void visitSynchronized(JCTree.JCSynchronized that) {
        log(that);
        super.visitSynchronized(that);
    }

    @Override
    public void visitTry(JCTree.JCTry that) {
        log(that);
        super.visitTry(that);
    }

    @Override
    public void visitCatch(JCTree.JCCatch that) {
        log(that);
        super.visitCatch(that);
    }

    @Override
    public void visitConditional(JCTree.JCConditional that) {
        log(that);
        super.visitConditional(that);
    }

    @Override
    public void visitIf(JCTree.JCIf that) {
        log(that);
        super.visitIf(that);
    }

    @Override
    public void visitExec(JCTree.JCExpressionStatement that) {
        log(that);
        super.visitExec(that);
    }

    @Override
    public void visitBreak(JCTree.JCBreak that) {
        log(that);
        super.visitBreak(that);
    }

    @Override
    public void visitYield(JCTree.JCYield that) {
        log(that);
        super.visitYield(that);
    }

    @Override
    public void visitContinue(JCTree.JCContinue that) {
        log(that);
        super.visitContinue(that);
    }

    @Override
    public void visitReturn(JCTree.JCReturn that) {
        log(that);
        super.visitReturn(that);
    }

    @Override
    public void visitThrow(JCTree.JCThrow that) {
        log(that);
        super.visitThrow(that);
    }

    @Override
    public void visitAssert(JCTree.JCAssert that) {
        log(that);
        super.visitAssert(that);
    }

    @Override
    public void visitApply(JCTree.JCMethodInvocation that) {
        log(that);
        super.visitApply(that);
    }

    @Override
    public void visitNewClass(JCTree.JCNewClass that) {
        log(that);
        super.visitNewClass(that);
    }

    @Override
    public void visitNewArray(JCTree.JCNewArray that) {
        log(that);
        super.visitNewArray(that);
    }

    @Override
    public void visitLambda(JCTree.JCLambda that) {
        log(that);
        super.visitLambda(that);
    }

    @Override
    public void visitParens(JCTree.JCParens that) {
        log(that);
        super.visitParens(that);
    }

    @Override
    public void visitAssign(JCTree.JCAssign that) {
        log(that);
        super.visitAssign(that);
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp that) {
        log(that);
        super.visitAssignop(that);
    }

    @Override
    public void visitUnary(JCTree.JCUnary that) {
        log(that);
        super.visitUnary(that);
    }

    @Override
    public void visitBinary(JCTree.JCBinary that) {
        log(that);
        super.visitBinary(that);
    }

    @Override
    public void visitTypeCast(JCTree.JCTypeCast that) {
        log(that);
        super.visitTypeCast(that);
    }

    @Override
    public void visitTypeTest(JCTree.JCInstanceOf that) {
        log(that);
        super.visitTypeTest(that);
    }

    @Override
    public void visitBindingPattern(JCTree.JCBindingPattern that) {
        log(that);
        super.visitBindingPattern(that);
    }

    @Override
    public void visitDefaultCaseLabel(JCTree.JCDefaultCaseLabel that) {
        log(that);
        super.visitDefaultCaseLabel(that);
    }

    @Override
    public void visitParenthesizedPattern(JCTree.JCParenthesizedPattern that) {
        log(that);
        super.visitParenthesizedPattern(that);
    }

    @Override
    public void visitGuardPattern(JCTree.JCGuardPattern that) {
        log(that);
        super.visitGuardPattern(that);
    }

    @Override
    public void visitIndexed(JCTree.JCArrayAccess that) {
        log(that);
        super.visitIndexed(that);
    }

    @Override
    public void visitSelect(JCTree.JCFieldAccess that) {
        log(that);
        super.visitSelect(that);
    }

    @Override
    public void visitReference(JCTree.JCMemberReference that) {
        log(that);
        super.visitReference(that);
    }

    @Override
    public void visitIdent(JCTree.JCIdent that) {
        log(that);
        super.visitIdent(that);
    }

    @Override
    public void visitLiteral(JCTree.JCLiteral that) {
        log(that);
        super.visitLiteral(that);
    }

    @Override
    public void visitTypeIdent(JCTree.JCPrimitiveTypeTree that) {
        log(that);
        super.visitTypeIdent(that);
    }

    @Override
    public void visitTypeArray(JCTree.JCArrayTypeTree that) {
        log(that);
        super.visitTypeArray(that);
    }

    @Override
    public void visitTypeApply(JCTree.JCTypeApply that) {
        log(that);
        super.visitTypeApply(that);
    }

    @Override
    public void visitTypeUnion(JCTree.JCTypeUnion that) {
        log(that);
        super.visitTypeUnion(that);
    }

    @Override
    public void visitTypeIntersection(JCTree.JCTypeIntersection that) {
        log(that);
        super.visitTypeIntersection(that);
    }

    @Override
    public void visitTypeParameter(JCTree.JCTypeParameter that) {
        log(that);
        super.visitTypeParameter(that);
    }

    @Override
    public void visitWildcard(JCTree.JCWildcard that) {
        log(that);
        super.visitWildcard(that);
    }

    @Override
    public void visitTypeBoundKind(JCTree.TypeBoundKind that) {
        log(that);
        super.visitTypeBoundKind(that);
    }

    @Override
    public void visitAnnotation(JCTree.JCAnnotation that) {
        log(that);
        super.visitAnnotation(that);
    }

    @Override
    public void visitModifiers(JCTree.JCModifiers that) {
        log(that);
        super.visitModifiers(that);
    }

    @Override
    public void visitAnnotatedType(JCTree.JCAnnotatedType that) {
        log(that);
        super.visitAnnotatedType(that);
    }

    @Override
    public void visitErroneous(JCTree.JCErroneous that) {
        log(that);
        super.visitErroneous(that);
    }

    @Override
    public void visitModuleDef(JCTree.JCModuleDecl that) {
        log(that);
        super.visitModuleDef(that);
    }

    @Override
    public void visitExports(JCTree.JCExports that) {
        log(that);
        super.visitExports(that);
    }

    @Override
    public void visitOpens(JCTree.JCOpens that) {
        log(that);
        super.visitOpens(that);
    }

    @Override
    public void visitProvides(JCTree.JCProvides that) {
        log(that);
        super.visitProvides(that);
    }

    @Override
    public void visitRequires(JCTree.JCRequires that) {
        log(that);
        super.visitRequires(that);
    }

    @Override
    public void visitUses(JCTree.JCUses that) {
        log(that);
        super.visitUses(that);
    }

    @Override
    public void visitLetExpr(JCTree.LetExpr that) {
        log(that);
        super.visitLetExpr(that);
    }
}
