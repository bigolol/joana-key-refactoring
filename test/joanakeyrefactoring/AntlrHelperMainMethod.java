/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import javax.swing.JFrame;
import javax.swing.JPanel;
import joanakeyrefactoring.antlr.java8.Java8Lexer;
import joanakeyrefactoring.antlr.java8.Java8Parser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 *
 * @author holger
 */
public class AntlrHelperMainMethod {

    public static void main(String[] args) {
        String java = "package p;"
                + "class ClassA {"
                + "private int x;"   
                + "}";
        Java8Lexer java8Lexer = new Java8Lexer(new ANTLRInputStream(java));
        Java8Parser java8Parser = new Java8Parser(new CommonTokenStream(java8Lexer));
        ParseTree tree = java8Parser.compilationUnit();
        System.out.println(tree.toStringTree(java8Parser));
    }
}
