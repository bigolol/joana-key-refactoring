/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaforkeycreator;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import joanakeyrefactoring.AutomationHelper;
import joanakeyrefactoring.CombinedApproach;
import joanakeyrefactoring.JoanaAndKeyCheckData;
import joanakeyrefactoring.ViolationsWrapper;
import joanakeyrefactoring.staticCG.JCallGraph;
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
public class JavaForKeyCreatorTest {

    public JavaForKeyCreatorTest() {
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
    public void testGenerateForSummaryEdge() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        JoanaAndKeyCheckData parsedCheckData = CombinedApproach.parseInputFile("testdata/multipleClassesArrFalsePos.joak");
        parsedCheckData.addAnnotations();
        JCallGraph callGraph = new JCallGraph();
        SDG sdg = parsedCheckData.getAnalysis().getProgram().getSDG();
        callGraph.generateCG(new File(parsedCheckData.getPathToJar()));
        AutomationHelper automationHelper = new AutomationHelper(parsedCheckData.getPathToJavaFile());
        JavaForKeyCreator javaForKeyCreator = new JavaForKeyCreator(
                parsedCheckData.getPathToJavaFile(),
                callGraph,
                sdg,
                parsedCheckData.getStateSaver(),
                parsedCheckData.getAnalysis());

        Collection<? extends IViolation<SecurityNode>> violations = parsedCheckData.getAnalysis().doIFC();
        ViolationsWrapper violationsWrapper = new ViolationsWrapper(
                violations, sdg,             
                automationHelper,
                parsedCheckData.getPathToJar(),
                parsedCheckData.getAnalysis(),
                callGraph);
        SDGEdge se = violationsWrapper.nextSummaryEdge();
        SDGNode actualInNode = se.getSource();
        SDGNode actualOutNode = se.getTarget();

        Collection<SDGNodeTuple> formalNodePairs = sdg.getAllFormalPairs(actualInNode, actualOutNode);

        javaForKeyCreator.generateJavaForFormalNodeTuple(
                formalNodePairs.iterator().next(),
                violationsWrapper.getMethodCorresToSummaryEdge(se));
    }

}
