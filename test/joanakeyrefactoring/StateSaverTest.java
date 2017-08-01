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
import java.io.PrintWriter;
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
public class StateSaverTest {

    public StateSaverTest() {
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
    public void testGenerateSaveStringMulClassesFalsePos() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        JoanaAndKeyCheckData parsedCheckData = CombinedApproach.parseInputFile("testdata/multipleClassesArrFalsePos.joak");
        String saveString = parsedCheckData.getStateSaver().getSaveString();
    }

    @Test
    public void testGenerateFromSaveFile() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        JoanaAndKeyCheckData parsedCheckData = CombinedApproach.parseInputFile("testdata/multipleClassesArrFalsePos.joak");
        String saveString = parsedCheckData.getStateSaver().getSaveString();
        PrintWriter printWriter = new PrintWriter("testdata/savestr_statesaver.txt");
        printWriter.write(saveString);
        printWriter.close();
        StateSaver generateFromSaveStr = StateSaver.generateFromSaveStr("testdata/savestr_statesaver.txt");        
        Assert.assertTrue(generateFromSaveStr.equals(parsedCheckData.getStateSaver()));         
    }

}
