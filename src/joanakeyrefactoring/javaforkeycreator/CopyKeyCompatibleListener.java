/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaforkeycreator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import joanakeyrefactoring.antlr.java8.Java8BaseListener;
import joanakeyrefactoring.antlr.java8.Java8Lexer;
import joanakeyrefactoring.antlr.java8.Java8Parser;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 *
 * @author holger
 */
public class CopyKeyCompatibleListener extends Java8BaseListener {

    private StringBuilder currentlyGenerated;
    private List<String> keyCompatibleJavaFeature = new ArrayList<>();
    List<String> classCodeAsLines = new ArrayList<>();

    public CopyKeyCompatibleListener() throws FileNotFoundException, IOException {
        InputStream is = new FileInputStream("dep/JAVALANG.txt");
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));
        String line = buf.readLine();
        while (line != null) {
            keyCompatibleJavaFeature.add(line);
            line = buf.readLine();
        }
        buf.close();
    }

    private boolean isKeyCompatible(String type) {
        for (String s : keyCompatibleJavaFeature) {
            if (s.endsWith(type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void enterClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {
        int line = ctx.getStart().getLine();
    }

    private String extractStringInBetween(ParserRuleContext ctx) {
        int startLine = ctx.getStart().getLine();
        int startCharPositionInLine = ctx.getStart().getCharPositionInLine();
        int stopLine = ctx.getStop().getLine();
        int stopCharPositionInLine = ctx.getStop().getCharPositionInLine();

        String s = "";
        if (startLine == stopLine) {
            return classCodeAsLines.get(startLine - 1).substring(startCharPositionInLine, stopCharPositionInLine);
        }
        for (int i = startLine - 1; i < stopLine; ++i) {
            if (i == startLine) {
                s += classCodeAsLines.get(i).substring(startCharPositionInLine - 1, classCodeAsLines.get(i).length()) + '\n';
            } else if (i == stopLine) {
                s += classCodeAsLines.get(i).substring(0, stopCharPositionInLine - 1);
            } else {
                s += classCodeAsLines.get(i)+ '\n';
            }
        }

        return s;
    }
    

    @Override
    public void enterPackageDeclaration(Java8Parser.PackageDeclarationContext ctx) {
        currentlyGenerated.append(extractStringInBetween(ctx)).append(";\n");
    }

    @Override
    public void enterImportDeclaration(Java8Parser.ImportDeclarationContext ctx) {
        String text = ctx.getText().substring("import".length(), ctx.getText().length() - 1);
        if (isKeyCompatible(text)) {
            currentlyGenerated.append(extractStringInBetween(ctx)).append(";\n");
        }
    }

    @Override
    public void enterFieldDeclaration(Java8Parser.FieldDeclarationContext ctx) {
        String type = ctx.unannType().getText();
        int lastIndexOfLessThan = type.lastIndexOf("<");
        if (lastIndexOfLessThan != -1) {
            type = type.substring(0, lastIndexOfLessThan);
        }
        if (isKeyCompatible(type)) {
            currentlyGenerated.append(extractStringInBetween(ctx)).append(";\n");
        }
    }

    @Override
    public void enterMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
        currentlyGenerated.append(extractStringInBetween(ctx)).append('\n');
    }

    String generateKeyCompatible(String classCode) {
        currentlyGenerated = new StringBuilder();
        classCodeAsLines = new ArrayList<>();

        String[] split = classCode.split("\n");
        for (int i = 0; i < split.length; i++) {
            String string = split[i];
            classCodeAsLines.add(string);
        }

        Java8Lexer java8Lexer = new Java8Lexer(new ANTLRInputStream(classCode));
        Java8Parser java8Parser = new Java8Parser(new CommonTokenStream(java8Lexer));
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, java8Parser.compilationUnit());

        return currentlyGenerated.toString();
    }
}
