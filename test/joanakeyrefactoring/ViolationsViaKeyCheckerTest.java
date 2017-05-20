/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author holger
 */
public class ViolationsViaKeyCheckerTest {

    public ViolationsViaKeyCheckerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testGeneratePrecondition() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        String testCase = "plusminusfalsepos";
        JoanaAndKeyCheckData parsedData
                = CombinedApproach.parseInputFile("testdata/" + testCase + ".joak");
        parsedData.addAnnotations();
        IViolation<SecurityNode> violationNode = parsedData.getAnalysis().doIFC().iterator().next();
        AutomationHelper automationHelper = new AutomationHelper(parsedData.getPathToJavaFile());
        ViolationsViaKeyChecker violationsViaKeyChecker = new ViolationsViaKeyChecker(automationHelper, parsedData);
        SDG sdg = parsedData.getAnalysis().getProgram().getSDG();
        ViolationsViaKeyChecker.ViolationChopData violationChopData
                = violationsViaKeyChecker.getViolationChopForSecNodeViolation(violationNode, sdg);
        SDG violationChopInducedSubgraph = sdg.subgraph(violationChopData.violationChop);
        List<EdgeMetric> summaryEdges = violationsViaKeyChecker.getSummaryEdges(sdg, new ArrayList<SDGEdge>(), sdg, false);
        SDGEdge summaryEdgeToBeChecked = summaryEdges.get(0).edge;
        SDGNode actualInNode = summaryEdgeToBeChecked.getSource();
        SDGNode actualOutNode = summaryEdgeToBeChecked.getTarget();
        Collection<SDGNodeTuple> allFormalNodePairsForActualNodes = sdg.getAllFormalPairs(actualInNode, actualOutNode);
        SDGNode formalInNode = allFormalNodePairsForActualNodes.iterator().next().getFirstNode();
        SDGNode formalOutNode = allFormalNodePairsForActualNodes.iterator().next().getSecondNode();
        SDGNode calledMethodNode = sdg.getEntry(formalInNode);

        String precondition = violationsViaKeyChecker.generatePreconditionFromPointsToSet(sdg, calledMethodNode);
        System.out.println(precondition);
    }

}
