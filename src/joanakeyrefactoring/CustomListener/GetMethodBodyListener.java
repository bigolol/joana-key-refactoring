/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.CustomListener;

import java.util.ArrayList;
import java.util.List;
import joanakeyrefactoring.antlr.java8.Java8BaseListener;
import joanakeyrefactoring.antlr.java8.Java8Lexer;
import joanakeyrefactoring.antlr.java8.Java8Parser;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;
import org.antlr.runtime.tree.TreeVisitor;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 *
 * @author holger
 */
public class GetMethodBodyListener extends Java8BaseListener {

    private List<String> extractedMethodParamNames;
    private boolean parsedRightMethod;
    private String methodBody;
    private StaticCGJavaMethod method;
    private int methodStartLine;

    public void parseFile(String file, StaticCGJavaMethod method) {
        Java8Lexer java8Lexer = new Java8Lexer(new ANTLRInputStream(file));
        Java8Parser java8Parser = new Java8Parser(new CommonTokenStream(java8Lexer));
        ParseTreeWalker walker = new ParseTreeWalker();
        extractedMethodParamNames = new ArrayList<>();
        this.method = method;
        parsedRightMethod = false;
        walker.walk(this, java8Parser.compilationUnit());
    }

    public List<String> getExtractedMethodParamNames() {
        return extractedMethodParamNames;
    }

    public String getMethodBody() {
        return methodBody;
    }

    public int getMethodStartLine() {
        return methodStartLine;
    }
    
    @Override
    public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        if (parsedRightMethod == true) {
            return;
        }
        parseMethodDeclarator(ctx.methodHeader().methodDeclarator());
        if(parsedRightMethod == true) {
            methodStartLine = ctx.getStart().getLine();
            methodBody = ctx.methodBody().getText();
        }
    }

    public void parseMethodDeclarator(Java8Parser.MethodDeclaratorContext ctx) {
        String text = ctx.getText();
        String methodName = ctx.Identifier().getText();
        if (!methodName.equals(method.getId())) {
            return;
        }
        Java8Parser.FormalParameterListContext formalParameterList = ctx.formalParameterList();
        String argTypeString = getArgTypeString(formalParameterList);

        if (!argTypeString.equals(method.getParameter())) {
            return;
        }
        
        if (formalParameterList != null) {
            Java8Parser.FormalParametersContext formalParameters = formalParameterList.formalParameters();
            if (formalParameters != null) {
                for (Java8Parser.FormalParameterContext currentFormalParam : formalParameters.formalParameter()) {
                    String variableDeclaratorId = currentFormalParam.variableDeclaratorId().getText();
                    extractedMethodParamNames.add(variableDeclaratorId);
                }
            }
            Java8Parser.LastFormalParameterContext lastFormalParameter = formalParameterList.lastFormalParameter();
            String variableDeclaratorId = lastFormalParameter.formalParameter().variableDeclaratorId().getText();
            extractedMethodParamNames.add(variableDeclaratorId);
        }
        parsedRightMethod = true;
    }

    private String getArgTypeString(Java8Parser.FormalParameterListContext ctx) {
        if (ctx == null) {
            return "";
        }
        String created = "";
        Java8Parser.FormalParametersContext formalParameters = ctx.formalParameters();
        if (formalParameters != null) {
            for (Java8Parser.FormalParameterContext currentFormalParam : formalParameters.formalParameter()) {
                Java8Parser.UnannTypeContext unannType = currentFormalParam.unannType();
                String currentTypeString = unannType.getText();
                created += currentTypeString + ",";
            }
        }
        Java8Parser.LastFormalParameterContext lastFormalParameter = ctx.lastFormalParameter();
        created += lastFormalParameter.formalParameter().unannType().getText();
        return created;
    }

}
