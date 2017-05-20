package joanakeyrefactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import joanakeyrefactoring.antlr.JavaBaseListener;
import joanakeyrefactoring.antlr.JavaLexer;
import joanakeyrefactoring.antlr.JavaParser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.*;

/**
 *
 * @author Marko Kleine B�ning
 *
 *
 */
public class ParseJavaForKeyListener extends JavaBaseListener {

    private static List<String[]> fields = new ArrayList<>();
    private static Map<String, String> methods = new HashMap<>();
    private static Map<String, String> methodsAndClasses = new HashMap<>();
    private static Map<String, String> constructors = new HashMap<>();
    private static Map<String, String[]> params = new HashMap<>();
    private static Map<String, List<String>> creators = new HashMap<>();
    private static List<String> createdName = new ArrayList<>();
    private static String methodName = "";
    private static String holeMethod = "";
    private boolean inConstructor = false;
    private String constructorName;
    private String className = "";
    private String holeMethodMinus1;
    private static Map<String, String> paramsWithNullable = new HashMap<>();
    private static List<String> fieldsCorrect = new ArrayList<>();
    private static List<String> classList = new ArrayList<>();

    public ParseJavaForKeyListener() {

    }

    public ParseJavaForKeyListener(String allClasses) {
        ANTLRInputStream input = new ANTLRInputStream(allClasses);
        saveAllRequirements(input);
    }

    public List<String[]> getFields() {
        return fields;
    }

    public String getFieldsCorrectAsString() {
        StringBuilder sb = new StringBuilder();
        for (String field : fieldsCorrect) {
            sb.append(field);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public Set<String> getMethods() {
        return methods.keySet();
    }

    public String getFieldsAsString() {
        StringBuilder sb = new StringBuilder();
        for (String[] field : fields) {
            sb.append(field[0] + " " + field[1] + ";");
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String getConstructorOfMethod(String methodName) {
        return constructors.get(methodName);
    }

    public String getParamsWithNullable(String methodName) {
        return paramsWithNullable.get(methodName);
    }

    public List<String> getClassList() {
        return classList;
    }

    public String getCompleteMethod(String methodName) {
        return methods.get(methodName);

    }

    public String getClass(String methodName) {
        return methodsAndClasses.get(methodName);

    }

    public String[] getParamsOfMethod(String methodName) {
        return params.get(methodName);
    }

    public List<String> getCreatedNames(String methodName) {
        return creators.get(methodName);
    }

    @Override
    public void enterFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        String[] field = new String[2];
        field[0] = ctx.getChild(0).getText();
        field[1] = ctx.getChild(1).getText();
        fields.add(field);
        int a = ctx.start.getStartIndex();
        int b = ctx.stop.getStopIndex();
        Interval interval = new Interval(a, b);
        CharStream input = ctx.start.getInputStream();
        String nullable = "/*@nullable*/ ";
        String[] fieldArray = input.getText(interval).split(" ");
        StringBuilder sbF = new StringBuilder();
        for (int i = 0; i < fieldArray.length; i++) {
            sbF.append(fieldArray[i] + " ");
            if (i == 0) {
                sbF.append(nullable);
            }
        }
        fieldsCorrect.add(sbF.toString());
    }

    @Override
    public void enterConstructorDeclaration(
            JavaParser.ConstructorDeclarationContext ctx) {
        inConstructor = true;
        constructors.put(ctx.getChild(0).getText(), ctx.getText());
        constructorName = ctx.getChild(0).getText();
    }

    @Override
    public void enterFormalParameters(JavaParser.FormalParametersContext ctx) {
        //TODO: nullable auch bei methodenKopf nullable String
        if (inConstructor) {
            int a = ctx.start.getStartIndex();
            int b = ctx.stop.getStopIndex();
            Interval interval = new Interval(a, b);
            CharStream input = ctx.start.getInputStream();
            String test = input.getText(interval);
            String[] param = test.split(",");
            String[] allParamTypes = new String[param.length];
            for (int i = 0; i < param.length; i++) {
                allParamTypes[i] = param[i].trim().split(" ")[0].trim();
                if (allParamTypes[i].contains("(")) {
                    String tmp = allParamTypes[i].split("\\(")[1].trim();
                    allParamTypes[i] = tmp;
                }
            }
            params.put(constructorName, allParamTypes);
            inConstructor = false;
        } else {
            int a = ctx.start.getStartIndex();
            int b = ctx.stop.getStopIndex();
            Interval interval = new Interval(a, b);
            CharStream input = ctx.start.getInputStream();
            StringBuilder sbF = new StringBuilder();
            if (input.getText(interval).length() > 3) {
                String nullable = "/*@nullable*/ ";
                String[] paramArray = input.getText(interval).split(",");
                for (int j = 0; j < paramArray.length; j++) {
                    String[] fieldArray = paramArray[j].split(" ");
                    for (int i = 0; i < fieldArray.length; i++) {
                        sbF.append(fieldArray[i] + " ");
                        if (i == 0) {
                            sbF.append(nullable);
                        }
                    }
                    if (j < paramArray.length - 1) {
                        sbF.append(", ");
                    }
                }
            } else {
                sbF.append("()");
            }
            paramsWithNullable.put(methodName, sbF.toString());
        }
    }

    @Override
    public void enterMyMethodName(JavaParser.MyMethodNameContext ctx) {
        createdName = new ArrayList<String>();
        methodName = ctx.getText();
        methods.put(methodName, holeMethod);
        methodsAndClasses.put(methodName, className);
        if (holeMethod.contains(".substring")
                || holeMethod.contains(".getByte")) {
            createdName.add("String.methods");
        }
        creators.put(methodName, createdName);
    }

    @Override
    public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        int a = ctx.start.getStartIndex();
        int b = ctx.stop.getStopIndex();
        Interval interval = new Interval(a, b);
        CharStream input = ctx.start.getInputStream();
        holeMethod = input.getText(interval);
        String[] lines = holeMethod.split(System.getProperty("line.separator"));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < lines.length; i++) {
            sb.append(lines[i] + System.lineSeparator());
        }
        holeMethodMinus1 = sb.toString();
    }

    @Override
    public void enterFormalParameterList(
            JavaParser.FormalParameterListContext ctx) {
        int a = ctx.start.getStartIndex();
        int b = ctx.stop.getStopIndex();
        Interval interval = new Interval(a, b);
        CharStream input = ctx.start.getInputStream();
        String test = input.getText(interval);
        String[] param = test.split(",");
        String[] allParamTypes = new String[param.length];
        for (int i = 0; i < param.length; i++) {
            allParamTypes[i] = param[i].trim().split(" ")[0].trim();
            if (allParamTypes[i].contains("byte[]")) {
                allParamTypes[i] = "[B";
            }
        }
        params.put(methodName, allParamTypes);
    }

    @Override
    public void enterCreatedName(JavaParser.CreatedNameContext ctx) {
        createdName.add(ctx.getText());
    }

    @Override
    public void enterTypeType(JavaParser.TypeTypeContext ctx) {
        createdName.add(ctx.getText());
    }

    @Override
    public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        className = ctx.getChild(1).getText();
        classList.add(ctx.getChild(1).getText());
    }

    private static void printCompilationUnit(ANTLRInputStream input) {
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        JavaParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        ParseJavaForKeyListener listener = new ParseJavaForKeyListener();
        walker.walk(listener, compilationUnitContext);
    }

    private void saveAllRequirements(ANTLRInputStream input) {
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        JavaParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        ParseJavaForKeyListener listener = new ParseJavaForKeyListener();
        walker.walk(listener, compilationUnitContext);
    }
}
