package joanakeyrefactoring;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles all automation processes of the Combined Approach.
 * Summarize files, export methods and classes, run KeY, open files ...
 *
 * @author Marko Kleine B�ning
 *
 */
public class AutomationHelper {

    private final String pathToJavaFile;
    private String[] paramInClass;
    private ArrayList<String> classNames = new ArrayList<>();
    final static String lineSeparator = System.getProperty("line.separator");
    private ParseJavaForKeyListener javaForKeyListener;
    private HashMap<String, String> classes = new HashMap<>();

    public AutomationHelper(String pathToJavaFile) {
        this.pathToJavaFile = pathToJavaFile;
    }

    /**
     * @return an array with all class names
     */
    public ArrayList<String> getClassNames() {
        return classNames;
    }

    public ParseJavaForKeyListener generateParseJavaForKeyListener() {
        this.javaForKeyListener = new ParseJavaForKeyListener(readAllSourceFilesIntoOneStringAndFillClassMap());;
        return this.javaForKeyListener;
    }

    /**
     * @return a String summarizing all .java files
     */
    public String readAllSourceFilesIntoOneStringAndFillClassMap() {
        StringBuilder stringBuilder = new StringBuilder();
        final File folder = new File(pathToJavaFile);
        Collection<File> javaFiles = listAllJavaFilesInFolder(folder);
        javaFiles.forEach((file) -> {
            String fileContent = putFileContentsIntoStringAndIntoClassMap(file);
            stringBuilder.append(fileContent);
            stringBuilder.append(System.lineSeparator());
        });
        return stringBuilder.toString();
    }

    public String putFileContentsIntoStringAndIntoClassMap(File file) {
        StringBuilder stringBuilderForFile = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!lineIsntPackageDecl(line)) {
                    stringBuilderForFile.append(line);
                    stringBuilderForFile.append(System.lineSeparator());
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AutomationHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AutomationHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        classes.put(file.getName().split("\\.")[0], stringBuilderForFile.toString());
        return stringBuilderForFile.toString();
    }

    private static boolean lineIsntPackageDecl(String line) {
        return line.contains("package");
    }

    /**
     * Lists all files that are in the folder. Used for summarizing all java
     * files of a package.
     *
     * @param folder becomes a folder of .java files
     * @return ArrayList of all files in the folder
     */
    public Collection<File> listAllJavaFilesInFolder(final File folder) {
        List<File> fileNames = new ArrayList<File>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listAllJavaFilesInFolder(fileEntry);
            } else {
                if (fileEntry.getName().trim().endsWith(".java")) {
                    classNames.add(fileEntry.getName().split("\\.")[0]);
                    fileNames.add(fileEntry);
                }
            }
        }
        return fileNames;
    }

    /**
     * Exports all necessary code into one file. uses createloopinvariant for
     * some reason
     *
     * @param descriptionForKey
     * @param methodName
     * @param descSink
     * @param descOtherParams
     * @return the parameter of the method that is to be proven
     */
    public String[] exportJava(
            String descriptionForKey, String methodName, String descSink,
            String descOtherParams) {
        ArrayList<String> allMethodNames = new ArrayList<>();

        // TODO: use Parser change classpathJava to classpathPackage
        // Global Variables with Parser:
        String globalVariables = "";
        globalVariables = javaForKeyListener.getFieldsCorrectAsString();
        // complete method with Java
        String completeMethod = "";
        if (methodName.contains("<init>")) {
            methodName = methodName.split("\\.")[1];
            completeMethod = javaForKeyListener.getConstructorOfMethod(methodName);
        } else {
            completeMethod = javaForKeyListener.getCompleteMethod(methodName);
            String firstLine = javaForKeyListener.getParamsWithNullable(methodName);
            String[] array = completeMethod.split(System.lineSeparator());
            StringBuilder sb = new StringBuilder();
            sb.append(array[0].split("\\(")[0]);
            sb.append(firstLine + "{");
            sb.append(System.lineSeparator());
            for (int i = 1; i < array.length; i++) {
                // Loop Invariants:
                String lineLoop = array[i];
                if (lineLoop.contains("for(") || lineLoop.contains("for (")
                        || lineLoop.contains("while(")
                        || lineLoop.contains("while (")) {
                    String loopInv = KeyStringGenerator.createLoopInvariant(descSink,
                            descOtherParams, methodName, "");
                    array[i] = loopInv + System.lineSeparator() + array[i];
                }

                sb.append(array[i]);
                sb.append(System.lineSeparator());
            }
            completeMethod = sb.toString();
        }

        System.out.println("complete Method: " + completeMethod);

        String otherClasses = "";
        StringBuilder sbClasses = new StringBuilder();
        String classOfMethod = javaForKeyListener.getClass(methodName);
        // System.out.println("classOfMethod " + classOfMethod);
        List<String> classList = javaForKeyListener.getClassList();
        for (int i = 0; i < classList.size(); i++) {
            if (!classList.get(i).equals(classOfMethod)) {
                sbClasses.append(classes.get(classList.get(i)));
            }
            sbClasses.append(System.lineSeparator());
        }
        otherClasses = sbClasses.toString();

        // get Params String[]
        paramInClass = javaForKeyListener.getParamsOfMethod(methodName);
        System.out.println("Params: " + paramInClass);

        // all methods that are needed to run the complete method
        String allOtherMethods = "";
        StringBuilder sbOM = new StringBuilder();
        String[] lines = completeMethod.split(System
                .getProperty("line.separator"));

        for (String line : lines) {
            String pattern = "(.*)([a-zA-Z1-9]+)(\\s*)([=a-zA-Z1-9]+)(\\s*)([a-zA-Z1-9]+)([a-zA-Z1-9]*)(\\()([a-zA-Z]+)(.*)";
            // Create Pattern object
            Pattern r = Pattern.compile(pattern);
            // Create matcher object.
            Matcher m = r.matcher(line);
            String lineOc = line;
            if (m.find() && !lineOc.contains("\\\\")) {
                if (lineOc.contains("=")) {
                    lineOc = lineOc.split("=")[1];
                }
                lineOc = lineOc.split("\\(")[0];
                if (!allMethodNames.contains(lineOc.trim())) {
                    allMethodNames.add(lineOc.trim());
                    if (javaForKeyListener.getCompleteMethod(lineOc.trim()) != null) {
                        sbOM.append(javaForKeyListener.getCompleteMethod(lineOc.trim()));
                        sbOM.append(System.lineSeparator());
                    }
                }
            }
        }
        allOtherMethods = sbOM.toString();

        // write specification and source code in file
        PrintWriter writer;
        try {
            writer = new PrintWriter("proofs\\sourceFile.java", "UTF-8");
            writer.println("package proofs;");
            writer.println("public class sourceFile{");
            writer.println(globalVariables);
            writer.println(descriptionForKey);
            writer.println(completeMethod);
            writer.println(allOtherMethods);
            writer.println(System.lineSeparator() + "}");
            writer.println(otherClasses);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return paramInClass;
    }

    /**
     * Creates the Information flow Proof Obligation for KeY
     *
     * @param javaFile
     * @param method
     */
    public void createKeYFile(String javaFile, String method) {
        PrintWriter writer;
        try {
            writer = new PrintWriter("proofObIF.key", "UTF-8");
            String firstRow = "\\profile \"Java Profile\";";
            writer.println(firstRow);
            // Java Source
            String js = "\\javaSource \"proofs\";";
            writer.println(js);
            // Proof Obligation
            String p1 = "\\proofObligation \"#Proof Obligation Settings";
            writer.println(p1);
            String obliName = "name = " + javaFile + "[" + javaFile
                    + "\\\\:\\\\:" + method + "].Non-interference contract.0";
            writer.println(obliName);
            String obliContract = "contract = " + javaFile + "[" + javaFile
                    + "\\\\:\\\\:" + method + "].Non-interference contract.0";
            writer.println(obliContract);
            // String obliClass =
            // "class=de.uka.ilkd.key.proof.init.InfFlowContractPO";
            String obliClass = "class=de.uka.ilkd.key.informationflow.po.InfFlowContractPO";
            writer.println(obliClass);
            String end = "\";";
            writer.println(end);

            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the Functional Proof Obligation for KeY
     *
     * @param javaFile
     * @param method
     */
    public void createKeYFileFunctional(String javaFile, String method) {
        PrintWriter writer;
        try {
            writer = new PrintWriter("proofObFunc.key", "UTF-8");
            String firstRow = "\\profile \"Java Profile\";";
            writer.println(firstRow);
            // Java Source
            String js = "\\javaSource \"proofs\";";
            writer.println(js);
            // Proof Obligation
            String p1 = "\\proofObligation \"#Proof Obligation Settings";
            writer.println(p1);
            String obliName = "name = " + javaFile + "[" + javaFile
                    + "\\\\:\\\\:" + method + "].JML operation contract.0";
            writer.println(obliName);
            String obliContract = "contract = " + javaFile + "[" + javaFile
                    + "\\\\:\\\\:" + method + "].JML operation contract.0";
            writer.println(obliContract);
            // String obliClass =
            // "class=de.uka.ilkd.key.proof.init.InfFlowContractPO";
            String obliClass = "class=de.uka.ilkd.key.proof.init.FunctionalOperationContractPO";
            writer.println(obliClass);
            String end = "\";";
            writer.println(end);

            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    /**
     * runs KeY automatically
     *
     * @param pathKeY
     * @param obligation
     * @return result of the proof
     */
    public boolean runKeY(String pathKeY, String obligation) {
        boolean result = false;
        String cmd = "java -Xmx512m -jar " + pathKeY + " --auto proofObIF.key";
        if (obligation == "functional") {
            cmd = "java -Xmx512m -jar " + pathKeY + " --auto proofObFunc.key";
        }
        Runtime r = Runtime.getRuntime();
        Process pr;
        try {
            pr = r.exec(cmd);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    pr.getInputStream()));

            String s;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                if (s.contains("Number of goals remaining open: 0")) {
                    result = true;
                }
                // TODO:
                if (s.contains("Proof loading failed")) {
                    result = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Opens the program KeY for a manual proof.
     *
     * @param fileName
     * @param methodName
     * @return the result of the proof
     */
    public boolean openKeY(String fileName, String methodName) {
        boolean result = false;
        String cmd = "java -Xmx1024m -jar C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\KeY\\key\\key\\deployment\\KeY.jar";
        Runtime r = Runtime.getRuntime();
        Process pr;
        try {
            pr = r.exec(cmd);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    pr.getInputStream()));

            String s;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                if (s.contains("Number of goals remaining open: 0")) {
                    result = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Opens a file on the desktop. Is used to open the java .java file for the
     * key proof.
     *
     * @param file
     */
    public void openJava(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().edit(file);

            } else {
                System.out
                        .println("System is not DesktopSupported. Was not able to open .java File.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Currently not used but can be used, if furhter properties are needed for
     * the KeY proof
     *
     * @return settings
     */
    public String getSettings() {
        String settings = "\\settings {"
                + lineSeparator
                + " \"#Proof-Settings-Config-File"
                + lineSeparator
                + "#Wed Jun 10 12:53:58 CEST 2016"
                + lineSeparator
                + "[StrategyProperty]VBT_PHASE=VBT_SYM_EX"
                + lineSeparator
                + "[SMTSettings]useUninterpretedMultiplication=true"
                + lineSeparator
                + "[SMTSettings]SelectedTaclets="
                + lineSeparator
                + "[StrategyProperty]METHOD_OPTIONS_KEY=METHOD_CONTRACT"
                + lineSeparator
                + "[StrategyProperty]USER_TACLETS_OPTIONS_KEY3=USER_TACLETS_OFF"
                + lineSeparator
                + "[StrategyProperty]SYMBOLIC_EXECUTION_ALIAS_CHECK_OPTIONS_KEY=SYMBOLIC_EXECUTION_ALIAS_CHECK_NEVER"
                + lineSeparator
                + "[StrategyProperty]LOOP_OPTIONS_KEY=LOOP_INVARIANT"
                + lineSeparator
                + "[StrategyProperty]USER_TACLETS_OPTIONS_KEY2=USER_TACLETS_OFF"
                + lineSeparator
                + "[StrategyProperty]USER_TACLETS_OPTIONS_KEY1=USER_TACLETS_OFF"
                + lineSeparator
                + "[StrategyProperty]QUANTIFIERS_OPTIONS_KEY=QUANTIFIERS_NON_SPLITTING_WITH_PROGS"
                + lineSeparator
                + "[StrategyProperty]NON_LIN_ARITH_OPTIONS_KEY=NON_LIN_ARITH_NONE"
                + lineSeparator
                + "[SMTSettings]instantiateHierarchyAssumptions=true"
                + lineSeparator
                + "[StrategyProperty]AUTO_INDUCTION_OPTIONS_KEY=AUTO_INDUCTION_OFF"
                + lineSeparator
                + "[StrategyProperty]DEP_OPTIONS_KEY=DEP_ON"
                + lineSeparator
                + "[StrategyProperty]BLOCK_OPTIONS_KEY=BLOCK_CONTRACT"
                + lineSeparator
                + "[StrategyProperty]CLASS_AXIOM_OPTIONS_KEY=CLASS_AXIOM_FREE"
                + lineSeparator
                + "[StrategyProperty]SYMBOLIC_EXECUTION_NON_EXECUTION_BRANCH_HIDING_OPTIONS_KEY=SYMBOLIC_EXECUTION_NON_EXECUTION_BRANCH_HIDING_OFF"
                + lineSeparator
                + "[StrategyProperty]QUERY_NEW_OPTIONS_KEY=QUERY_OFF"
                + lineSeparator
                + "[Strategy]Timeout=-1"
                + lineSeparator
                + "[Strategy]MaximumNumberOfAutomaticApplications=10000"
                + lineSeparator
                + "[SMTSettings]integersMaximum=2147483645"
                + lineSeparator
                + "[Choice]DefaultChoices=assertions-assertions\\:on , initialisation-initialisation\\:disableStaticInitialisation , intRules-intRules\\:arithmeticSemanticsIgnoringOF , programRules-programRules\\:Java , JavaCard-JavaCard\\:on , Strings-Strings\\:on , modelFields-modelFields\\:treatAsAxiom , bigint-bigint\\:on , sequences-sequences\\:on , reach-reach\\:on , integerSimplificationRules-integerSimplificationRules\\:full , wdOperator-wdOperator\\:L , wdChecks-wdChecks\\:off , runtimeExceptions-runtimeExceptions\\:ban"
                + lineSeparator
                + "[SMTSettings]useConstantsForBigOrSmallIntegers=true"
                + lineSeparator
                + "[StrategyProperty]STOPMODE_OPTIONS_KEY=STOPMODE_DEFAULT"
                + lineSeparator
                + "[StrategyProperty]QUERYAXIOM_OPTIONS_KEY=QUERYAXIOM_OFF"
                + lineSeparator
                + "[StrategyProperty]INF_FLOW_CHECK_PROPERTY=INF_FLOW_CHECK_FALSE"
                + lineSeparator + "[SMTSettings]maxGenericSorts=2"
                + lineSeparator + "[SMTSettings]integersMinimum=-2147483645"
                + lineSeparator + "[SMTSettings]invariantForall=false"
                + lineSeparator + "[SMTSettings]UseBuiltUniqueness=false"
                + lineSeparator + "[SMTSettings]explicitTypeHierarchy=false"
                + lineSeparator + "[Strategy]ActiveStrategy=JavaCardDLStrategy"
                + lineSeparator
                + "[StrategyProperty]SPLITTING_OPTIONS_KEY=SPLITTING_DELAYED"
                + lineSeparator + "	\"" + lineSeparator + "}";
        return settings;
    }

    public void setJavaForKeyListener(ParseJavaForKeyListener javaForKeyListener) {
        this.javaForKeyListener = javaForKeyListener;
    }

}
