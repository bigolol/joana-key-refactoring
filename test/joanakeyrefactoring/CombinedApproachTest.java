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

    @Test
    public void testParseFile() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        JoanaAndKeyCheckData parsedData
                = CombinedApproach.parseInputFile("testdata/jzip.joak");
        Assert.assertEquals("programPart, jzip.JZip.CONFIGURATION, high",
                parsedData.getAnnotationsSource().get(0));
        Assert.assertEquals("programPart, jzip.MyFileOutputStream.content, low",
                parsedData.getAnnotationsSink().get(0));
        String secLevel = CombinedApproach.parseSecLevel(parsedData.getAnnotationsSource().get(0));
        Assert.assertEquals("high", secLevel);
        secLevel = CombinedApproach.parseSecLevel(parsedData.getAnnotationsSink().get(0));
        Assert.assertEquals("low", secLevel);
        IFCAnalysis ifcAnalysis = new IFCAnalysis(SDGProgram.loadSDG("testdata/JZip.pdg"));
        String sinkPartString = CombinedApproach.parseAnnoDesc(parsedData.getAnnotationsSink().get(0));
        SDGProgramPart sinkPart = ifcAnalysis.getProgram().getPart(sinkPartString);
        Assert.assertNotNull(sinkPart);

        String sourcePartString = CombinedApproach.parseAnnoDesc(parsedData.getAnnotationsSource().get(0));
        SDGProgramPart sourcePart = ifcAnalysis.getProgram().getPart(sinkPartString);
        Assert.assertNotNull(sourcePart);

    }

}
