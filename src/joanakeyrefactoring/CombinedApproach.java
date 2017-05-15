package joanakeyrefactoring;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;

import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

public class CombinedApproach {

    public static String field = "L\\w*\\.\\w*";
    public static RepsRosayChopper chopper;
    public static I2PBackward slicer;
    public static StateSaver state = new StateSaver();
    public static SDGProgram program;
    final static String lineSeparator = System.getProperty("line.separator");
    public static String classpathJavaM;
    public static String javaClass;
    public static String[] paramInClass;
    public static SDG sdg;
    public static String pathKeY;
    public static String classPath;
    public static String classpathJava;
    public static JavaMethodSignature entryMethod;
    public static String entryMethodString;
    public static ArrayList<String> annotationsSink;
    public static ArrayList<String> annotationsSource;
    public static String annotationPath;
    public static boolean debugOutput;

    /**
     * checks a annotated method with JOANA. If alarms are found, they are
     * checked with KeY. Jar files used for this must contain sufficient debug
     * information (parameter names).
     *
     * set jar and class here use appropriate path here
     *
     * Note that the jar must contain parameter names, therefore it has to be
     * compiled with sufficient debug information.
     */
    public static void main(String[] args) throws ClassHierarchyException,
            IOException, UnsoundGraphException, CancelException {
        if (args.length < 1) {
            System.out.println("String[] args is empthy, no input.");
        }
        for (int i = 0; i < args.length; i++) {
            System.out.print(" Param: " + args[i]);
        }
        long timeKeY = 0;
        debugOutput = false;
        boolean fullyAutomatic = true;
        pathKeY = "";

        pathKeY = "/home/holger/Code/hiwi/prototypes/joana-api-prototype-2/keyKeY.jar";
        annotationsSink = new ArrayList<String>();
        annotationsSource = new ArrayList<String>();
        /**
         * choose the project you want to want to analyze *
         */
        String exampleName = "JZip2"; // Voter, JZip, JavaFTP,JZipUnzip

        if (args.length >= 1) {
            String filePath = args[0];
            readInputFile(filePath);
            exampleName = "fileBased";
        } else {
            // setUp(exampleName);
            if (exampleName == "JZip") {
                classPath = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test-Programme\\Marko\\JavaZIP.jar";
                classpathJava = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test-Programme\\Marko\\JZip.java";
                javaClass = "Test-Programme.Test";
            }
            if (exampleName == "JZip2") {
                javaClass = "";
                classPath = "/home/holger/Code/hiwi/repo_2/Combined-Approach/JavaZip/JZipWithException.jar";
                classpathJava = "/home/holger/Code/hiwi/repo_2/Combined-Approach/JavaZip/src/jzip/";
            }
            if (exampleName == "JZipUnzip") {
                classPath = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\JavaZipUnzip\\JZipUnzip.jar";// JZIP,
                // JZipHoleProgram3B,
                // JZipUnchanged,JZipOutputArrayInt
                classpathJava = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\JavaZipUnzip\\src\\jzip";
                javaClass = "";
            }
            if (exampleName == "JavaFTP") {
                classPath = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test-Programme\\Marko\\JavaFTP.jar";
                classpathJava = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test-Programme\\Marko\\JavaFTP.java";
                javaClass = "Test-Programme.Test";
            }
            if (exampleName == "ExampleRecursion") {
                // classPath =
                // "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test-Programme\\Marko\\ExampleRecursion.jar";
                classPath = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test\\ExampleRecursion.jar";
                classpathJava = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test\\ExampleRecursion.java";
                javaClass = "";
            }
            if (exampleName == "ExampleRecursionAlt") {
                classPath = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\Test-Programme\\Marko\\ExampleRecursionAlt.jar";
                classpathJava = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\Test-Programme\\Marko";
                javaClass = "Test-Programme.Test";
            }
            if (exampleName == "Voter") {
                classPath = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\CombinedApproachJar\\examples\\Voter.jar";
                // classPath =
                // "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test-Programme\\Marko\\Voter.jar";
                classpathJava = "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\Test-Programme\\Marko\\Voter.java";
            }
        }

        System.out
                .println("Should the approach run fully automatic (a) or fully precise? (p)");
        Scanner scanInput = new Scanner(System.in);
        String keyAnswer = scanInput.nextLine();

        if (!keyAnswer.equals("a")) {
            fullyAutomatic = false;
        }
        final long timeStart = System.currentTimeMillis();
        Automation auto = new Automation(classpathJava);
        String allClasses = auto.summarizeSourceFiles();
        MyListener ml = new MyListener(allClasses);
        CheckViolations cV = new CheckViolations(auto, javaClass, state,
                fullyAutomatic, pathKeY, ml);
        auto.setMyListener(ml);
        ExtractAnnotations exAnn = new ExtractAnnotations();
        System.out.println("Classes created.");
        final long timeStartJOANA = System.currentTimeMillis();

        // define entry method
        if (exampleName == "JZip") {
            entryMethod = JavaMethodSignature.mainMethodOfClass("jzip/JZip");
        }
        if (exampleName == "JZip2" || exampleName == "JZip3"
                || exampleName == "JZipUnzip") {
            entryMethod = JavaMethodSignature.mainMethodOfClass("jzip/JZip");
        }
        if (exampleName == "JavaFTP") {
            entryMethod = JavaMethodSignature.mainMethodOfClass("JavaFTP/JFTP");
        }
        if (exampleName == "ExampleRecursion") {
            entryMethod = JavaMethodSignature
                    .mainMethodOfClass("Test/ExampleRecursion");
        }
        if (exampleName == "ExampleRecursionAlt") {
            entryMethod = JavaMethodSignature
                    .mainMethodOfClass("ExampleRecursionAlt");
        }
        if (exampleName == "Voter") {
            entryMethod = JavaMethodSignature.mainMethodOfClass("Voter");
        }
        System.out.println("Entry Method set.");
        // run Joana and IFC
        IFCAnalysis ana = runJoana(classPath, entryMethod);
        System.out.println("JOANA ran successfully.");

        /**
         * annotate sources and sinks
         */
        // program.getCallsToMethod("ExampleRecursion.testMethod(III)").getParameter(1),
        SDG sdg = ana.getProgram().getSDG();
        InputStream source = null;
        if (exampleName == "fileBased") {
            // System.out.println(exampleName + ":" + annotationsSink.size() +
            // ":" + annotationsSource.size());
            String an = "";
            if (annotationsSink.size() > 0) {
                for (int i = 0; i < annotationsSink.size(); i++) {
                    an = annotationsSink.get(0);
                    // System.out.println(an.split(",")[0].trim());
                    if (an.split(",")[1].trim().equals("low")) {
                        ana.addSinkAnnotation(
                                program.getPart(an.split(",")[0].trim()),
                                BuiltinLattices.STD_SECLEVEL_LOW);
                    } else {
                        ana.addSinkAnnotation(
                                program.getPart(an.split(",")[0].trim()),
                                BuiltinLattices.STD_SECLEVEL_HIGH);
                    }
                }
            }
            if (annotationsSource.size() > 0) {
                for (int i = 0; i < annotationsSource.size(); i++) {
                    an = annotationsSource.get(0);
                    if (an.split(",")[1].trim().equals("low")) {
                        ana.addSourceAnnotation(
                                program.getPart(an.split(",")[0].trim()),
                                BuiltinLattices.STD_SECLEVEL_LOW);
                    } else {
                        ana.addSourceAnnotation(
                                program.getPart(an.split(",")[0].trim()),
                                BuiltinLattices.STD_SECLEVEL_HIGH);
                    }

                }
            } else {
                source = new FileInputStream(annotationPath);
                Collection<IFCAnnotation> coll = exAnn.loadAnnotations(source,
                        sdg);
                Iterator<IFCAnnotation> itr = coll.iterator();
                while (itr.hasNext()) {
                    ana.addAnnotation(itr.next());
                }
            }
        }
        if (exampleName == "JZip") {
            source = new FileInputStream(
                    "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\jzip.ann");
            Collection<IFCAnnotation> coll = exAnn.loadAnnotations(source, sdg);
            Iterator<IFCAnnotation> itr = coll.iterator();
            while (itr.hasNext()) {
                ana.addAnnotation(itr.next());
            }
        }
        if (exampleName == "JZip2") {

            // ana.addSourceAnnotation(program.getPart("jzip.JZip.CONFIGURATION"),
            // BuiltinLattices.STD_SECLEVEL_HIGH);
            for (SDGCall call : program
                    .getCallsToMethod(JavaMethodSignature
                            .fromString("java.util.Properties.getProperty(Ljava/lang/String;)Ljava/lang/String;"))) {
                System.out.println("Stream Annotation in:"
                        + call.getOwningMethod().toString());
                ana.addSourceAnnotation(call.getActualParameter(1),
                        BuiltinLattices.STD_SECLEVEL_HIGH);
            }

            ana.addSinkAnnotation(
                    program.getPart("jzip.MyFileOutputStream.content"),
                    BuiltinLattices.STD_SECLEVEL_LOW);

        }
        if (exampleName == "JZip3") {
            ana.addSourceAnnotation(
                    program.getPart("jzip.JZip.unZipIt(Ljava/lang/String;Ljava/lang/String;)[B->p1"),
                    BuiltinLattices.STD_SECLEVEL_HIGH);
            ana.addSourceAnnotation(
                    program.getPart("jzip.JZip.unZipIt(Ljava/lang/String;Ljava/lang/String;)[B->p2"),
                    BuiltinLattices.STD_SECLEVEL_LOW);

            ana.addSinkAnnotation(
                    program.getPart("jzip.JZip.unZipIt(Ljava/lang/String;Ljava/lang/String;)[B->exit"),
                    BuiltinLattices.STD_SECLEVEL_LOW);
        }
        if (exampleName == "JZipUnzip") {
            // ana.addSourceAnnotation(
            // program.getPart("jzip.JZip.unZipItByte([B[B)V->p1"),
            // BuiltinLattices.STD_SECLEVEL_LOW);
            // ana.addSourceAnnotation(
            // program.getPart("jzip.JZip.unZipItByte([B[B)V->p2"),
            // // BuiltinLattices.STD_SECLEVEL_HIGH);
            // ana.addSourceAnnotation(
            // program.getPart("jzip.JZip.unZipItExtract([BLjzip/MyZipInputStream;Ljzip/MyFileOutputStream)V->p1"),
            // BuiltinLattices.STD_SECLEVEL_HIGH);

            ana.addSourceAnnotation(
                    program.getPart("jzip.JZip.unZipIt(Ljava/lang/String;Ljava/lang/String;)V->p2"),
                    BuiltinLattices.STD_SECLEVEL_HIGH);
            ana.addSinkAnnotation(
                    program.getPart("jzip.MyFileOutputStream.content"),
                    BuiltinLattices.STD_SECLEVEL_LOW);

            // ana.addSourceAnnotation(
            // program.getPart("jzip.JZip.unZipIt(I)V->p1"),
            // BuiltinLattices.STD_SECLEVEL_HIGH);
            // ana.addSinkAnnotation(
            // program.getPart("jzip.JZip.content"),
            // BuiltinLattices.STD_SECLEVEL_LOW);
            // ana.addSinkAnnotation(
            // program.getPart("jzip.JZip.unZipItByte([B[B)[B->exit"),
            // BuiltinLattices.STD_SECLEVEL_LOW);
            // ana.addSinkAnnotation(
            // program.getPart("jzip.MyFileOutputStream.loc2"),
            // BuiltinLattices.STD_SECLEVEL_LOW);
        }
        if (exampleName == "JavaFTP") {
            source = new FileInputStream(
                    "C:\\Users\\Marko\\Documents\\Uni\\PraxisderForschung\\workspaceJoana\\HybridApproach\\src\\jftp.ann");
            Collection<IFCAnnotation> coll = exAnn.loadAnnotations(source, sdg);
            Iterator<IFCAnnotation> itr = coll.iterator();
            while (itr.hasNext()) {
                ana.addAnnotation(itr.next());
            }
        }
        if (exampleName == "ExampleRecursion") {
            /**
             * ana.addSourceAnnotation( program.getPart(
             * "ExampleRecursion.test(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;->p1"
             * ), BuiltinLattices.STD_SECLEVEL_HIGH); ana.addSourceAnnotation(
             * program.getPart(
             * "ExampleRecursion.test(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;->p2"
             * ), BuiltinLattices.STD_SECLEVEL_LOW); ana.addSinkAnnotation(
             * program.getPart(
             * "ExampleRecursion.test(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;->exit"
             * ), BuiltinLattices.STD_SECLEVEL_LOW);
             */

            ana.addSourceAnnotation(
                    program.getPart("ExampleRecursion.testMethodXX(II)I->p1"),
                    BuiltinLattices.STD_SECLEVEL_HIGH);
            ana.addSourceAnnotation(
                    program.getPart("ExampleRecursion.testMethodXX(II)I->p2"),
                    BuiltinLattices.STD_SECLEVEL_LOW);
            ana.addSinkAnnotation(
                    program.getPart("ExampleRecursion.testMethodXX(II)I->exit"),
                    BuiltinLattices.STD_SECLEVEL_LOW);

        }
        if (exampleName == "ExampleRecursionAlt") {
            ana.addSourceAnnotation(program
                    .getPart("ExampleRecursionAlt.testMethodActive(II)I->p1"),
                    BuiltinLattices.STD_SECLEVEL_HIGH);
            ana.addSourceAnnotation(program
                    .getPart("ExampleRecursionAlt.testMethodActive(II)I->p2"),
                    BuiltinLattices.STD_SECLEVEL_LOW);
            ana.addSinkAnnotation(
                    program.getPart("ExampleRecursionAlt.testMethodActive(II)I->exit"),
                    BuiltinLattices.STD_SECLEVEL_LOW);
        }
        if (exampleName == "Voter") {
            ana.addSinkAnnotation(program.getPart("Voter.low_outputStream"),
                    BuiltinLattices.STD_SECLEVEL_LOW);
            ana.addSinkAnnotation(
                    program.getPart("Voter.low_outputStreamAvailable"),
                    BuiltinLattices.STD_SECLEVEL_LOW);

            ana.addSinkAnnotation(program.getPart("Voter.low_numOfVotes"),
                    BuiltinLattices.STD_SECLEVEL_LOW);
            ana.addSinkAnnotation(program.getPart("Voter.low_sendSuccessful"),
                    BuiltinLattices.STD_SECLEVEL_LOW);
            ana.addSourceAnnotation(program.getPart("Voter.high_inputStream"),
                    BuiltinLattices.STD_SECLEVEL_HIGH);
        }
        System.out.println("Annotations ready");

        /**
         * run the analysis
         */
        // �berlegen, ob ich anden chopper benutze
        // RepsRosayChopper
        // Fluss vom Objekt bei KeY, Fluss vom Objekt oder von Teil vom
        // Object?
        // GroupbyPPpart sonst macht KeY 10mal den gleichen Beweis In
        // Chopper
        // kann man auch collections von Konoten reingeben
        // SDG nodecollector dem programpart dann gruppe von knoten
        // chopper = new SimpleThreadChopper(ana.getProgram().getSDG());
        chopper = new RepsRosayChopper(ana.getProgram().getSDG());
        Collection<? extends IViolation<SecurityNode>> result = ana.doIFC();
        // chopper

        int numberOfViolations = result.size();
        int disprovedViolations = 0;
        // output for tests
        System.out.println(result);
        if (debugOutput) {
            for (IViolation<SecurityNode> v : result) {
                ViolationPath vp = cV.getVP(v);
                System.out.println(vp.toString());
            }
        }
        /**
         * output violations
         */
        // slicer = new I2PBackward(sdg);
        final long timeEndJOANA = System.currentTimeMillis();
        System.out.println("Number of violations:" + numberOfViolations);
        /**
         * care System.exit(0);
         */
        for (IViolation<SecurityNode> v : result) {
            ViolationPath vp = cV.getVP(v);
            /**
             * check individual alarm
             */

            // Zeitmessung
            long timeStartKeY = System.currentTimeMillis();

            boolean disproved = cV.checkViolation(vp,
                    ana.getProgram().getSDG(), chopper);

            long timeEndKeY = System.currentTimeMillis();
            timeKeY += (timeEndKeY - timeStartKeY);

            System.out.println(disproved ? "Success" : "No success");
            if (disproved) {
                disprovedViolations++;
            }
            System.out.println("");
        }

        int remaining = numberOfViolations - disprovedViolations;
        System.out.println(String.format(
                "Found %d violations, disproved %d, remaining %d",
                numberOfViolations, disprovedViolations, remaining));
        if (remaining == 0) {
            System.out.println("Program proven secure!");
        }

        final long timeEnd = System.currentTimeMillis();
        System.out.println("Runtime All: " + (timeEnd - timeStart) / 1000
                + " Sec.");
        System.out.println("Runtime JOANA: " + (timeEndJOANA - timeStartJOANA)
                / 1000 + " Sec.");
        System.out.println("Runtime KeY: " + (timeKeY) / 1000 + " Sec.");
    }

    /**
     * Reads an input file and extract the path to KeY, classpath to the .jar
     * file, the classpath to the .java file, the entry method and the
     * annotations
     *
     * @param filePath
     */
    private static void readInputFile(String filePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line = br.readLine();
            while (line != null) {
                if (line.contains("%") || line.isEmpty()) {
                    line = br.readLine();
                }
                if ((line.contains("pathKeY"))) {
                    pathKeY = line.split("=")[1].trim();
                }
                if ((line.contains("classPath"))) {
                    classPath = line.split("=")[1].trim();
                    System.out.println(classPath);
                }
                if ((line.contains("classpathJava"))) {
                    classpathJava = line.split("=")[1].trim();
                }
                if ((line.contains("entryMethod"))) {
                    entryMethodString = line.split("=")[1].trim();
                    entryMethod = JavaMethodSignature
                            .mainMethodOfClass(entryMethodString);
                }
                if ((line.contains("Add Sink"))) {
                    annotationsSink.add(line.split("\\:")[1].trim());
                }
                if ((line.contains("Add Source"))) {
                    annotationsSource.add(line.split("\\:")[1].trim());
                }
                if ((line.contains("annotationPath"))) {
                    annotationPath = line.split("=")[1].trim();
                }
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static IFCAnalysis runJoana(String classPath,
            JavaMethodSignature entryMethod) throws ClassHierarchyException,
            IOException, UnsoundGraphException, CancelException {
        /**
         * some settings
         */
        SDGConfig config = new SDGConfig(classPath, entryMethod.toBCString(),
                Stubs.JRE_14);
        config.setComputeInterferences(true);
        config.setMhpType(MHPType.PRECISE);
        config.setPointsToPrecision(PointsToPrecision.INSTANCE_BASED);
        config.setExceptionAnalysis(ExceptionAnalysis.INTERPROC);
        config.setFieldPropagation(FieldPropagation.OBJ_GRAPH_NO_MERGE_AT_ALL);

        /**
         * save intermediate results of SDG generation (i.e. points-to, call
         * graph)
         */
        config.setCGConsumer(state);
        /**
         * Schneidet beim SDG application edges raus, so besser sichtbar mit dem
         * graphviewer
         *
         */
        config.setPruningPolicy(ApplicationLoaderPolicy.INSTANCE);

        /**
         * build the PDG etc.
         */
        program = SDGProgram.createSDGProgram(config, System.out,
                new NullProgressMonitor());
        sdg = program.getSDG();

        // Immutable
        // Analysis Scope
        // SDGBuilder.build(scfg);
        /**
         * save PDG
         */
        SDGSerializer.toPDGFormat(program.getSDG(), new FileOutputStream(
                "SDG.pdg"));

        IFCAnalysis ana = new IFCAnalysis(program);
        return ana;
    }
}
