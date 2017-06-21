/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.CustomListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static joanakeyrefactoring.CustomListener.ExtractJavaProjModelListener.createMethodFromDeclCtx;
import joanakeyrefactoring.antlr.java8.Java8BaseListener;
import joanakeyrefactoring.antlr.java8.Java8Lexer;
import joanakeyrefactoring.antlr.java8.Java8Parser;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaClass;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaMethod;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaScopeHandler;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 *
 * @author holger
 */
public class CreateSimpleCallgraphListener extends Java8BaseListener {

    private Set<JavaClass> classes;
    private Set<JavaMethod> methods;
    private Map<String, String> idNamesToTypes;
    private String currentPackage;
    private JavaClass currentClass;
    private JavaMethod currentMethod;
    private JavaScopeHandler javaScopeHandler = new JavaScopeHandler();

    public void createCallGraph(Set<JavaClass> classes, Set<JavaMethod> methods, String allClassesInOneString) {
        this.classes = classes;
        this.methods = methods;
        idNamesToTypes = new HashMap<>();
        Java8Lexer lexer = new Java8Lexer(new ANTLRInputStream(allClassesInOneString));
        Java8Parser parser = new Java8Parser(new CommonTokenStream(lexer));
        Java8Parser.CompilationUnitContext parseTree = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, parseTree);
    }

    @Override
    public void enterPackageDeclaration(Java8Parser.PackageDeclarationContext ctx) {
        currentPackage = "";
        for (TerminalNode id : ctx.Identifier()) {
            currentPackage += "." + id.getText();
        }
        currentPackage = currentPackage.substring(1); //remove the beginning "."
    }

    @Override
    public void enterClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {
        javaScopeHandler.enterNewScope();
        String identifier = ctx.normalClassDeclaration().Identifier().getText();
        currentClass = new JavaClass(identifier, currentPackage);
    }

    @Override
    public void exitClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {
        javaScopeHandler.exitScope();
    }

    @Override
    public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        javaScopeHandler.enterNewScope();
        currentMethod = createMethodFromDeclCtx(currentClass, ctx);
    }

    @Override
    public void exitMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        currentMethod = null;
        javaScopeHandler.exitScope();
    }

    @Override
    public void enterForStatement(Java8Parser.ForStatementContext ctx) {
        javaScopeHandler.enterNewScope();
    }

    @Override
    public void exitForStatement(Java8Parser.ForStatementContext ctx) {
        javaScopeHandler.exitScope();
    }

    @Override
    public void enterWhileStatement(Java8Parser.WhileStatementContext ctx) {
        javaScopeHandler.enterNewScope();
    }

    @Override
    public void exitWhileStatement(Java8Parser.WhileStatementContext ctx) {
        javaScopeHandler.exitScope();
    }

    @Override
    public void enterFieldDeclaration(Java8Parser.FieldDeclarationContext ctx) {
        String type = ctx.unannType().getText();
        Java8Parser.VariableDeclaratorListContext variableDeclaratorList = ctx.variableDeclaratorList();
        List<Java8Parser.VariableDeclaratorContext> variableDeclarator = variableDeclaratorList.variableDeclarator();
        for (Java8Parser.VariableDeclaratorContext varDeclCtx : variableDeclarator) {
            String id = varDeclCtx.variableDeclaratorId().getText();
            javaScopeHandler.addVar(id, type);
        }
    }

    @Override
    public void enterLocalVariableDeclaration(Java8Parser.LocalVariableDeclarationContext ctx) {
        String type = ctx.unannType().getText();
        List<Java8Parser.VariableDeclaratorContext> variableDeclarator = ctx.variableDeclaratorList().variableDeclarator();
        for (Java8Parser.VariableDeclaratorContext varDeclCtx : variableDeclarator) {
            String id = varDeclCtx.variableDeclaratorId().getText();
            javaScopeHandler.addVar(id, type);
        }
    }

    @Override
    public void enterMethodInvocation(Java8Parser.MethodInvocationContext ctx) {
        String methodName = ctx.Identifier().getText();

    }

}
