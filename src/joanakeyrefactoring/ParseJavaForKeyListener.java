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
 * @author Marko Kleine B�ning
 */
public class ParseJavaForKeyListener extends JavaBaseListener {

    private List<String[]> fields = new ArrayList<>();
    private Map<String, String> methods = new HashMap<>();
    private Map<String, String> methodsAndClasses = new HashMap<>();
    private Map<String, String> constructors = new HashMap<>();
    private Map<String, String[]> params = new HashMap<>();
    private Map<String, List<String>> creators = new HashMap<>();
    private List<String> createdName = new ArrayList<>();
    private String methodName = "";
    private String completeMethod = "";
    private boolean inConstructor = false;
    private String constructorName;
    private String className = "";
    private Map<String, String> paramsWithNullable = new HashMap<>();
    private List<String> fieldsWithNullable = new ArrayList<>();
    private List<String> classList = new ArrayList<>();
    private static final String nullable = "/*@nullable*/ ";

    /**
     * takes as input a string containing every .java file of interest and then
     * extracts all necessary information. This string is created by the
     * Automationhelpers method called
     * readAllSourceFilesIntoOneStringAndFillClassMap.
     *
     * @param allClasses the string file containing the summary of every .java
     * file of interest to us, i e every file which belongs to the checked
     * program
     */
    public ParseJavaForKeyListener(String allClasses) {
        ANTLRInputStream input = new ANTLRInputStream(allClasses);
        saveAllRequirements(input);
    }

    /**
     * creates the syntax tree and visits it
     *
     * @param input the ANTLRInputStream which represents the java file which
     * summarizes all .java files belonging to the project
     */
    private void saveAllRequirements(ANTLRInputStream input) {
        JavaLexer lexer = new JavaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        JavaParser parser = new JavaParser(tokens);
        JavaParser.CompilationUnitContext compilationUnitContext = parser.compilationUnit();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, compilationUnitContext);
    }

    public List<String[]> getFields() {
        return fields;
    }

    public String getFieldsAsString() {
        StringBuilder sb = new StringBuilder();
        for (String[] field : fields) {
            sb.append(field[0] + " " + field[1] + ";");
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public String getFieldsWithNullableAsString() {
        StringBuilder sb = new StringBuilder();
        for (String field : fieldsWithNullable) {
            sb.append(field);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public Set<String> getMethods() {
        return methods.keySet();
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

    /**
     * if the visitor enters a field decl, this function adds the fields type
     * and name as an array of strings to the fields List and adds the
     * declaration with the string @nullable after the type decl to the
     * fieldsCorrect List
     *
     * ex: public static int x -> {"int", "x"} into fields -> "int @nullable x"
     * into fieldsCorrect
     *
     * @param ctx
     */
    @Override
    public void enterFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
        String typeAsString = ctx.getChild(0).getText();
        String idAsString = ctx.getChild(1).getText();
        fields.add(new String[]{typeAsString, idAsString});
        String declWithInsertedNullable = insertNullableAfterTypeIntoFieldDecl(ctx);
        fieldsWithNullable.add(declWithInsertedNullable);
    }

    private String insertNullableAfterTypeIntoFieldDecl(JavaParser.FieldDeclarationContext ctx) {
        int startIndex = ctx.start.getStartIndex();
        int stopIndex = ctx.stop.getStopIndex();
        Interval interval = new Interval(startIndex, stopIndex);
        CharStream input = ctx.start.getInputStream();
        String[] fieldArray = input.getText(interval).split(" ");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < fieldArray.length; i++) {
            stringBuilder.append(fieldArray[i] + " ");
            if (i == 0) {
                stringBuilder.append(nullable);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * sets inConstructor bool to true, and adds the constructor decl to the
     * constructors hashmap under the key of the class name. Also sets var
     * contructorName to the constructors name
     *
     * Example: C(int x, int y){} -> ["C"] => [C(int x, int y)] in constructors
     *
     * @param ctx
     */
    @Override
    public void enterConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
        inConstructor = true;
        String className = ctx.getChild(0).getText();
        String entireCtorDecl = ctx.getText();
        constructors.put(className, entireCtorDecl);
        constructorName = className;
    }

    /**
     * This method is called when the visitor enters a method or ctor
     * declaration and handles its parameters.
     *
     * @param ctx
     */
    @Override
    public void enterFormalParameters(JavaParser.FormalParametersContext ctx) {
        //TODO: nullable auch bei methodenKopf nullable String
        if (inConstructor) {
            enterFormalParamsInCtor(ctx);
        } else {
            enterFormalParamsInMethod(ctx);
        }
    }

    /**
     * takes the input params to the method and either -> inserts nullable
     * between type and id -> creates a string (), if the method takes no params
     * puts whatever it created into the paramsWithNullable map
     *
     * @param ctx
     */
    private void enterFormalParamsInMethod(JavaParser.FormalParametersContext ctx) {
        String stringBetweenStartAndStop = extractTextBetweenStartAndStopIndex(ctx);
        if (stringBetweenStartAndStop.length() <= 3) { //this means the method takes no parameters
            paramsWithNullable.put(methodName, "()");
        } else {
            String methodParamsWithNullable = insertNullableBetweenMethodParameters(stringBetweenStartAndStop);
            paramsWithNullable.put(methodName, methodParamsWithNullable);
        }
    }

    /**
     * inserts the nullable string between the type and id decl of each of the
     * params int the passed string
     *
     * eg: (int x, String s, char c) -> (int nullable x, String nullable s, char
     * nullable c) *
     *
     * @param stringBetweenStartAndStop the String of all the parameters passed
     * to the method, including the brackets
     * @return
     */
    private String insertNullableBetweenMethodParameters(String stringBetweenStartAndStop) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] parameter = stringBetweenStartAndStop.split(",");
        for (String currentParam : parameter) {
            String paramDeclWithNullable = insertNullableIntoParamDecl(currentParam);
            stringBuilder.append(paramDeclWithNullable);
            stringBuilder.append(", ");
        }
        return stringBuilder.toString();
    }

    /**
     * inserts the nullable string inbetween a parameters type and id:
     *
     * eg: int x -> itn nullable x
     *
     * @param currentParam the string decl of the param into which nullable will
     * be inserted
     * @return the decl with nullable inserted
     */
    private String insertNullableIntoParamDecl(String currentParam) {
        String[] paramSplit = currentParam.trim().split(" ");
        String created = paramSplit[0] + " " + nullable;
        for (int i = 1; i < paramSplit.length; ++i) {
            created += " " + paramSplit[i];
        }
        return created;
    }

    /**
     * this method takes in a ParserRuleCtx and returns the string contained
     * between ints start and stopindex
     *
     *
     *
     * @param ctx the context for which the text is to be extracted
     * @return the text as a string between ctx.start.getStartIndex() and
     * ctx.stop.getStopIndex()
     */
    private String extractTextBetweenStartAndStopIndex(ParserRuleContext ctx) {
        int startIndex = ctx.start.getStartIndex();
        int stopIndex = ctx.stop.getStopIndex();
        Interval interval = new Interval(startIndex, stopIndex);
        CharStream input = ctx.start.getInputStream();
        String stringBetweenStartAndStop = input.getText(interval);
        return stringBetweenStartAndStop;
    }

    /**
     * this method extracts all types of the parameters passed to the
     * constructor as Strings and puts it into the params hashmap, using the
     * ctors name as key
     *
     * @param ctx
     */
    private void enterFormalParamsInCtor(JavaParser.FormalParametersContext ctx) {
        String allParamsAsString = getParameterStringWithoutBrackets(ctx);
        String[] parameters = allParamsAsString.split(",");
        String[] allParamTypes = new String[parameters.length];
        int i = 0;
        for (String param : parameters) {
            String parameterType = param.split(" ")[0].trim();
            allParamTypes[i] = parameterType;
            ++i;
        }
        params.put(constructorName, allParamTypes);
        inConstructor = false;
    }

    private String getParameterStringWithoutBrackets(JavaParser.FormalParametersContext ctx) {
        String allParamsAsString = extractTextBetweenStartAndStopIndex(ctx);
        allParamsAsString = allParamsAsString.substring(1, allParamsAsString.length() - 1);
        return allParamsAsString;
    }

    @Override
    public void enterMyMethodName(JavaParser.MyMethodNameContext ctx) {
        createdName = new ArrayList<String>();
        methodName = ctx.getText();
        methods.put(methodName, completeMethod);
        methodsAndClasses.put(methodName, className);
        if (completeMethod.contains(".substring")
                || completeMethod.contains(".getByte")) {
            createdName.add("String.methods");
        }
        creators.put(methodName, createdName);
    }

    @Override
    public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        completeMethod = extractTextBetweenStartAndStopIndex(ctx);
    }

    /**
     * I think this method tries to generate a method signature that joana can
     * deal with, but it currently only translates a byte array into [B and
     * puts this type into the params map. So, if a methods decl is visited, it
     * a) generates an entry for the paramswithnullable array and 
     * b) does this right here.
     * So yeah, idk. Prolly isnt needed.
     * @param ctx
     */
    @Override
    public void enterFormalParameterList(JavaParser.FormalParameterListContext ctx) {
        String textBetweenStartAndStop = extractTextBetweenStartAndStopIndex(ctx);
        String[] param = textBetweenStartAndStop.split(",");
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
        classList.add(className);
    }
}
