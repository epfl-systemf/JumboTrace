package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

import java.util.Objects;

public final class TransformationListener implements TaskListener {
    private final Transformer transformer;

    public TransformationListener(Transformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public void started(TaskEvent e) {
        System.out.println("Started " + e.getKind().name() + " of " + (Objects.isNull(e.getSourceFile()) ? "" : e.getSourceFile().getName()));
        if (e.getKind() == TaskEvent.Kind.GENERATE) {
            var cu = e.getCompilationUnit();
            System.out.println(cu);
        }
    }

    @Override
    public void finished(TaskEvent e) {
        System.out.println("Finished " + e.getKind().name() + " of " + (Objects.isNull(e.getSourceFile()) ? "" : e.getSourceFile().getName()));
        if (e.getKind() == TaskEvent.Kind.ANALYZE) {
            var cu = (JCTree.JCCompilationUnit) e.getCompilationUnit();
            transformer.translate(cu);
        }
    }

    //    private static class PrintVisitor extends TreeTranslator {
//        @Override
//        public void visitTopLevel(JCTree.JCCompilationUnit tree) {
//            super.visitTopLevel(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitPackageDef(JCTree.JCPackageDecl tree) {
//            super.visitPackageDef(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitImport(JCTree.JCImport tree) {
//            super.visitImport(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitClassDef(JCTree.JCClassDecl tree) {
//            super.visitClassDef(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitMethodDef(JCTree.JCMethodDecl tree) {
//            super.visitMethodDef(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitVarDef(JCTree.JCVariableDecl tree) {
//            super.visitVarDef(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitSkip(JCTree.JCSkip tree) {
//            super.visitSkip(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitBlock(JCTree.JCBlock tree) {
//            super.visitBlock(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitDoLoop(JCTree.JCDoWhileLoop tree) {
//            super.visitDoLoop(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitWhileLoop(JCTree.JCWhileLoop tree) {
//            super.visitWhileLoop(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitForLoop(JCTree.JCForLoop tree) {
//            super.visitForLoop(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitForeachLoop(JCTree.JCEnhancedForLoop tree) {
//            super.visitForeachLoop(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitLabelled(JCTree.JCLabeledStatement tree) {
//            super.visitLabelled(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitSwitch(JCTree.JCSwitch tree) {
//            super.visitSwitch(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitCase(JCTree.JCCase tree) {
//            super.visitCase(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitSwitchExpression(JCTree.JCSwitchExpression tree) {
//            super.visitSwitchExpression(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitSynchronized(JCTree.JCSynchronized tree) {
//            super.visitSynchronized(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTry(JCTree.JCTry tree) {
//            super.visitTry(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitCatch(JCTree.JCCatch tree) {
//            super.visitCatch(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitConditional(JCTree.JCConditional tree) {
//            super.visitConditional(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitIf(JCTree.JCIf tree) {
//            super.visitIf(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitExec(JCTree.JCExpressionStatement tree) {
//            super.visitExec(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitBreak(JCTree.JCBreak tree) {
//            super.visitBreak(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitYield(JCTree.JCYield tree) {
//            super.visitYield(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitContinue(JCTree.JCContinue tree) {
//            super.visitContinue(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitReturn(JCTree.JCReturn tree) {
//            super.visitReturn(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitThrow(JCTree.JCThrow tree) {
//            super.visitThrow(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitAssert(JCTree.JCAssert tree) {
//            super.visitAssert(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitApply(JCTree.JCMethodInvocation tree) {
//            super.visitApply(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitNewClass(JCTree.JCNewClass tree) {
//            super.visitNewClass(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitLambda(JCTree.JCLambda tree) {
//            super.visitLambda(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitNewArray(JCTree.JCNewArray tree) {
//            super.visitNewArray(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitParens(JCTree.JCParens tree) {
//            super.visitParens(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitAssign(JCTree.JCAssign tree) {
//            super.visitAssign(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitAssignop(JCTree.JCAssignOp tree) {
//            super.visitAssignop(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitUnary(JCTree.JCUnary tree) {
//            super.visitUnary(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitBinary(JCTree.JCBinary tree) {
//            super.visitBinary(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeCast(JCTree.JCTypeCast tree) {
//            super.visitTypeCast(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeTest(JCTree.JCInstanceOf tree) {
//            super.visitTypeTest(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitBindingPattern(JCTree.JCBindingPattern tree) {
//            super.visitBindingPattern(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitDefaultCaseLabel(JCTree.JCDefaultCaseLabel tree) {
//            super.visitDefaultCaseLabel(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitParenthesizedPattern(JCTree.JCParenthesizedPattern tree) {
//            super.visitParenthesizedPattern(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitGuardPattern(JCTree.JCGuardPattern tree) {
//            super.visitGuardPattern(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitIndexed(JCTree.JCArrayAccess tree) {
//            super.visitIndexed(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitSelect(JCTree.JCFieldAccess tree) {
//            super.visitSelect(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitReference(JCTree.JCMemberReference tree) {
//            super.visitReference(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitIdent(JCTree.JCIdent tree) {
//            super.visitIdent(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitLiteral(JCTree.JCLiteral tree) {
//            super.visitLiteral(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeIdent(JCTree.JCPrimitiveTypeTree tree) {
//            super.visitTypeIdent(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeArray(JCTree.JCArrayTypeTree tree) {
//            super.visitTypeArray(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeApply(JCTree.JCTypeApply tree) {
//            super.visitTypeApply(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeUnion(JCTree.JCTypeUnion tree) {
//            super.visitTypeUnion(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeIntersection(JCTree.JCTypeIntersection tree) {
//            super.visitTypeIntersection(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeParameter(JCTree.JCTypeParameter tree) {
//            super.visitTypeParameter(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitWildcard(JCTree.JCWildcard tree) {
//            super.visitWildcard(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTypeBoundKind(JCTree.TypeBoundKind tree) {
//            super.visitTypeBoundKind(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitErroneous(JCTree.JCErroneous tree) {
//            super.visitErroneous(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitLetExpr(JCTree.LetExpr tree) {
//            super.visitLetExpr(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitModifiers(JCTree.JCModifiers tree) {
//            super.visitModifiers(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitAnnotation(JCTree.JCAnnotation tree) {
//            super.visitAnnotation(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitAnnotatedType(JCTree.JCAnnotatedType tree) {
//            super.visitAnnotatedType(tree);System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//
//        @Override
//        public void visitTree(JCTree tree) {
//            super.visitTree(tree);
//            System.out.println(tree.getClass().getSimpleName() + ": " + tree + "\n\n");
//        }
//    }

}
