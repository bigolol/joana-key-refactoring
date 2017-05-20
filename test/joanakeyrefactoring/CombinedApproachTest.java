/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import java.io.IOException;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Holger-Desktop
 */
public class CombinedApproachTest {

    public CombinedApproachTest() {
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
    public void testParseFile() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException, InterruptedException {
    }

    public static void main(String[] args) throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException, InterruptedException, CouldntAddAnnoException {
        String testCase = "plusminusfalsepos";

        JoanaAndKeyCheckData parsedData
                = CombinedApproach.parseInputFile("testdata/" + testCase + ".joak");
        CombinedApproach.runTestFromCheckData(parsedData);
    }

}
