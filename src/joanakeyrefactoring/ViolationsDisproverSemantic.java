package joanakeyrefactoring;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ViolationsDisproverSemantic {

    public String pathToJar;
    public RepsRosayChopper chopper;
    public StateSaver stateSaver;
    private AutomationHelper automationHelper;
    private boolean fullyAutomatic;
    private String pathToKeyJar;
    private ParseJavaForKeyListener javaForKeyListener;
    private ViolationsWrapper violationsWrapper;

    public ViolationsDisproverSemantic(
            AutomationHelper automationHelper,
            JoanaAndKeyCheckData checkData) {
        this.automationHelper = automationHelper;
        this.javaForKeyListener = automationHelper.generateParseJavaForKeyListener();
        this.pathToJar = checkData.getPathToJar();
        this.stateSaver = checkData.getStateSaver();
        this.fullyAutomatic = checkData.isFullyAutomatic();
        this.pathToKeyJar = checkData.getPathKeY();
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
            KeyFileCreator.createKeyFiles(formalNodeTuple, sdg, automationHelper, stateSaver, javaForKeyListener);
            boolean result = false, resultFunc = false;
            try {
                result = automationHelper.runKeY(pathToKeyJar, "information flow");
                resultFunc = automationHelper.runKeY(pathToKeyJar, "functional");
            } catch (IOException ex) {
                Logger.getLogger(ViolationsDisproverSemantic.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (!result || !resultFunc) {
                return false;
            }
        }
        return true;
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

}
