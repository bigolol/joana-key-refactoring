/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.persistence;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import joanakeyrefactoring.CombinedApproach;
import joanakeyrefactoring.JoanaAndKeyCheckData;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hklein
 */
public class ViolationsSaverLoaderTest {

    public ViolationsSaverLoaderTest() {
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
     * Test of generateSaveString method, of class ViolationsSaverLoader.
     */
    @Test
    public void testGenerateSaveString() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        JoanaAndKeyCheckData parsedCheckData = CombinedApproach.parseInputFile("testdata/multipleClassesArrFalsePos.joak");
        parsedCheckData.addAnnotations();
        Collection<? extends IViolation<SecurityNode>> ifc = parsedCheckData.getAnalysis().doIFC();
        String generateSaveString = ViolationsSaverLoader.generateSaveString(ifc);
        System.out.println(generateSaveString);
    }

    @Test
    public void testLoadFromSaveString() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        JoanaAndKeyCheckData parsedCheckData = CombinedApproach.parseInputFile("testdata/multipleClassesArrFalsePos.joak");
        parsedCheckData.addAnnotations();
        Collection<? extends IViolation<SecurityNode>> ifc = parsedCheckData.getAnalysis().doIFC();
        String saveString = ViolationsSaverLoader.generateSaveString(ifc);
        PrintWriter printWriter = new PrintWriter("testdata/savestr_violations.txt");
        printWriter.write(saveString);
        printWriter.close();
        Collection<ClassifiedViolation> generated = ViolationsSaverLoader.generateFromSaveString("testdata/savestr_violations.txt", parsedCheckData.getAnalysis().getProgram().getSDG());
        System.out.println(saveString);
        System.out.println(ViolationsSaverLoader.generateSaveString(generated));
        Assert.assertEquals(saveString, ViolationsSaverLoader.generateSaveString(generated));
    }

}
