package joanakeyrefactoring;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import java.io.FileNotFoundException;
import java.util.List;

public class CombinedApproach {

    public static String field = "L\\w*\\.\\w*";
    public static RepsRosayChopper chopper;
    public static I2PBackward slicer;
    public static String[] paramInClass;
    public static SDG sdg;
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
    public static void main(String[] args) {
        try {
            JoanaAndKeyCheckData parsedCheckData = CombinedApproach.parseInputFile("testdata/jzip.joak");
            CombinedApproach.runTestFromCheckData(parsedCheckData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runTestFromCheckData(JoanaAndKeyCheckData checkData)
            throws ClassHierarchyException, IOException, UnsoundGraphException,
            CancelException, CouldntAddAnnoException, CancelException {
        String classpathJavaM = null;
        StateSaver stateSaver = new StateSaver();
        AutomationHelper automationHelper = new AutomationHelper(checkData.getPathToJavaFile());
        String allClasses = automationHelper.summarizeSourceFiles();
        ParseJavaForKeyListener javaForKeyListener = new ParseJavaForKeyListener(allClasses);
        automationHelper.setJavaForKeyListener(javaForKeyListener);

        ViolationsViaKeyChecker violationsViaKeyChecker
                = new ViolationsViaKeyChecker(
                        automationHelper, checkData, stateSaver, javaForKeyListener);

        IFCAnalysis analysis = runJoanaCreateSDGAndIFCAnalyis(
                checkData.getPathToJar(),
                checkData.getEntryMethod(),
                stateSaver);
        checkData.addAnnotations(analysis);
        checkAnnotatedPDGWithJoanaAndKey(analysis, violationsViaKeyChecker);
    }

    public static void checkAnnotatedPDGWithJoanaAndKey(
            IFCAnalysis annotatedAnalysis, ViolationsViaKeyChecker violationChecker) throws FileNotFoundException {
        chopper = new RepsRosayChopper(annotatedAnalysis.getProgram().getSDG());
        Collection<? extends IViolation<SecurityNode>> violations = annotatedAnalysis.doIFC();

        int numberOfViolations = violations.size();
        int disprovedViolations = 0;

        for (IViolation<SecurityNode> v : violations) {
            ViolationPath vp = violationChecker.getVP(v);

            boolean disproved = violationChecker.checkViolation(vp,
                    annotatedAnalysis.getProgram().getSDG(), chopper);

            if (disproved) {
                disprovedViolations++;
            }
        }

        int remaining = numberOfViolations - disprovedViolations;

        System.out.println(String.format(
                "Found %d violations, disproved %d, remaining %d",
                numberOfViolations, disprovedViolations, remaining));
        if (remaining == 0) {
            System.out.println("Program proven secure!");
        }
    }

    public static void addJzip2Annotations(IFCAnalysis ana) {
        SDGProgram program = ana.getProgram();
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

    public static void addAnnotationsFileBased(
            IFCAnalysis ana,
            List<String> annotationsSink,
            List<String> annotationsSource,
            String annotationPath) throws IOException {

        String lineSeparator = System.getProperty("line.separator");
        String an = "";

        SDGProgram program = ana.getProgram();
        SDG sdg = program.getSDG();

        if (annotationsSink.size() > 0) {
            for (int i = 0; i < annotationsSink.size(); i++) {
                parseSinkAnnotation(annotationsSink.get(i), ana, program);
            }
        }
        if (annotationsSource.size() > 0) {
            for (int i = 0; i < annotationsSource.size(); i++) {
                parseSourceAnnotation(annotationsSource.get(i), ana, program);
            }
        } else {
            FileInputStream annotationSourceStream = new FileInputStream(annotationPath);
            Collection<IFCAnnotation> coll
                    = ExtractAnnotations.loadAnnotations(annotationSourceStream, sdg);
            Iterator<IFCAnnotation> itr = coll.iterator();
            while (itr.hasNext()) {
                ana.addAnnotation(itr.next());
            }
        }
    }

    private static void parseSourceAnnotation(String sourceString, IFCAnalysis ana, SDGProgram program) {
        String programPart = "programPart";
        String secLevel = parseSecLevel(sourceString);
        String kind = parseAnnoKind(sourceString);
        String desc = parseAnnoDesc(sourceString);
        if (desc.equals(programPart)) {
            ana.addSourceAnnotation(program.getPart(desc), secLevel);
        }
    }

    private static void parseSinkAnnotation(String sinkString, IFCAnalysis ana, SDGProgram program) {
        String programPart = "programPart";
        String secLevel = parseSecLevel(sinkString);
        String kind = parseAnnoKind(sinkString);
        String desc = parseAnnoDesc(sinkString);
        if (desc.equals(programPart)) {
            ana.addSinkAnnotation(program.getPart(desc), secLevel);
        }
    }

    /**
     * Reads an input file and extract the path to KeY, classpath to the .jar
     * file, the classpath to the .java file, the entry method and the
     * annotations
     *
     * @param filePath
     */
    public static JoanaAndKeyCheckData parseInputFile(String filePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine();
        String pathKeY = null;
        String pathToJar = null;
        String pathToJavaFile = null;
        String entryMethodString = null;
        String annotationPath = null;
        JavaMethodSignature entryMethod = null;
        boolean fullyAutomatic = true;
        List<String> annotationsSource = new ArrayList<>();
        List<String> annotationsSink = new ArrayList<>();
        while (line != null) {
            if (line.trim().startsWith("//") || line.isEmpty()) {
                line = br.readLine();
            }
            if ((line.contains("pathKeY"))) {
                pathKeY = line.split("=")[1].trim();
            }
            if ((line.contains("pathToJar"))) {
                pathToJar = line.split("=")[1].trim();
            }
            if ((line.contains("pathToJavaFile"))) {
                pathToJavaFile = line.split("=")[1].trim();
            }
            if ((line.contains("entryMethod"))) {
                entryMethodString = line.split("=")[1].trim();
                entryMethod = JavaMethodSignature
                        .mainMethodOfClass(entryMethodString);
            }
            if ((line.contains("Add Sink"))) {
                annotationsSink.add(line.split(":")[1].trim());
            }
            if ((line.contains("Add Source"))) {
                annotationsSource.add(line.split(":")[1].trim());
            }
            if ((line.contains("annotationPath"))) {
                annotationPath = line.split("=")[1].trim();
            }
            if ((line.contains("fullyAutomatic"))) {
                String fullyAutoStr = line.split("=")[1].trim();
                fullyAutomatic = Boolean.valueOf(fullyAutoStr);
            }

            line = br.readLine();
        }
        br.close();
        return new JoanaAndKeyCheckData(
                annotationsSink, annotationsSource,
                pathKeY, pathToJar,
                pathToJavaFile, entryMethodString,
                annotationPath, entryMethod, fullyAutomatic,
                (analysis, checkData) -> {
                    try {
                        addAnnotationsFileBased(
                                analysis,
                                checkData.getAnnotationsSink(),
                                checkData.getAnnotationsSink(),
                                checkData.getAnnotationPath());
                    } catch (IOException e) {
                        throw new CouldntAddAnnoException();
                    }
                });
    }

    public static IFCAnalysis runJoanaCreateSDGAndIFCAnalyis(String pathToJar,
            JavaMethodSignature entryMethod, StateSaver stateSaver) throws ClassHierarchyException,
            IOException, UnsoundGraphException, CancelException {
        SDGConfig config = new SDGConfig(
                pathToJar, entryMethod.toBCString(),
                Stubs.JRE_14);
        config.setComputeInterferences(true);
        config.setMhpType(MHPType.PRECISE);
        config.setPointsToPrecision(PointsToPrecision.INSTANCE_BASED);
        config.setExceptionAnalysis(ExceptionAnalysis.INTERPROC);
        config.setFieldPropagation(FieldPropagation.OBJ_GRAPH_NO_MERGE_AT_ALL);

        // save intermediate results of SDG generation points to call graph
        config.setCGConsumer(stateSaver);
        // Schneidet beim SDG application edges raus, so besser sichtbar mit dem graphviewer
        config.setPruningPolicy(ApplicationLoaderPolicy.INSTANCE);

        SDGProgram program = SDGProgram.createSDGProgram(config, System.out,
                new NullProgressMonitor());
        sdg = program.getSDG();
        IFCAnalysis ana = new IFCAnalysis(program);
        return ana;
    }

    public static String parseSecLevel(String annotationString) {
        //format: from part, jzip.MyFileOutputStream.content, low
        String secLevelStr = annotationString.split(",")[2].trim();
        if (secLevelStr.equals("high")) {
            return BuiltinLattices.STD_SECLEVEL_HIGH;
        }
        return BuiltinLattices.STD_SECLEVEL_LOW;
    }

    public static String parseAnnoKind(String annotationString) {
        return annotationString.split(",")[0].trim();
    }

    public static String parseAnnoDesc(String annotationString) {
        return annotationString.split(",")[1].trim();
    }
}
