/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import java.io.IOException;
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
        AutomationHelper automationHelper = new AutomationHelper(parsedData.getPathToJavaFile());
    }

}
