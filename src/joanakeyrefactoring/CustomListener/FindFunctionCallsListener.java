/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.CustomListener;

import java.util.HashSet;
import java.util.Set;
import joanakeyrefactoring.antlr.java8.Java8BaseListener;
import joanakeyrefactoring.antlr.java8.Java8Lexer;
import joanakeyrefactoring.antlr.java8.Java8Parser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 *
 * @author holger
 */
public class FindFunctionCallsListener extends Java8BaseListener {

    private Set<String> currentCalledFunctions;

    public Set<String> getCalledFunctions(String function) {
        currentCalledFunctions = new HashSet<>();

        Java8Lexer java8Lexer = new Java8Lexer(new ANTLRInputStream(function));
        Java8Parser java8Parser = new Java8Parser(new CommonTokenStream(java8Lexer));
        Java8Parser.MethodBodyContext compilationUnit = java8Parser.methodBody();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, compilationUnit);
        return currentCalledFunctions;
    }

    @Override
    public void enterMethodInvocation(Java8Parser.MethodInvocationContext ctx) {
        Java8Parser.MethodNameContext methodName = ctx.methodName();
        currentCalledFunctions.add(methodName.getText());
    }
    
    
    
}
