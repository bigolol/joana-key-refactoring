package joanakeyrefactoring;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

public class CombinedApproach {

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
            JoanaAndKeyCheckData parsedCheckData = CombinedApproach.parseInputFile("testdata/plusminusfalsepos.joak");
            CombinedApproach.runTestFromCheckData(parsedCheckData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runTestFromCheckData(JoanaAndKeyCheckData checkData)
            throws ClassHierarchyException, IOException, UnsoundGraphException,
            CancelException, CouldntAddAnnoException, CancelException {
        String classpathJavaM = null;
        AutomationHelper automationHelper = new AutomationHelper(checkData.getPathToJavaFile());
        String allClasses = automationHelper.summarizeSourceFiles();
        ParseJavaForKeyListener javaForKeyListener = new ParseJavaForKeyListener(allClasses);
        automationHelper.setJavaForKeyListener(javaForKeyListener);

        ViolationsViaKeyChecker violationsViaKeyChecker
                = new ViolationsViaKeyChecker(
                        automationHelper, checkData, javaForKeyListener);

        checkData.addAnnotations();
        checkAnnotatedPDGWithJoanaAndKey(checkData.getAnalysis(), violationsViaKeyChecker);
    }

    public static void checkAnnotatedPDGWithJoanaAndKey(
            IFCAnalysis annotatedAnalysis, ViolationsViaKeyChecker violationChecker) throws FileNotFoundException {

        Collection<? extends IViolation<SecurityNode>> violations = annotatedAnalysis.doIFC();

        int numberOfViolations = violations.size();
        int disprovedViolations = 0;

        for (IViolation<SecurityNode> violationNode : violations) {

            boolean disproved = violationChecker.checkViolation(violationNode,
                    annotatedAnalysis.getProgram().getSDG());

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
    
    public static void addAnnotationsFileBased(
            IFCAnalysis ana,
            List<String> annotationsSink,
            List<String> annotationsSource,
            String annotationPath) throws IOException {

        FileInputStream annotationSourceStream = new FileInputStream(annotationPath);
        Collection<IFCAnnotation> annotationCollection
                = ExtractAnnotations.loadAnnotations(annotationSourceStream, ana.getProgram().getSDG());

        annotationCollection.forEach((annotation) -> {
            ana.addAnnotation(annotation);
        });

    }

    /**
     * Reads an input file and extract the path to KeY, classpath to the .jar
     * file, the classpath to the .java file, the entry method and the
     * annotations
     *
     * @param filePath
     */
    public static JoanaAndKeyCheckData parseInputFile(String filePath)
            throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        StringBuilder completeString = new StringBuilder();
        completeString.append("{\n");
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.trim().startsWith("//")) {
                continue;
            }
            completeString.append(line + '\n');
        }
        completeString.append("}\n");

        JSONObject jsonObj = new JSONObject(completeString.toString());

        String pathKeY = jsonObj.getString("pathKeY");
        String pathToJar = jsonObj.getString("pathToJar");
        String pathToJavaFile = jsonObj.getString("pathToJavaFile");
        String entryMethodString = jsonObj.getString("entryMethod");
        JavaMethodSignature entryMethod = JavaMethodSignature.mainMethodOfClass(entryMethodString);
        String annotationPath = jsonObj.getString("annotationPath");
        boolean fullyAutomatic = jsonObj.getBoolean("fullyAutomatic");

        StateSaver stateSaver = new StateSaver();

        IFCAnalysis analysis = runJoanaCreateSDGAndIFCAnalyis(pathToJar, entryMethod, stateSaver);

        JSONArray sources = jsonObj.getJSONArray("sources");
        JSONArray sinks = jsonObj.getJSONArray("sinks");

        ArrayList<SingleAnnotationAdder> singleAnnotationAdders = new ArrayList<>();

        sources.forEach((src) -> {
            singleAnnotationAdders.add(
                    createAnnotationAdder((JSONObject) src, (part, sec) -> {
                        analysis.addSourceAnnotation(part, sec);
                    }, analysis)
            );
        });

        sinks.forEach((src) -> {
            singleAnnotationAdders.add(
                    createAnnotationAdder((JSONObject) src, (part, sec) -> {
                        analysis.addSinkAnnotation(part, sec);
                    }, analysis)
            );
        });

        return new JoanaAndKeyCheckData(
                pathKeY, pathToJar, pathToJavaFile, entryMethodString, annotationPath,
                entryMethod, fullyAutomatic, analysis, singleAnnotationAdders, stateSaver);
    }

    public static SingleAnnotationAdder createAnnotationAdder(JSONObject jsonObj, BiConsumer<SDGProgramPart, String> annoAddMethod, IFCAnalysis analysis) {
        String securityLevel = jsonObj.getString("securityLevel");
        JSONObject description = jsonObj.getJSONObject("description");
        String from = description.getString("from");
        Supplier<Collection<SDGProgramPart>> partSupplier = null;
        if (from.equals("callsToMethod")) {
            int paramPos = description.getInt("paramPos");
            String method = description.getString("method");
            partSupplier
                    = () -> {
                        Collection<SDGCall> allCallsToMethod = analysis.getProgram().getCallsToMethod(JavaMethodSignature.fromString(method));
                        List<SDGProgramPart> collectedParts = allCallsToMethod.stream().map((call) -> {
                            return (SDGProgramPart) call.getActualParameter(paramPos);
                        }).collect(Collectors.toList());
                        return collectedParts;
                    };
        } else if (from.equals("programPart")) {
            String programPartString = description.getString("programPart");
            partSupplier
                    = () -> {
                        return analysis.getProgram().getParts(programPartString).stream().collect(Collectors.toList());
                    };
        }

        return new SingleAnnotationAdder(partSupplier, annoAddMethod, securityLevel);

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
