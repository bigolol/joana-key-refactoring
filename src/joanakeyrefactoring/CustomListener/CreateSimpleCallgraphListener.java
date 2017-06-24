/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.CustomListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static joanakeyrefactoring.CustomListener.ExtractJavaProjModelListener.createMethodFromDeclCtx;
import joanakeyrefactoring.antlr.java8.Java8BaseListener;
import joanakeyrefactoring.antlr.java8.Java8Lexer;
import joanakeyrefactoring.antlr.java8.Java8Parser;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaClass;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaMethod;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaMethodArgument;
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

    private List<JavaClass> classes;
    private List<JavaMethod> methods;
    private Map<String, String> idNamesToTypes;
    private String currentPackage;
    private JavaClass currentClass;
    private JavaMethod currentMethod;
    private JavaScopeHandler javaScopeHandler = new JavaScopeHandler();

    public void createCallGraph(List<JavaClass> classes, List<JavaMethod> methods, String allClassesInOneString) {
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
        for(JavaMethodArgument arg : currentMethod.getArgs()) {
            javaScopeHandler.addVar(arg.getName(), arg.getType());
        }
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
        String text = ctx.getText();
        Java8Parser.MethodNameContext methodName = ctx.methodName();
        TerminalNode identifier = ctx.Identifier();

        if (methodName != null) { //it is a pure function
            //do nothing since the class gets added anyways

        } else if (identifier != null) { //it is a function called on an object/class
            TerminalNode Identifier = ctx.Identifier();
            String typenameText = ctx.typeName().getText();
            String varType = javaScopeHandler.getTypeForVar(typenameText);
            Java8Parser.ArgumentListContext argumentList = ctx.argumentList();
            List<JavaMethodArgument> args = new ArrayList<>();
            if(argumentList == null) {
                
            } else {
                List<Java8Parser.ExpressionContext> expressions = argumentList.expression();
                for(Java8Parser.ExpressionContext expCtx : expressions) {
                    String passedVarId = expCtx.getText();
                    String type = javaScopeHandler.getTypeForVar(passedVarId);
                    args.add(new JavaMethodArgument(type, passedVarId));
                }
            }
            if (varType != null) {
                for (JavaClass jc : classes) {
                    if (jc.getName().equals(varType)) {
                        jc.addDependentMethod(currentMethod);
                        JavaMethod calledMethod = new JavaMethod(Identifier.getText(), false, currentClass);
                        calledMethod.getArgs().addAll(args);
                        final int indexOfCalling = methods.indexOf(currentMethod);
                        final int indexOfCalled = methods.indexOf(calledMethod);
                        methods.get(indexOfCalling).getCalledMethods().add(methods.get(indexOfCalled));
                        break;
                    }
                }
            } else { //it is either static or called by a library function 

            }
            int i = 0;
        }
    }

}
