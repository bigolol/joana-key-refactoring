package joanakeyrefactoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation.Chop;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;

public class ViolationsViaKeyChecker {

    public String[] paramInClass;
    public String javaClass;
    public RepsRosayChopper chopper;
    public StateSaver state;
    private AutomationHelper auto;
    private boolean fullyAutomatic;
    private String pathKeY;
    private ArrayList<String> keyFeatures = new ArrayList<String>();
    private MyListener ml;

    public ViolationsViaKeyChecker(AutomationHelper auto2, String javaClass,
            StateSaver state, boolean fullyAutomatic, String pathKeY,
            MyListener ml) {
        this.ml = ml;
        this.auto = auto2;
        this.javaClass = javaClass;
        this.state = state;
        this.fullyAutomatic = fullyAutomatic;
        this.pathKeY = pathKeY;
        loadAndAddList();
    }

    /**
     * checks a violation (information about one supposed illegal flow) uses KeY
     * to check whether this is a false alarm
     *
     * @return true if there is no illegal flow
     * @throws FileNotFoundException
     */
    public boolean checkViolation(ViolationPath vp, SDG sdg,
            RepsRosayChopper chopper2) throws FileNotFoundException {
        File file = new File("proofs\\sourceFile.java");
        boolean neueHeuristic = true;
        this.chopper = chopper2;
        LinkedList<SecurityNode> list = vp.getPathList();
        SDGNode source = list.get(0);
        SDGNode sink = list.get(1);
        //get collection of nodes involved in illegal flow
        Collection<SDGNode> c = chopper.chop(source, sink);
        if (c.isEmpty()) {
            return true;
        }
        //get edges involved in the flow
        SDG flowSDG = sdg.subgraph(c);
        SDGSerializer
                .toPDGFormat(flowSDG, new FileOutputStream("subgraph.pdg"));
        List<EdgeMetric> summaryEdges;
        List<SDGEdge> checkedEdges = new ArrayList<SDGEdge>();
        boolean change = true;
        while (change) {
            change = false;
            summaryEdges = getSummaryEdges(flowSDG, source, sink, checkedEdges,
                    sdg, neueHeuristic);
            for (EdgeMetric em : summaryEdges) {
                SDGEdge e = em.e;
                SDGNode v = e.getSource();
                SDGNode w = e.getTarget();
                boolean removable = true;

//              check all possible method invocations; needed in case of
//              dynamic dispatch
                Collection<SDGNodeTuple> callPairs = sdg
                        .getAllFormalPairs(v, w);
                for (SDGNodeTuple t : callPairs) {
                    //get source and sink node in the callee that induce the
                    //summary edge
                    SDGNode p = t.getFirstNode();
                    SDGNode r = t.getSecondNode();
                    // skip methods that are already secure
                    if (chopper.chop(p, r).isEmpty()) {
                        continue;
                    }
                    SDGNode callee = sdg.getEntry(p);
                    //generate spec for KeY
                    String descSink = descSink(r, sdg);
                    String descOtherParams = descOtherParams(p, sdg);
                    String a1 = callee.getBytecodeMethod();
                    Boolean javaLibary = false;
                    if (a1.contains("java.") || a1.contains("lang")) {
                        javaLibary = true;
                    }
                    System.out.println("Bytecodename: " + a1);
                    String b = "\t/*@ requires " + pointsTo(sdg, callee)
                            + ";\n\t  @ determines " + descSink + " \\by "
                            + descOtherParams + "; */";
                    String[] a2 = a1.split("\\.");
                    String[] a3 = a2[a2.length - 1].split("\\(");
                    String methodName = a3[0];
                    if (a1.contains("<init>")) {
                        methodName += "." + a2[a2.length - 2].split("\\(")[0];
                    }
                    if (!isKeyCompatible(methodName, javaLibary)) {
                        System.out.println("Break, class is not compatible");
                        removable = false;
                        break;
                    }
                    // System.out.println("descSink: " + descSink);
                    // System.out.println("descOtherParams: " +
                    // descOtherParams);
                    if (descSink == null || descOtherParams == null) {
                        /**
                         * How to check such a method with KeY?
                         */
                        removable = false;
                        System.out
                                .print("!DescSink or DescOtherParams = null. For nodes:"
                                        + p + ", " + r + "/");
                        System.out.print("descSink:" + descSink
                                + ", descOtherParams" + descOtherParams + "/");
                        System.out.println("/ in method "
                                + p.getBytecodeMethod() + "and: "
                                + r.getBytecodeMethod());
                        break;
                    }
                    System.out.println("test method\n\t"
                            + callee.getBytecodeMethod() + "\nwith spec:");
                    System.out.println("\t/*@ requires "
                            + pointsTo(sdg, callee) + ";\n\t  @ determines "
                            + descSink + " \\by " + descOtherParams + "; */");

                    // wirte method to same file below
                    paramInClass = auto.exportJava(b, methodName, descSink,
                            descOtherParams);
                    // create .key file
                    String params = "";
                    if (paramInClass != null) {
                        for (int i = 0; i < paramInClass.length; i++) {
                            if (i == 0) {
                                params += paramInClass[i];
                            } else {
                                params += "," + paramInClass[i];
                            }
                        }
                    }
                    String methodNameKeY = methodName + "(" + params + ")";
                    String newJavaFile = "proofs.sourceFile";
                    auto.createKeYFile(newJavaFile, methodNameKeY);
                    // executeKeY with parameter
                    // read result
                    long timeStartKeY = System.currentTimeMillis();
                    boolean result = auto.runKeY(pathKeY, "information flow");
                    boolean resultFunc = auto.runKeY(pathKeY, "functional");
                    System.out.println("Information Flow Result: " + result);
                    System.out.println("Functional Result: " + resultFunc);

                    long timeEndKeY = System.currentTimeMillis();
                    System.out.println("Runtime KeYProof: "
                            + (timeEndKeY - timeStartKeY) / 1000 + " Sec.");

                    if (!result || !resultFunc) {
                        System.out
                                .println("Could not proof method automatically.");
                        if (!fullyAutomatic) {
                            System.out.println("From node: " + p + " to node: "
                                    + r);
                            System.out
                                    .println("type \"y\" to verify method manually or \"n\" to go on automatically ");
                            Scanner scanInput = new Scanner(System.in);
                            String keyAnswer = scanInput.nextLine();
                            if (keyAnswer.equals("y")) {
                                // open JAVA and KeY
                                auto.openJava(file);
                                auto.openKeY(javaClass, methodNameKeY);

                                System.out.println("type y if KeY could prove");
                                Scanner scanInput2 = new Scanner(System.in);
                                String keyAnswer2 = scanInput2.nextLine();

                                if (!keyAnswer2.equals("y")) {
                                    removable = false;
                                    break;
                                } else {
                                    result = true;
                                }
                            }
                        }
                    }
                    if (!result || !resultFunc) {
                        removable = false;
                        break;
                    }

                    /**
                     * System.out.println("type y if KeY could prove"); Scanner
                     * scanInput = new Scanner(System.in); String keyAnswer =
                     * scanInput.nextLine();
                     *
                     * if only one invocation is not found secure, we cannot
                     * delete that summary edge
                     *
                     * if (!keyAnswer.equals("y")) { removable = false; break; }
                     */
                }
                if (removable) {
                    /**
                     * remove the summary edge
                     */
                    sdg.removeEdge(e);
                    flowSDG.removeEdge(e);
                    /**
                     * recalculating of the chop after deleting the summary
                     * edge; if the new chop is empty, our alarm is found to be
                     * a false alarm
                     */
                    c = chopper.chop(source, sink);
                    if (c.isEmpty()) {
                        return true;
                    }
                    flowSDG = flowSDG.subgraph(c);
                    change = true;
                    break;
                } else {
                    /**
                     * we already checked this edge, no need to check again
                     */
                    checkedEdges.add(e);
                }
            }
        }

        /**
         * all summary edges are checked but the program is not found secure, so
         * we have to check the top level: the annotated method itself
         */
        boolean result = checkTopLevelComplete(sdg, source, source, sink, file);
        if (!result) {
            result = checkTopLevelComplete(sdg, sink, source, sink, file);
            if (!result) {
                result = checkTopLevelComplete(sdg, sink, source, sink, file);
            }
        }
        if (result) {
            return true;
        }
        return false;
    }

    /**
     * Check the top level method for sink or source annotation
     *
     * @param sdg
     * @param source
     * @param sink
     * @param file
     * @return
     */
    private boolean checkTopLevelComplete(SDG sdg, SDGNode entryNode,
            SDGNode source, SDGNode sink, File file) {
        /**
         * all summary edges are checked but the program is not found secure, so
         * we have to check the top level: the annotated method itself
         */
        // does not work properly
        // checks the top level method of the source annotation (not the one
        // from the sink)
        if (descSink(sink, sdg) == null || descOtherParams(source, sdg) == null) {
            /**
             * How to check such a method with KeY?
             */
            System.out
                    .println("!!!!DescSink and DescOtherParams = null. For nodes:"
                            + source + ", " + sink);
            System.out.print("descSink:" + descSink(sink, sdg)
                    + ", descOtherParams" + descOtherParams(source, sdg) + "/");
            System.out.println("/ in method " + sink.getBytecodeMethod()
                    + "and: " + source.getBytecodeMethod());
            return false;
        }
        SDGNode m = sdg.getEntry(entryNode);
        System.out.println("Summary edge from: " + source.getBytecodeName()
                + " to " + sink.getBytecodeName());
        System.out.println("\t\ttop level call in " + m.getBytecodeMethod());
        System.out.println("\t\t /*@ requires " + pointsTo(sdg, m)
                + ";\n\t\t  @ determines " + descSink(sink, sdg) + " \\by "
                + descOtherParams(source, sdg) + "; */");
        String a1 = m.getBytecodeMethod();
        String b = "\t/*@ requires " + pointsTo(sdg, m)
                + ";\n\t  @ determines " + descSink(sink, sdg) + " \\by "
                + descOtherParams(source, sdg) + "; */";
        String[] a2 = a1.split("\\.");
        String[] a3 = a2[a2.length - 1].split("\\(");
        String methodName = a3[0];
        // wirte method to same file below
        paramInClass = auto.exportJava(b, methodName, descSink(sink, sdg),
                descOtherParams(source, sdg));
        // create .key file
        String params = "";
        if (paramInClass != null) {
            for (int i = 0; i < paramInClass.length; i++) {
                if (i == 0) {
                    params += paramInClass[i];
                } else {
                    params += "," + paramInClass[i];
                }
            }
        }
        String methodNameKeY = methodName + "(" + params + ")";
        String newJavaFile = "proofs.sourceFile";
        auto.createKeYFile(newJavaFile, methodNameKeY);
        auto.createKeYFileFunctional(newJavaFile, methodNameKeY);

        long timeStartKeY = System.currentTimeMillis();
        boolean result = false;

        // TODO
        System.out.println("runKeY: Path:" + pathKeY + " javaClass:"
                + javaClass + " methodName: " + methodNameKeY);
        result = auto.runKeY(pathKeY, "information flow");
        boolean resultFunc = auto.runKeY(pathKeY, "functional");
        System.out.println("Information Flow Result: " + result);
        System.out.println("Functional Result: " + resultFunc);
        // if(!methodName.contains("secure_voting")){
        // result = auto.runKeY(javaClass, methodNameKeY, "information flow");
        // }
        long timeEndKeY = System.currentTimeMillis();
        System.out.println("Runtime KeYProof: " + (timeEndKeY - timeStartKeY)
                / 1000 + " Sec.");
        if (!result || !resultFunc) {
            System.out.println("Could not proof method automatically.");
            if (!fullyAutomatic) {
                System.out
                        .println("From node: " + source + " to node: " + sink);
                System.out
                        .println("type \"y\" to verify method manually or \"n\" to go on automatically ");
                Scanner scanInput = new Scanner(System.in);
                String keyAnswer = scanInput.nextLine();
                if (keyAnswer.equals("y")) {
                    // open JAVA and KeY
                    auto.openJava(file);
                    auto.openKeY(javaClass, methodNameKeY);

                    System.out.println("type y if KeY could prove");
                    Scanner scanInput2 = new Scanner(System.in);
                    String keyAnswer2 = scanInput2.nextLine();

                    if (keyAnswer2.equals("y")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
        return result;
    }

    /**
     * get all summary edges contained in a given flow, including metric for
     * method selection strategy. The result is sorted according to the method
     * selection strategy
     *
     * @param flowSDG subgraph induced by our flow
     * @param source source of the flow (used to check for bridges)
     * @param sink sink of the flow (used to check for bridges)
     * @param checkedEdges edges already checked. They are not included in the
     * result.
     * @param neueHeuristic
     * @return summary edges of the given flow sorted according to the selection
     * strategy
     */
    private List<EdgeMetric> getSummaryEdges(SDG flowSDG, SDGNode source,
            SDGNode sink, List<SDGEdge> checkedEdges, SDG sdg,
            boolean neueHeuristic) {
        List<EdgeMetric> summaryEdges = new ArrayList<EdgeMetric>();

        /**
         * clone() is needed because removing/adding edges to check for bridges
         * throws the iterator off
         */
        System.out.print("Starting...");
        Collection<SDGEdge> edges = flowSDG.clone().edgeSet();
        System.out.println("Ended cloning");
        System.out.println("Number of edeges: " + edges.size());
        for (SDGEdge e : edges) {
            if (e.getKind() == SDGEdge.Kind.SUMMARY
                    && !checkedEdges.contains(e)) {
                // System.out.println("Enter Summary Edge...");
                SDGNode callee = sdg.getEntry(e.getSource());
                String a1 = callee.getBytecodeMethod();
                Boolean javaLibary = false;
                if (a1.contains("java.") || a1.contains(".lang.")) {
                    javaLibary = true;
                }
                // if (a1.contains("unZipItByte")) {
                // System.out.println("javaLibary: " + javaLibary);
                // }
                // if (a1.contains("unZipItExtract")) {
                // System.out.println("unZipItExtract: " + "javaLibary: "
                // + javaLibary);
                // }
                // System.out.print("  Bytecodename: " + a1);
                String[] a2 = a1.split("\\.");
                String[] a3 = a2[a2.length - 1].split("\\(");
                String methodName = a3[0];
                // System.out.print("Summary Edge: ");
                // System.out.print("Soruce: " + e.getSource());
                // System.out.print("Target: " + e.getTarget());

                boolean isKeYCompatible = isKeyCompatible(methodName,
                        javaLibary);
                // if (a1.contains("unZipItByte")) {
                // System.out.println("isKeYC: " + isKeYCompatible);
                // }
                // if (a1.contains("unZipItExtract")) {
                // System.out.println("unZipItExtract: " + "isKeYC: "
                // + isKeYCompatible);
                // }
                /**
                 * check whether method matches a pattern
                 */
                boolean machtesPattern = isKeYCompatible;
                if (machtesPattern) {
                    System.out.println("True: " + methodName + " " + javaLibary
                            + " " + e.getSource() + ", " + e.getTarget());
                }
                // System.out.println(" compatible:" + isKeYCompatible);
                // machtesPattern = checkPattern(e, sdg);

                boolean isBridge = false;
                // if (e.getSource().getBytecodeMethod().contains("unzip")
                // || e.getTarget().getBytecodeMethod().contains("unzip")) {
                // isBridge = true;
                // }
                // System.out.print("(se)");
                /**
                 * check whether e is a bridge
                 */
                // flowSDG.removeEdge(e);
                // System.out.print("Starting chopping ");

                // chopper.chop(source, sink).isEmpty();
                // System.out.println("Ended chopping ");
                // flowSDG.addEdge(e);
                /**
                 * check whether a low parameter is regarded *
                 */
                // boolean regardsLow = regardsLowPattern(e, sdg);
                /**
                 * get the number of summary edges contained by the flow
                 * summarized by e
                 */
                // SDGNode v = e.getSource();
                // SDGNode w = e.getTarget();
                // SDG containedSDG = flowSDG.subgraph(chopper.chop(v, w));
                int containedSummary = 0;
                // for (SDGEdge e2 : containedSDG.edgeSet()) {
                // if (e2.getKind() == SDGEdge.Kind.SUMMARY) {
                // containedSummary++;
                // }
                // }
                if (neueHeuristic) {
                    summaryEdges.add(new EdgeMetric(e, machtesPattern,
                            isBridge, containedSummary));
                } else {
                    summaryEdges.add(new EdgeMetric(e, isBridge,
                            containedSummary));
                }
            }
        }
        System.out.print("Sorting: ");
        /**
         * sort according to edge metric
         */
        Collections.sort(summaryEdges);
        System.out.println("finished.");
        System.out.println("Number of summary edges: " + summaryEdges.size());

        return summaryEdges;
    }

    private boolean isKeyCompatible(String methodName, Boolean javaLibary) {
        if (javaLibary) {
            // System.out.println(methodName + ": " + javaLibary);
            return false;
        }
        if (methodName.contains("<init>.")) {
            methodName = methodName.split("\\.")[1];
        }
        ArrayList<String> methodFeatures = ml.getCreatedNames(methodName);
        if (methodFeatures == null) {
            methodFeatures = new ArrayList<String>();
            methodFeatures.add(methodName);
        } else {
            methodFeatures.add(methodName);
        }
        boolean isSubset = keyFeatures.containsAll(methodFeatures);
//		if (methodName.contains("write")) {
//			System.out.println(keyFeatures);
//			System.out.println(methodFeatures);
//		}
        return isSubset;
    }

    /**
     * loads List of KeY features in ArrayList keyFeatures
     */
    public void loadAndAddList() {
        // load List
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "dep/JAVALANG.txt"));
            String line = br.readLine();
            String[] entrys;
            String entry;
            while (line != null) {
                entrys = line.split("\\.");
                entry = entrys[entrys.length - 1].trim();
                keyFeatures.add(entry);
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // add List with self created Classes and methods
        keyFeatures.addAll(auto.getClassNames());
        Set<String> methods = ml.getMethods();
        keyFeatures.addAll(methods);
    }

    private boolean isHighVar(SDG sdg, SDGNode source, SDGNode sink) {
        Collection<SDGNode> c = chopper.chop(source, sink);
        if (c.isEmpty()) {
            return false;
        }
        /**
         * get edges involved in flow
         */
        SDG flowSDG = sdg.subgraph(c);

        return false;
    }

    private boolean checkPattern(SDGEdge e, SDG sdg) {
        SDGNode v = e.getSource();
        SDGNode w = e.getTarget();
        Collection<SDGNodeTuple> callPairs = sdg.getAllFormalPairs(v, w);
        for (SDGNodeTuple t : callPairs) {
            SDGNode p = t.getFirstNode();
            SDGNode r = t.getSecondNode();
            /**
             * skip methods that are already secure
             */
            if (chopper.chop(p, r).isEmpty()) {
                continue;
            }
            SDGNode callee = sdg.getEntry(p);
            String methodName = callee.getBytecodeMethod();
            String[] a2 = methodName.split("\\.");
            int last = a2.length - 1;
            String[] a3 = a2[last].split("\\(");
            methodName = a3[0];
            System.out.println("match pattern: " + methodName);
            ArrayList<String> methodString = auto.getJava(methodName);

            ArrayList<String> highVar = new ArrayList<String>();
            // ArrayList<String> lowVar = new ArrayList<String>();
            ArrayList<String> arrayNames = new ArrayList<String>();
            // ArrayList<String> arrayListNames = new ArrayList<String>();
            /**
             * get the high and low variables, should normally be done by graph
             * analysis
             *
             */
            boolean all = true;
            if (all) {

                // it can be a parameter or a Formal-In Node. Formal In Node:
                // p.getKind().name().equals("FORMAL_IN")
                if (!p.getBytecodeName().startsWith("<param> ")
                        && !p.getKind().name().equals("FORMAL_IN")) {
                    // TODO: probability of global variables is high, has to be
                    // handeled
                    System.out.println("break with node: " + p);
                    break;
                }
                // all parameter as variables
                String param = descAllParams(p, sdg);
                String[] params = param.split(",");
                for (int i = 0; i < params.length; i++) {
                    highVar.add(params[i]);
                }
            } else {
                // all parameter that influence the result as variables
                // TODO: ausgabe �berpr�fen
                SDGNode method = sdg.getEntry(r);
                CGNode methodCG = state.cg.getNode(sdg.getCGNodeId(method));
                IR ir = methodCG.getIR();
                for (SDGNode f : sdg.getFormalInsOfProcedure(method)) {
                    int f_number = Integer.parseInt(f.getBytecodeName()
                            .substring(8));
                    if (f_number != 0
                            && p.getBytecodeName().startsWith("<param> ")
                            || p.getKind().name().equals("FORMAL_IN")) {
                        if (isHighVar(sdg, f, r)) {
                            String pa = ir.getLocalNames(0,
                                    ir.getParameter(f_number))[0];
                            highVar.add(pa);
                        }
                    }
                }
            }

            /**
             * get arrays *
             */
            for (int i = 0; i < methodString.size(); i++) {
                String line = methodString.get(i);
                if (line.contains("[]") && i != 0) {
                    String ar = line.split("\\[\\]")[1].trim();
                    ar = ar.split("=")[0].trim();
                    arrayNames.add(ar);
                } // array given as a parameter
                else if (i == 0 && line.contains("[]")) {
                    String ar = line.split("(")[1];
                    ar = ar.split(")")[0];
                    ar = ar.split("[]")[1].split(")")[0].split(",")[0].trim();
                    arrayNames.add(ar);
                }
            }

            ArrayList<String> varArrName = new ArrayList<String>();
            for (int a = 0; a < methodString.size(); a++) {
                String line = methodString.get(a);
                if (line.contains("=") && arrayNames.size() >= 1) {
                    if (line.contains(arrayNames.get(0))) {
                        varArrName.add(line.split("=")[1]);
                    }
                }
            }

            /**
             * check for array access *
             */
            boolean highToA = false;
            boolean lowToA = false;
            boolean boolLow = false;
            boolean boolHigh = false;
            int tresh = 9000;
            for (int j = 0; j < arrayNames.size(); j++) {
                for (int i = 0; i < methodString.size(); i++) {
                    String line = methodString.get(i);
                    if (line.contains("=")) {
                        String[] line1 = line.split("=");
                        for (int k = 0; k < highVar.size(); k++) {
                            if (line1[1].contains(highVar.get(k))) {
                                boolHigh = true;
                            }
                        }
                        if (line1[0].contains(arrayNames.get(j)) && boolHigh) {
                            highToA = true;
                            tresh = i;
                        }

                        boolLow = true;
                        // for (int k = 0; k < lowVar.size(); k++) {
                        // if (line.contains(lowVar.get(k))) {
                        // boolLow = true;
                        // }
                        // }

                        // TODO: if result has something to do with array
                        // chop(line mit array to chop not empty)
                        // if line.cotains("result") &&
                        // (line.cotains(arrayNames.get(j)) ||
                        // line.contains(VarWithArrayName)) && i < tresh)
                        // VarWithArrayName =
                        if (line.contains(arrayNames.get(j)) && boolLow
                                && i > tresh) {
                            lowToA = true;
                        }
                    }

                    boolLow = false;
                    boolHigh = false;
                }
                if (lowToA && highToA) {
                    return true;
                } else {
                    lowToA = false;
                    highToA = false;
                }
            }
            // that was array-heuristic

            // Multiplication with zero
            for (int i = 0; i < methodString.size(); i++) {
                String line = methodString.get(i);
                if (line.contains("*0") || line.contains("* 0")) { // ||
                    // line.contains(nullFunction.get(i))
                    return true;
                }
            }
            // identity
            // calculate value of every variable that influences result
        }

        return false;
    }

    private String descAllParams(SDGNode n, SDG sdg) {
        StringBuilder sb = new StringBuilder();
        if (!n.getBytecodeName().startsWith("<param> ")
                && !n.getKind().name().equals("FORMAL_IN")) {
            return null;
        }
        SDGNode method = sdg.getEntry(n);
        CGNode methodCG = state.cg.getNode(sdg.getCGNodeId(method));
        /**
         * get IR to get names of the parameters. Need to compile classes with
         * sufficient debug information for this.
         */
        IR ir = methodCG.getIR();

        String delim = "";
        for (SDGNode p : sdg.getFormalInsOfProcedure(method)) {
            /**
             * only describe real parameters
             */
            if (!p.getBytecodeName().startsWith("<param> ")
                    && !p.getKind().name().equals("FORMAL_IN")) {
                continue;
            }
            // TODO: change for global Variables
            if (p.getBytecodeName().startsWith("<param> ")) {
                int p_number = Integer.parseInt(p.getBytecodeName()
                        .substring(8));
                /**
                 * find out parameter name through IR
                 */
                sb.append(delim).append(
                        ir.getLocalNames(0, ir.getParameter(p_number))[0]);
            } else {
                String forInName = p.getBytecodeName();
                // System.out.println(forInName);
                String[] forInNames = forInName.split("\\.");
                forInName = forInNames[forInNames.length - 1];
                sb.append(delim).append(forInNames);
            }
            delim = ", ";
        }
        /**
         * if no other parameter is found, we need to insert "\\nothing" to
         * generate valid JML
         */
        if (sb.toString().equals("")) {
            return "\\nothing";
        }
        return sb.toString();
    }

    /**
     * describe the sink of a flow within a method. Currently only available for
     * result nodes. Returns null otherwise.
     *
     * @param n sink node
     * @param sdg the SDG for our program
     * @return Description of sink node (null if no description possible)
     */
    private String descSink(SDGNode n, SDG sdg) {
        if (n.getKind() == SDGNode.Kind.EXIT) {
            return "\\result";
        } else {
            return "this";
        }
        // return null;
    }

    /**
     * describe the params except the source of a flow within a method.
     * Currently only available for true parameter nodes (explicit parameters
     * and this). Returns null otherwise.
     *
     * @param n source node
     * @param sdg the SDG for our program
     * @return Description of params except source node (null if no description
     * possible)
     */
    private String descOtherParams(SDGNode n, SDG sdg) {
        StringBuilder sb = new StringBuilder();
        try {
            if (!n.getBytecodeName().startsWith("<param> ")
                    && !n.getKind().name().equals("FORMAL_IN")) {
                return null;
            }
            SDGNode method = sdg.getEntry(n);
            CGNode methodCG = state.cg.getNode(sdg.getCGNodeId(method));
            /**
             * get IR to get names of the parameters. Need to compile classes
             * with sufficient debug information for this.
             */
            IR ir = methodCG.getIR();

            String delim = "";
            Set<SDGNode> ps = sdg.getFormalInsOfProcedure(method);
            for (SDGNode p : ps) {

                /**
                 * only describe real parameters
                 */
                if (p == n
                        || (!p.getBytecodeName().startsWith("<param> ") && !p
                        .getKind().name().equals("FORMAL_IN"))) {
                    continue;
                }
                if (p.getBytecodeName().startsWith("<param> ")) {
                    int p_number = Integer.parseInt(p.getBytecodeName()
                            .substring(8));
                    /**
                     * find out parameter name through IR
                     */
                    sb.append(delim).append(
                            ir.getLocalNames(0, ir.getParameter(p_number))[0]);
                } else {
                    String forInName = p.getBytecodeName();
                    // System.out.println(forInName);
                    String[] forInNames = forInName.split("\\.");
                    forInName = forInNames[forInNames.length - 1];
                    sb.append(delim).append(forInNames);
                }
                delim = ", ";
            }
            /**
             * if no other parameter is found, we need to insert "\\nothing" to
             * generate valid JML
             */
        } catch (Exception e) {
            if (sb.toString().equals("")) {
                return "\\nothing";
            }
        }
        return sb.toString();
    }

    /**
     * Calculates non-aliasing information for parameters of a method node m,
     * using JOANA's points-to information.
     *
     * @param sdg our SDG
     * @param m method node to check
     * @return non-aliasing information as a string that can be used as
     * precondition
     */
    private String pointsTo(SDG sdg, SDGNode m) {
        PointerAnalysis<? extends InstanceKey> pts = state.pts;
        CallGraph cg = state.cg;
        /**
         * get the call graph node corresponding to the SDG method node
         */
        CGNode method = cg.getNode(sdg.getCGNodeId(m));
        /**
         * get IR for parameter names
         */
        IR ir = method.getIR();
        Iterable<PointerKey> pKeys = pts.getPointerKeys();
        ArrayList<LocalPointerKey> lKeys = new ArrayList<LocalPointerKey>();
        for (PointerKey pKey : pKeys) {
            /**
             * save all LocalPointerKeys that are params of our method
             */
            if (!(pKey instanceof LocalPointerKey)) {
                continue;
            }
            LocalPointerKey lpk = (LocalPointerKey) pKey;
            if (lpk.getNode() == method && lpk.isParameter()) {
                lKeys.add(lpk);
            }
        }
        /**
         * calculate individual non-alias clauses
         */
        ArrayList<String> pointsToResult = calculateNonAliases(lKeys, pts, ir);
        StringBuilder sb = new StringBuilder();
        String delim = "";
        /**
         * chain clauses together by conjunction
         */
        for (String nonAliased : pointsToResult) {
            sb.append(delim).append(nonAliased);
            delim = " && ";
        }
        /**
         * for simpler code, if we don't have any clauses, we return "true" here
         * instead of writing code that does not emit a aliasing precondition.
         * do it the proper way
         */
        if (sb.toString().equals("")) {
            return "true";
        }
        return sb.toString();
    }

    private ArrayList<String> calculateNonAliases(
            ArrayList<LocalPointerKey> lKeys,
            PointerAnalysis<? extends InstanceKey> pts, IR ir) {
        int n = lKeys.size();
        ArrayList<String> result = new ArrayList<String>();
        /**
         * enumerate all two element subsets of pointer keys and check if those
         * to have disjunct points-to sets
         */
        for (int i = 0; i < n; i++) {
            OrdinalSet<? extends InstanceKey> instances = pts
                    .getPointsToSet(lKeys.get(i));
            for (int j = i + 1; j < n; j++) {
                if (disjunct(instances, pts.getPointsToSet(lKeys.get(j)))) {
                    /**
                     * get the names of the parameters associated with the
                     * pointer keys
                     */
                    String o1 = ir.getLocalNames(0, lKeys.get(i)
                            .getValueNumber())[0];
                    String o2 = ir.getLocalNames(0, lKeys.get(j)
                            .getValueNumber())[0];
                    /**
                     * if points-to sets are disjunct, o1 and o2 cannot alias
                     */
                    result.add(o1 + " != " + o2);
                }
            }
        }
        return result;
    }

    /**
     * calculates whether two Ordinal sets are disjunct.
     */
    private boolean disjunct(OrdinalSet<?> s1, OrdinalSet<?> s2) {
        for (Object e1 : s1) {
            for (Object e2 : s2) {
                if (e1.equals(e2)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * get violation path
     */
    public ViolationPath getVP(IViolation<SecurityNode> v) {
        return getVP(getChop(v));
    }

    private ViolationPath getVP(Chop c) {
        return c.getViolationPathes().getPathesList().get(0);
    }

    private Chop getChop(IViolation<SecurityNode> v) {
        return ((ClassifiedViolation) v).getChops().iterator().next();
    }
}