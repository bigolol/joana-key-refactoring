/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import org.junit.After;
import org.junit.AfterClass;
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

    /**
     * Test of main method, of class CombinedApproach.
     */
    @Test
    public void testWithParsedGraph() throws Exception {
        String pathKeY = "dep/KeY.jar";
        String javaClass = "";
        String pathToJavaFile = "JZipWithException/jzip";
        JavaMethodSignature entryMethod = JavaMethodSignature.mainMethodOfClass("jzip/JZip");
        StateSaver state = new StateSaver();

        IFCAnalysis analysis = new IFCAnalysis(SDGProgram.loadSDG("testdata/JZip.pdg"));
        CombinedApproach.addJzip2Annotations(analysis);
        
        

    }

}
