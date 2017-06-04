package joanakeyrefactoring;

import java.io.BufferedReader;
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

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ViolationsViaKeyChecker {

    public String[] paramInClass;
    public String pathToJar;
    public RepsRosayChopper chopper;
    public StateSaver stateSaver;
    private AutomationHelper automationHelper;
    private boolean fullyAutomatic;
    private String pathToKeyJar;
    private ArrayList<String> keyFeatures = new ArrayList<String>();
    private ParseJavaForKeyListener javaForKeyListener;
    private ViolationsWrapper violationsWrapper;

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

    public ViolationChop getViolationChopForSecNodeViolation(IViolation<SecurityNode> violationNode, SDG sdg) {
        ViolationPath violationPath = getViolationPath(violationNode);
        this.chopper = new RepsRosayChopper(sdg);
        LinkedList<SecurityNode> violationPathList = violationPath.getPathList();
        SDGNode violationSource = violationPathList.get(0);
        SDGNode violationSink = violationPathList.get(1);
        return new ViolationChop(violationSource, violationSink, sdg);
    }

    public void disproveViaKey(Collection<? extends IViolation<SecurityNode>> violations,
            SDG sdg) {
        violationsWrapper = new ViolationsWrapper(violations, sdg, javaForKeyListener, automationHelper);
        while (!violationsWrapper.allCheckedOrDisproved()) {
            SDGEdge nextSummaryEdge = violationsWrapper.nextSummaryEdge();
            if (canDisproveSummaryEdge(nextSummaryEdge, sdg)) {
                violationsWrapper.removeEdge(nextSummaryEdge);
            } else {
                violationsWrapper.checkedEdge(nextSummaryEdge);
            }
        }
    }

    private boolean canDisproveSummaryEdge(SDGEdge se, SDG sdg) {
        SDGNode actualInNode = se.getSource();
        SDGNode actualOutNode = se.getTarget();
        Collection<SDGNodeTuple> formalNodePairs = sdg.getAllFormalPairs(actualInNode, actualOutNode);

        for (SDGNodeTuple formalNodeTuple : formalNodePairs) {
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
            String pointsTo = KeyStringGenerator.generatePreconditionFromPointsToSet(sdg, calledMethodNode, stateSaver);

            String calledMethodByteCode = calledMethodNode.getBytecodeMethod();

            String descriptionStringForKey
                    = "\t/*@ requires "
                    + pointsTo
                    + ";\n\t  @ determines " + descOfFormalOutNode + " \\by "
                    + descAllFormalInNodes + "; */";
            String methodName = getMethodNameFromBytecode(calledMethodByteCode);
            if (!isKeyCompatible(calledMethodByteCode)) {
                return false;
            }
            if (descOfFormalOutNode == null || descAllFormalInNodes == null) {
                return false;
            }
            try {
                // write method to same file below
                paramInClass = automationHelper.createJavaFileForKeyToDisproveMEthod(
                        descriptionStringForKey, methodName, descOfFormalOutNode, descAllFormalInNodes);
            } catch (Exception ex) {
                return false;
            }
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
            try {
                automationHelper.createKeYFileIF(newJavaFile, methodNameKeY);
                automationHelper.createKeYFileFunctional(newJavaFile, methodNameKeY);
            } catch (IOException ex) {
                return false;
            }
            // executeKeY with parameter, read result
            boolean result = false, resultFunc = false;
            try {
                result = automationHelper.runKeY(pathToKeyJar, "information flow");
                resultFunc = automationHelper.runKeY(pathToKeyJar, "functional");
            } catch (IOException ex) {
                Logger.getLogger(ViolationsViaKeyChecker.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (!result || !resultFunc) {
                if (!fullyAutomatic) {
                    System.out.println("From node: " + formalInNode + " to node: " + formalOutNode);
                    System.out.println("type \"y\" to verify method manually or \"n\" to go on automatically ");
                    Scanner scanInput = new Scanner(System.in);
                    String keyAnswer = scanInput.nextLine();
                    if (keyAnswer.equals("y")) {
                        // open JAVA and KeY
                        automationHelper.openKeY(pathToJar, methodNameKeY);

                        System.out.println("type y if KeY could prove");
                        Scanner scanInput2 = new Scanner(System.in);
                        String keyAnswer2 = scanInput2.nextLine();

                        if (!keyAnswer2.equals("y")) {
                            return false;
                        } else {
                            result = true;
                        }
                    }
                }
            }
            if (!result || !resultFunc) {
                return false;
            }

        }
        return false;
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
     * Checks whether a violaton found by Joana exists on a semantic level in
     * the program by checking all summary edges in the violation chop whether
     * they can be dirsproved using Key. For each summary edge, all formal in
     * nodes for the actual in and out node
     *
     * @param violationNode
     * @param sdg
     * @return
     */
    public boolean myDreamCheckViolation(IViolation<SecurityNode> violationNode, SDG sdg) {

        //violationChop = getViolationChop(violationNode, sdg);
        //SDG violationChopSubgraph = sdg.subgraph(violationChopData.violationChop);
        //Collection<Edge> edgesSorted = sortEdgesByMetric(violationChopSubgraph);
        //for each summaryedge:
        //a_i, a_o = getActualIn, getActualOutNode
        //
        //for each
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
    private boolean checkTopLevelComplete(SDG sdg, ViolationChop violationPathSourceAndSink) throws UnsupportedEncodingException, IOException {
        // does not work properly
        // checks the top level method of the source annotation (not the one
        // from the sink)
        SDGNode sink = violationPathSourceAndSink.getViolationSink();
        SDGNode entryNode = violationPathSourceAndSink.getViolationSink();
        SDGNode source = violationPathSourceAndSink.getViolationSource();
        String descriptionOfSink = KeyStringGenerator.generateKeyDescriptionForSinkOfFlowWithinMethod(sink, sdg);
        String descriptionOfParams
                = KeyStringGenerator.generateKeyDecsriptionForParamsExceptSourceNode(source, sdg, stateSaver.callGraph);
        if (descriptionOfSink == null || descriptionOfParams == null) {

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
        try {
            // wirte method to same file below
            paramInClass = automationHelper.createJavaFileForKeyToDisproveMEthod(b, methodName, descriptionOfParams, descriptionOfParams);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ViolationsViaKeyChecker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ViolationsViaKeyChecker.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        try {
            automationHelper.createKeYFileIF(newJavaFile, methodNameKeY);
        } catch (IOException ex) {
            Logger.getLogger(ViolationsViaKeyChecker.class.getName()).log(Level.SEVERE, null, ex);
        }
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
