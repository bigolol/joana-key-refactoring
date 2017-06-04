package joanakeyrefactoring;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;

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

    public String pathToJar;
    public RepsRosayChopper chopper;
    public StateSaver stateSaver;
    private AutomationHelper automationHelper;
    private boolean fullyAutomatic;
    private String pathToKeyJar;
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
            SDG sdg) throws IOException {
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

    private boolean canDisproveSummaryEdge(SDGEdge se, SDG sdg) throws IOException {
        SDGNode actualInNode = se.getSource();
        SDGNode actualOutNode = se.getTarget();
        Collection<SDGNodeTuple> formalNodePairs = sdg.getAllFormalPairs(actualInNode, actualOutNode);

        for (SDGNodeTuple formalNodeTuple : formalNodePairs) {
            createKeyFiles(formalNodeTuple, sdg);
            boolean result = false, resultFunc = false;
            try {
                result = automationHelper.runKeY(pathToKeyJar, "information flow");
                resultFunc = automationHelper.runKeY(pathToKeyJar, "functional");
            } catch (IOException ex) {
                Logger.getLogger(ViolationsViaKeyChecker.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!result || !resultFunc) {
                return false;
            }
        }
        return true;
    }

    private void createKeyFiles(SDGNodeTuple formalNodeTuple, SDG sdg) throws IOException {
        String methodNameKeY = createJavaFileForKey(formalNodeTuple, sdg);
        String newJavaFile = "proofs.sourceFile";
        automationHelper.createKeYFileIF(newJavaFile, methodNameKeY);
        automationHelper.createKeYFileFunctional(newJavaFile, methodNameKeY);

    }

    private String createJavaFileForKey(SDGNodeTuple formalNodeTuple, SDG sdg) throws UnsupportedEncodingException, IOException {
        SDGNode formalInNode = formalNodeTuple.getFirstNode();
        SDGNode formalOutNode = formalNodeTuple.getSecondNode();
        SDGNode calledMethodNode = sdg.getEntry(formalInNode);

        String descAllFormalInNodes
                = KeyStringGenerator.generateKeyDecsriptionForParamsExceptSourceNode(
                        formalInNode, sdg, stateSaver.callGraph, javaForKeyListener);
        String descOfFormalOutNode
                = KeyStringGenerator.generateKeyDescriptionForSinkOfFlowWithinMethod(formalOutNode, sdg);
        String pointsTo = KeyStringGenerator.generatePreconditionFromPointsToSet(sdg, calledMethodNode, stateSaver);

        String calledMethodByteCode = calledMethodNode.getBytecodeMethod();
        String methodName = getMethodNameFromBytecode(calledMethodByteCode);

        String[] paramInClass = automationHelper.createJavaFileForKeyToDisproveMEthod(
                pointsTo, methodName, descOfFormalOutNode, descAllFormalInNodes);
        String params = "";
        for (String s : paramInClass) {
            params += s + ", ";
        }
        params = params.substring(0, params.length() - 2);
        return methodName + "(" + params + ")";
    }

    private void checkViaUser(SDGNode formalInNode, SDGNode formalOutNode, String methodNameKey) {
        if (!fullyAutomatic) {
            System.out.println("From node: " + formalInNode + " to node: " + formalOutNode);
            System.out.println("type \"y\" to verify method manually or \"n\" to go on automatically ");
            Scanner scanInput = new Scanner(System.in);
            String keyAnswer = scanInput.nextLine();
            if (keyAnswer.equals("y")) {
                // open JAVA and KeY
                automationHelper.openKeY(pathToJar, methodNameKey);
                System.out.println("type y if KeY could prove");
                Scanner scanInput2 = new Scanner(System.in);
                String keyAnswer2 = scanInput2.nextLine();
            }
        }
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
