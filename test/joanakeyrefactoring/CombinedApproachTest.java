/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import java.io.IOException;
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

    /**
     * Test of main method, of class CombinedApproach.
     */
    @Test
    public void testJzipFromFile() throws Exception {
        
    }
    
    @Test
    public void testParseFile() throws IOException {
        JoanaAndKeyCheckData parsedData = CombinedApproach.parseInputFile("testdata/jzip.joak");
        Assert.assertEquals("programPart, jzip.JZip.CONFIGURATION, high", parsedData.getAnnotationsSource().get(0));
        Assert.assertEquals("programPart, jzip.MyFileOutputStream.content, low", parsedData.getAnnotationsSink().get(0));

        CombinedApproach.parseSecLevel(parsedData.getAnnotationsSource().get(0));
    }

}
