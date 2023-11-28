package com.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.MissingInfoHandler;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.jvm.Target;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class HighLevelTreeMaker {

    private static final String JUMBOTRACE_CLASS_NAME = "___JumboTrace___";

    private final TreeMaker u;

    private final Names n;
    private final MissingInfoHandler missingInfoHandler;
    private final Target target;
    private final Symtab symtab;
    private final AtomicLong idGenerator;   // TODO check that we don't redefine existing identifiers


    public HighLevelTreeMaker(TreeMaker underlying, Names names, MissingInfoHandler missingInfoHandler, Target target, Symtab symtab) {
        this.u = underlying;
        this.n = names;
        this.missingInfoHandler = missingInfoHandler;
        this.target = target;
        this.symtab = symtab;
        idGenerator = new AtomicLong(0);
    }

    public Symbol.ClassSymbol jumbotraceClassSymbol() {
        // TODO modify this to account for the package containing ___JumboTrace___
        return classSymbol(n.fromString(JUMBOTRACE_CLASS_NAME), symtab.rootPackage);
    }

    public Name nextId(String debugHint) {
        return n.fromString("$" + idGenerator.incrementAndGet() + "_" + debugHint);
    }

    public Symbol.VarSymbol varSymbol(Name name, Type tpe, Symbol owner) {
        return new Symbol.VarSymbol(0, name, tpe, owner);
    }

    public Symbol.MethodSymbol methodSymbol(Name name, Type tpe, Symbol owner) {
        return new Symbol.MethodSymbol(0, name, tpe, owner);
    }

    public Symbol.ClassSymbol classSymbol(Name name, Symbol packageSymbol) {
        return new Symbol.ClassSymbol(0, name, packageSymbol);
    }

    public JCTree.JCExpression methodCallLogFunction() {
        // FIXME why not compiled to invokestatic?
        var jumbotraceClassSymbol = jumbotraceClassSymbol();
        var logFunType = new Type.MethodType(List.nil(), symtab.voidType, List.nil(), jumbotraceClassSymbol);
        return select(
                ident(jumbotraceClassSymbol, nonEnclosedClassType(jumbotraceClassSymbol)),
                methodSymbol(n.fromString("methodcall"), logFunType, jumbotraceClassSymbol),
                logFunType
        );
    }

    public Type.ClassType nonEnclosedClassType(Symbol.ClassSymbol classSymbol){
        return new Type.ClassType(Type.noType, List.nil(), classSymbol);
    }

    public JCTree.JCIdent ident(Symbol symbol, Type tpe) {
        var ident = u.Ident(symbol);
        ident.sym = symbol;
        ident.setType(tpe);
        return ident;
    }

    public JCTree.JCExpressionStatement exprStat(JCTree.JCExpression expr) {
        var exec = u.Exec(expr);
        exec.setType(Objects.requireNonNull(expr.type));
        return exec;
    }

    public JCTree.JCVariableDecl varDecl(Symbol.VarSymbol symbol, JCTree.JCExpression init) {
        var varDef = u.VarDef(u.Modifiers(0), symbol.name, u.Type(Objects.requireNonNull(init.type)), init);
        varDef.sym = symbol;
        varDef.setType(new Type.JCVoidType());
        return varDef;
    }

    public JCTree.JCMethodInvocation methodInvocation(JCTree.JCExpression callee, List<JCTree.JCExpression> args) {
        return u.App(callee, args);
    }

    public JCTree.LetExpr letExpr(List<JCTree.JCStatement> stats, JCTree.JCExpression expr) {
        var letExpr = u.LetExpr(stats, expr);
        letExpr.setType(Objects.requireNonNull(expr.type));
        return letExpr;
    }

    public JCTree.JCExpression select(JCTree.JCExpression selected, Symbol selector, Type tpe) {
        var select = u.Select(selected, selector);
        select.setType(tpe);
        return select;
    }

    private void requireArgNonNull(Object t) {
        if (Objects.isNull(t)) {
            throw new IllegalArgumentException("argument is null");
        }
    }

}
