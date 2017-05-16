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
        boolean fullyAutomatic = true;
        boolean filebased = false;
        pathKeY = "/home/holger/Code/hiwi/prototypes/joana-api-prototype-2/keyKeY.jar";
        javaClass = "";
        classPath = "/home/holger/Code/hiwi/repo_2/Combined-Approach/JavaZip/JZipWithException.jar";
        classpathJava = "/home/holger/Code/hiwi/repo_2/Combined-Approach/JavaZip/src/jzip/";
        entryMethod = JavaMethodSignature.mainMethodOfClass("jzip/JZip");
        
        annotationsSink = new ArrayList<String>();
        annotationsSource = new ArrayList<String>();

        Automation auto = new Automation(classpathJava);
        String allClasses = auto.summarizeSourceFiles();
        MyListener ml = new MyListener(allClasses);
        CheckViolations cV = new CheckViolations(auto, javaClass, state, fullyAutomatic, pathKeY, ml);
        auto.setMyListener(ml);
        ExtractAnnotations exAnn = new ExtractAnnotations();

        IFCAnalysis ana = runJoanaCreateSDGAndIFCana(classPath, entryMethod);

        SDG sdg = ana.getProgram().getSDG();
        InputStream source = null;
        if (filebased) {
            addAnnotationsFileBased(ana, exAnn, sdg);
        } else {
            addJzip2Annotations(ana);
        }

        chopper = new RepsRosayChopper(ana.getProgram().getSDG());
        Collection<? extends IViolation<SecurityNode>> violations = ana.doIFC();
        
        int numberOfViolations = violations.size();
        int disprovedViolations = 0;

        for (IViolation<SecurityNode> v : violations) {
            ViolationPath vp = cV.getVP(v);

            boolean disproved = cV.checkViolation(vp,
                    ana.getProgram().getSDG(), chopper);

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

    private static void addJzip2Annotations(IFCAnalysis ana) {
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

    private static void addAnnotationsFileBased(IFCAnalysis ana, ExtractAnnotations exAnn, SDG sdg1) throws IOException {
        InputStream source;
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
            Collection<IFCAnnotation> coll = exAnn.loadAnnotations(source, sdg1);
            Iterator<IFCAnnotation> itr = coll.iterator();
            while (itr.hasNext()) {
                ana.addAnnotation(itr.next());
            }
        }
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

    private static IFCAnalysis runJoanaCreateSDGAndIFCana(String classPath,
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
