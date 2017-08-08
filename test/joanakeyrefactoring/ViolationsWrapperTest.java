/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import joanakeyrefactoring.persistence.DisprovingProject;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;
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
public class ViolationsWrapperTest {

    public ViolationsWrapperTest() {
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
     * Test of generateSaveString method, of class ViolationsWrapper.
     */
    @Test
    public void testGenerateSaveString() throws IOException, ClassHierarchyException, GraphIntegrity.UnsoundGraphException, CancelException {
//        DisprovingProject disprovingProject = new DisprovingProject(
//                "data/jzip.pdg", "data/jzip_stateSaver.txt",
//                "data/jzip_viols.txt", "JZipWithException",
//                "JZipWithException.jar", "data/jzip_violWrapper.txt");
//        ViolationsWrapper generateNewViolWrapper = disprovingProject.generateNewViolWrapper();
//        String saveString = generateNewViolWrapper.generateSaveString();
//        PrintWriter printWriter = new PrintWriter("data/jzip_violWrapper_allchops.txt");
//        printWriter.write(saveString);
//        printWriter.close();
    }

    @Test
    public void testLoadFromSaveString() throws FileNotFoundException, IOException {
        DisprovingProject disprovingProject = new DisprovingProject(
                "data/jzip.pdg", "data/jzip_stateSaver.txt",
                "data/jzip_viols.txt", "JZipWithException",
                "JZipWithException.jar", "data/jzip_violWrapper.txt");
        ViolationsWrapper violationsWrapper = disprovingProject.getViolationsWrapper();
       
    }

}
