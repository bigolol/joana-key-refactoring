/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.CustomListener;

import java.util.List;
import java.util.Set;
import joanakeyrefactoring.antlr.java8.Java8BaseListener;
import joanakeyrefactoring.antlr.java8.Java8Lexer;
import joanakeyrefactoring.antlr.java8.Java8Parser;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaClass;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaMethod;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaMethodArgument;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.OrderedHashSet;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 *
 * @author holger
 */
public class ExtractJavaProjModelListener extends Java8BaseListener {

    private Set<JavaClass> classes = new OrderedHashSet<>();
    private Set<JavaMethod> methods = new OrderedHashSet<>();
    private JavaClass currentClass;
    private String currentPackage;

    public void extractDataFromProject(String allClassesInOneString) {
        classes = new OrderedHashSet<>();
        methods = new OrderedHashSet<>();
        Java8Lexer lexer = new Java8Lexer(new ANTLRInputStream(allClassesInOneString));
        Java8Parser parser = new Java8Parser(new CommonTokenStream(lexer));
        Java8Parser.CompilationUnitContext parseTree = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, parseTree);
    }

    public Set<JavaClass> getClasses() {
        return classes;
    }

    public Set<JavaMethod> getMethods() {
        return methods;
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
        String identifier = ctx.normalClassDeclaration().Identifier().getText();
        currentClass = new JavaClass(identifier, currentPackage);
    }

    @Override
    public void exitClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {
        classes.add(currentClass);
    }
       

    @Override
    public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        JavaMethod method = createMethodFromDeclCtx(currentClass, ctx);
        currentClass.addMethod(method);
        methods.add(method);
    }
    
    public static JavaMethod createMethodFromDeclCtx(JavaClass c, Java8Parser.MethodDeclarationContext ctx) {
        boolean isStatic = false;
        String methodName = ctx.methodHeader().methodDeclarator().Identifier().getText();
        List<Java8Parser.MethodModifierContext> methodModifier = ctx.methodModifier();
        for (Java8Parser.MethodModifierContext currentmodifier : methodModifier) {
            if (currentmodifier.getText().equals("static")) {
                isStatic = true;
            }
        }
        JavaMethod method = new JavaMethod(methodName, isStatic, c);
        Java8Parser.FormalParameterListContext formalParameterList = ctx.methodHeader().methodDeclarator().formalParameterList();
        if (formalParameterList != null) {
            Java8Parser.FormalParametersContext formalParameters = formalParameterList.formalParameters();
            if (formalParameters != null) {
                List<Java8Parser.FormalParameterContext> formalParams = formalParameters.formalParameter();
                for (Java8Parser.FormalParameterContext currentFormalParamCtx : formalParams) {
                    String id = currentFormalParamCtx.variableDeclaratorId().getText();
                    String type = currentFormalParamCtx.unannType().getText();
                    method.addArgument(new JavaMethodArgument(type, id));
                }
            } else {
                final Java8Parser.LastFormalParameterContext lastFormalParameter = ctx.methodHeader().methodDeclarator().formalParameterList().lastFormalParameter();
                if (lastFormalParameter != null) {
                    String id = lastFormalParameter.formalParameter().variableDeclaratorId().getText();
                    String type = lastFormalParameter.formalParameter().unannType().getText();
                    method.addArgument(new JavaMethodArgument(type, id));
                }
            }
        }
        return method;
    }

}
