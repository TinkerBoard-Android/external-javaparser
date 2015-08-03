package me.tomassetti.symbolsolver.model.javaparser.contexts;

import com.github.javaparser.ast.body.*;
import me.tomassetti.symbolsolver.model.*;
import me.tomassetti.symbolsolver.model.declarations.MethodDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.javaparser.JavaParserFactory;
import me.tomassetti.symbolsolver.model.javaparser.UnsolvedTypeException;
import me.tomassetti.symbolsolver.model.javaparser.declarations.JavaParserClassDeclaration;
import me.tomassetti.symbolsolver.model.usages.TypeUsage;

import java.util.List;

/**
 * Created by federico on 28/07/15.
 */
public class ClassOrInterfaceDeclarationContext extends AbstractJavaParserContext<ClassOrInterfaceDeclaration> {

    public ClassOrInterfaceDeclarationContext(ClassOrInterfaceDeclaration wrappedNode) {
        super(wrappedNode);
    }

    @Override
    public SymbolReference solveSymbol(String name, TypeSolver typeSolver) {
        if (typeSolver == null) throw new IllegalArgumentException();

        // first among declared fields
        for (BodyDeclaration member : wrappedNode.getMembers()){
            if (member instanceof FieldDeclaration) {
                SymbolDeclarator symbolDeclarator = JavaParserFactory.getSymbolDeclarator(member, typeSolver);
                SymbolReference ref = solveWith(symbolDeclarator, name);
                if (ref.isSolved()) {
                    return ref;
                }
            }
        }

        // then among inherited fields
        if (!wrappedNode.isInterface() && wrappedNode.getExtends() != null && wrappedNode.getExtends().size() > 0){
            String superClassName = wrappedNode.getExtends().get(0).getName();
            SymbolReference<TypeDeclaration> superClass = solveType(superClassName, typeSolver);
            if (!superClass.isSolved()) {
                throw new UnsolvedTypeException(this, superClassName);
            }
            SymbolReference ref = superClass.getCorrespondingDeclaration().getContext().solveSymbol(name, typeSolver);
            if (ref.isSolved()) {
                return ref;
            }
        }

        // then to parent
        return getParent().solveSymbol(name, typeSolver);
    }

    @Override
    public SymbolReference<TypeDeclaration> solveType(String name, TypeSolver typeSolver) {
        if (this.wrappedNode.getName().equals(name)){
            return SymbolReference.solved(new JavaParserClassDeclaration(this.wrappedNode));
        }
        // TODO consider also internal classes
        return getParent().solveType(name, typeSolver);
    }

    @Override
    public SymbolReference<MethodDeclaration> solveMethod(String name, List<TypeUsage> parameterTypes, TypeSolver typeSolver) {
        for (BodyDeclaration member : this.wrappedNode.getMembers()) {
            if (member instanceof com.github.javaparser.ast.body.MethodDeclaration) {
                com.github.javaparser.ast.body.MethodDeclaration method = (com.github.javaparser.ast.body.MethodDeclaration)member;
                if (method.getName().equals(name)) {
                    // TODO implement signature comparison
                    throw new UnsupportedOperationException();
                }
            }
        }
        return SymbolReference.unsolved(MethodDeclaration.class);
    }
}
