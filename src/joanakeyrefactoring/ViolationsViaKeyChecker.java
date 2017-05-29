package joanakeyrefactoring;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;

public class ViolationsViaKeyChecker {

    class ViolationChopData {

        public final SDGNode violationSource;
        public final SDGNode violationSink;
        public final Collection<SDGNode> violationChop;

        public ViolationChopData(SDGNode violationSource, SDGNode violationSink, Collection<SDGNode> violationChop) {
            this.violationSource = violationSource;
            this.violationSink = violationSink;
            this.violationChop = violationChop;
        }
    }

    public String[] paramInClass;
    public String pathToJar;
    public RepsRosayChopper chopper;
    public StateSaver stateSaver;
    private AutomationHelper automationHelper;
    private boolean fullyAutomatic;
    private String pathToKeyJar;
    private ArrayList<String> keyFeatures = new ArrayList<String>();
    private ParseJavaForKeyListener javaForKeyListener;

    public ViolationsViaKeyChecker(
            AutomationHelper automationHelper,
            JoanaAndKeyCheckData checkData) {
        this.automationHelper = automationHelper;
        this.javaForKeyListener = automationHelper.generateParseJavaForKeyListener();
        this.pathToJar = checkData.getPathToJar();
        this.stateSaver = checkData.getStateSaver();
        this.fullyAutomatic = checkData.isFullyAutomatic();
        this.pathToKeyJar = checkData.getPathKeY();
        loadAndAddListOfKeyFeatures();
    }

    public ViolationChopData getViolationChopForSecNodeViolation(IViolation<SecurityNode> violationNode, SDG sdg) {
        ViolationPath violationPath = getViolationPath(violationNode);
        this.chopper = new RepsRosayChopper(sdg);
        LinkedList<SecurityNode> violationPathList = violationPath.getPathList();
        SDGNode violationSource = violationPathList.get(0);
        SDGNode violationSink = violationPathList.get(1);
        Collection<SDGNode> violationChop = chopper.chop(violationSource, violationSink);
        return new ViolationChopData(violationSource, violationSink, violationChop);
    }

    public boolean tryToDisproveViolationsViaKey(
            Collection<? extends IViolation<SecurityNode>> violations,
            SDG sdg) throws FileNotFoundException {
        int numberOfViolations = violations.size();
        int disprovedViolations = 0;
        for (IViolation<SecurityNode> violationNode : violations) {
            boolean disproved = checkViolation(violationNode, sdg);
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
        return remaining == 0;
    }

    /**
     * checks a violation (information about one supposed illegal flow) uses KeY
     * to check whether this is a false positive
     *
     * @return true if there is no illegal flow
     * @throws FileNotFoundException
     */
    public boolean checkViolation(IViolation<SecurityNode> violationNode, SDG sdg) throws FileNotFoundException {
        File sourceFile = new File("proofs/sourceFile.java");
        boolean neueHeuristic = true;
        ViolationChopData violationChopData = getViolationChopForSecNodeViolation(violationNode, sdg);
        if (violationChopData.violationChop.isEmpty()) {
            return true;
        }
        //get edges involved in the flow
        SDG violationChopInducedSubgraph = sdg.subgraph(violationChopData.violationChop);
        List<SDGEdge> checkedEdges = new ArrayList<SDGEdge>();
        boolean change = true;
        while (change) {
            change = false;
            List<EdgeMetric> summaryEdges = getSummaryEdges(violationChopInducedSubgraph, checkedEdges, sdg, neueHeuristic);
            for (EdgeMetric edgeMetric : summaryEdges) {
                //name in the paper: se(a_i, a_o)
                SDGEdge summaryEdgeToBeChecked = edgeMetric.edge;
                SDGNode actualInNode = summaryEdgeToBeChecked.getSource();
                SDGNode actualOutNode = summaryEdgeToBeChecked.getTarget();
                boolean removable = true;
                //check all possible method invocations; needed in case of dynamic dispatch
                //name of this collection in the paper: L_i
                Collection<SDGNodeTuple> allFormalNodePairsForActualNodes = sdg.getAllFormalPairs(actualInNode, actualOutNode);
                for (SDGNodeTuple formalNodeTuple : allFormalNodePairsForActualNodes) {
                    //get source and sink node in the callee that induce the summary edge
                    //name of the nodes in the paper: f_i and f_o
                    SDGNode formalInNode = formalNodeTuple.getFirstNode();
                    SDGNode formalOutNode = formalNodeTuple.getSecondNode();
                    if (chopper.chop(formalInNode, formalOutNode).isEmpty()) {
                        continue;
                    }
                    SDGNode calledMethodNode = sdg.getEntry(formalInNode);
                    //generate spec for KeY
                    String descOfFormalOutNode
                            = KeyStringGenerator.generateKeyDescriptionForSinkOfFlowWithinMethod(formalOutNode, sdg);
                    String descAllFormalInNodes
                            = KeyStringGenerator.generateKeyDecsriptionForParamsExceptSourceNode(
                                    formalInNode, sdg, stateSaver.callGraph);
                    String calledMethodByteCode = calledMethodNode.getBytecodeMethod();
                    Boolean javaLibary = false;
                    if (calledMethodByteCode.contains("java.") || calledMethodByteCode.contains("lang")) {
                        javaLibary = true;
                    }
                    String descriptionStringForKey = "\t/*@ requires " + 
                            KeyStringGenerator.generatePreconditionFromPointsToSet(sdg, calledMethodNode, stateSaver)
                            + ";\n\t  @ determines " + descOfFormalOutNode + " \\by "
                            + descAllFormalInNodes + "; */";
                        String methodName = getMethodNameFromBytecode(calledMethodByteCode);
                    if (!isKeyCompatible(methodName, javaLibary)) {
                        removable = false;
                        break;
                    }
                    if (descOfFormalOutNode == null || descAllFormalInNodes == null) {
                        removable = false;
                        break;
                    }
                    // write method to same file below
                    paramInClass = automationHelper.exportJava(
                            descriptionStringForKey, methodName, descOfFormalOutNode, descAllFormalInNodes);
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
                    automationHelper.createKeYFile(newJavaFile, methodNameKeY);
                    // executeKeY with parameter, read result
                    boolean result = automationHelper.runKeY(pathToKeyJar, "information flow");
                    boolean resultFunc = automationHelper.runKeY(pathToKeyJar, "functional");

                    if (!result || !resultFunc) {
                        if (!fullyAutomatic) {
                            System.out.println("From node: " + formalInNode + " to node: " + formalOutNode);
                            System.out.println("type \"y\" to verify method manually or \"n\" to go on automatically ");
                            Scanner scanInput = new Scanner(System.in);
                            String keyAnswer = scanInput.nextLine();
                            if (keyAnswer.equals("y")) {
                                // open JAVA and KeY
                                automationHelper.openJava(sourceFile);
                                automationHelper.openKeY(pathToJar, methodNameKeY);

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

                }
                if (removable) {
                    sdg.removeEdge(summaryEdgeToBeChecked);
                    violationChopInducedSubgraph.removeEdge(summaryEdgeToBeChecked);
                    /**
                     * recalculating of the chop after deleting the summary
                     * edge; if the new chop is empty, our alarm is found to be
                     * a false alarm
                     */
                    Collection<SDGNode> recalcViolationChop = chopper.chop(violationChopData.violationSource, violationChopData.violationSink);
                    if (recalcViolationChop.isEmpty()) {
                        return true;
                    }
                    violationChopInducedSubgraph = violationChopInducedSubgraph.subgraph(recalcViolationChop);
                    change = true;
                    break;
                } else {
                    checkedEdges.add(summaryEdgeToBeChecked);
                }
            }
        }

        /**
         * all summary edges are checked but the program is not found secure, so
         * we have to check the top level: the annotated method itself
         */
        boolean result = checkTopLevelComplete(sdg, violationChopData, sourceFile);
        if (!result) {
            result = checkTopLevelComplete(sdg, violationChopData, sourceFile);
            if (!result) {
                result = checkTopLevelComplete(sdg, violationChopData, sourceFile);
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
    private boolean checkTopLevelComplete(SDG sdg, ViolationChopData violationPathSourceAndSink, File file) {
        // does not work properly
        // checks the top level method of the source annotation (not the one
        // from the sink)
        SDGNode sink = violationPathSourceAndSink.violationSink;
        SDGNode entryNode = violationPathSourceAndSink.violationSink;
        SDGNode source = violationPathSourceAndSink.violationSource;
        String descriptionOfSink = KeyStringGenerator.generateKeyDescriptionForSinkOfFlowWithinMethod(sink, sdg);
        String descriptionOfParams
                = KeyStringGenerator.generateKeyDecsriptionForParamsExceptSourceNode(source, sdg, stateSaver.callGraph);
        if (descriptionOfSink == null || descriptionOfParams == null) {
            /**
             * How to check such a method with KeY?
             */
            System.out
                    .println("!!!!DescSink and DescOtherParams = null. For nodes:"
                            + source + ", " + sink);
            System.out.print("descSink:" + descriptionOfSink
                    + ", descOtherParams" + descriptionOfParams + "/");
            System.out.println("/ in method " + sink.getBytecodeMethod()
                    + "and: " + source.getBytecodeMethod());
            return false;
        }
        SDGNode m = sdg.getEntry(entryNode);
        System.out.println("Summary edge from: " + source.getBytecodeName()
                + " to " + sink.getBytecodeName());
        System.out.println("\t\ttop level call in " + m.getBytecodeMethod());
        System.out.println("\t\t /*@ requires " + KeyStringGenerator.generatePreconditionFromPointsToSet(sdg, m, stateSaver)
                + ";\n\t\t  @ determines " + descriptionOfSink + " \\by "
                + descriptionOfParams + "; */");
        String a1 = m.getBytecodeMethod();
        String b = "\t/*@ requires " + KeyStringGenerator.generatePreconditionFromPointsToSet(sdg, m, stateSaver)
                + ";\n\t  @ determines " + descriptionOfSink + " \\by "
                + descriptionOfParams + "; */";
        String methodName = getMethodNameFromBytecode(a1);
        // wirte method to same file below
        paramInClass = automationHelper.exportJava(b, methodName, descriptionOfParams, descriptionOfParams);
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
        automationHelper.createKeYFile(newJavaFile, methodNameKeY);
        automationHelper.createKeYFileFunctional(newJavaFile, methodNameKeY);

        long timeStartKeY = System.currentTimeMillis();
        boolean result = false;

        // TODO
        System.out.println("runKeY: Path:" + pathToKeyJar + " javaClass:"
                + pathToJar + " methodName: " + methodNameKeY);
        result = automationHelper.runKeY(pathToKeyJar, "information flow");
        boolean resultFunc = automationHelper.runKeY(pathToKeyJar, "functional");
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
                    automationHelper.openJava(file);
                    automationHelper.openKeY(pathToJar, methodNameKeY);

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
    public List<EdgeMetric> getSummaryEdges(
            SDG flowSDG, List<SDGEdge> checkedEdges, SDG sdg,
            boolean neueHeuristic) {
        List<EdgeMetric> summaryEdges = new ArrayList<EdgeMetric>();
        //clone() is needed because removing/adding edges to check for bridges throws the iterator off
        Collection<SDGEdge> clonedEdgesFromflowSDG = flowSDG.clone().edgeSet();
        for (SDGEdge currentEdge : clonedEdgesFromflowSDG) {
            if (isSummaryEdge(currentEdge) && !checkedEdges.contains(currentEdge)) {
                SDGNode callee = sdg.getEntry(currentEdge.getSource());
                String calleeByteCodeMethod = callee.getBytecodeMethod();
                Boolean isPartOfJavaLibrary = false;
                if (calleeByteCodeMethod.contains("java.") || calleeByteCodeMethod.contains(".lang.")) {
                    isPartOfJavaLibrary = true;
                }
                String methodName = getMethodNameFromBytecode(calleeByteCodeMethod);
                boolean isKeYCompatible = isKeyCompatible(methodName,
                        isPartOfJavaLibrary);

                boolean machtesPattern = isKeYCompatible;
                boolean isBridge = false;
                int containedSummary = 0;
                if (neueHeuristic) {
                    summaryEdges.add(new EdgeMetric(currentEdge, machtesPattern,
                            isBridge, containedSummary));
                } else {
                    summaryEdges.add(new EdgeMetric(currentEdge, isBridge,
                            containedSummary));
                }
            }
        }
        Collections.sort(summaryEdges);
        return summaryEdges;
    }

    private static boolean isSummaryEdge(SDGEdge currentEdge) {
        return currentEdge.getKind() == SDGEdge.Kind.SUMMARY;
    }

    private String getMethodNameFromBytecode(String byteCodeMethod) {
        String[] a2 = byteCodeMethod.split("\\.");
        String[] a3 = a2[a2.length - 1].split("\\(");
        String methodName = a3[0];
        if (byteCodeMethod.contains("<init>")) {
            methodName += "." + a2[a2.length - 2].split("\\(")[0];
        }
        return methodName;
    }

    private boolean isKeyCompatible(String methodName, Boolean javaLibary) {
        if (javaLibary) {
            return false;
        }
        if (methodName.contains("<init>.")) {
            methodName = methodName.split("\\.")[1];
        }
        List<String> methodFeatures = javaForKeyListener.getCreatedNames(methodName);
        if (methodFeatures == null) {
            methodFeatures = new ArrayList<String>();
            methodFeatures.add(methodName);
        } else {
            methodFeatures.add(methodName);
        }
        boolean isSubset = keyFeatures.containsAll(methodFeatures);
        return isSubset;
    }

    /**
     * loads List of KeY features in ArrayList keyFeatures
     */
    public void loadAndAddListOfKeyFeatures() {
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
        keyFeatures.addAll(automationHelper.getClassNames());
        Set<String> methods = javaForKeyListener.getMethods();
        keyFeatures.addAll(methods);
    }

    private boolean isHighVar(SDG sdg, SDGNode source, SDGNode sink) {
        Collection<SDGNode> c = chopper.chop(source, sink);
        if (c.isEmpty()) {
            return false;
        }
        SDG flowSDG = sdg.subgraph(c);
        return false;
    }

    /**
     * get violation path
     */
    public ViolationPath getViolationPath(IViolation<SecurityNode> v) {
        return ((ClassifiedViolation) v).getChops().iterator().next().getViolationPathes().getPathesList().get(0);
    }
}
