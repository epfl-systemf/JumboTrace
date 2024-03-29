package ch.epfl.systemf.jumbotrace.javacplugin;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class bundles together several symbols generators and nodes generators that are needed during the transformation of the AST
 */
public final class TreeMakingContainer {

    private final TreeMaker treeMaker;

    private final Names names;
    private final Symtab symbolTable;
    private final AtomicLong idGenerator;


    public TreeMakingContainer(TreeMaker treeMaker, Names names, Symtab symbolTable) {
        this.treeMaker = treeMaker;
        this.names = names;
        this.symbolTable = symbolTable;
        idGenerator = new AtomicLong(0);
    }

    TreeMaker mk(){
        return treeMaker;
    }

    Names n(){
        return names;
    }

    Symtab st(){
        return symbolTable;
    }

    public Name nextId(String debugHint) {
        return names.fromString("$" + idGenerator.incrementAndGet() + "_" + debugHint);
    }

    public Symbol.PackageSymbol makeCompositePackageSymbol(Symbol.PackageSymbol rootPackage, String... parts) {
        var curr = rootPackage;
        for (var part : parts) {
            curr = new Symbol.PackageSymbol(n().fromString(part), curr);
        }
        return curr;
    }

}
