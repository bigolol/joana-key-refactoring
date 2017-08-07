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
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import joanakeyrefactoring.CombinedApproach;
import joanakeyrefactoring.JoanaAndKeyCheckData;
import joanakeyrefactoring.ViolationsWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hklein
 */
public class DisprovingProjectTest {

    public DisprovingProjectTest() {
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

    public static void main(String[] args) throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        JoanaAndKeyCheckData parsedCheckData = CombinedApproach.parseInputFile("testdata/jzip.joak");
        parsedCheckData.addAnnotations();
        SDGSerializer.toPDGFormat(parsedCheckData.getAnalysis().getProgram().getSDG(), new PrintWriter("data/jzip.pdg"));
        Collection<? extends IViolation<SecurityNode>> ifc = parsedCheckData.getAnalysis().doIFC();
        String violationSaveString = ViolationsSaverLoader.generateSaveString(ifc);
        PrintWriter printWriter = new PrintWriter("data/jzip_viols.txt");
        printWriter.write(violationSaveString);
        printWriter.close();
        String stateSaverSaveString = parsedCheckData.getStateSaver().getSaveString();
        printWriter = new PrintWriter("data/jzip_stateSaver.txt");
        printWriter.write(stateSaverSaveString);
        printWriter.close();
    }
    
    @Test
    public void testLoadJzip() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
        DisprovingProject disprovingProject = new DisprovingProject(
                "data/jzip.pdg", "data/jzip_stateSaver.txt", "data/jzip_viols.txt", "JZipWithException", "JZipWithException.jar", "data/jzip_violWrapper");
        ViolationsWrapper violWrapper = disprovingProject.generateViolWrapper();
    }

}
