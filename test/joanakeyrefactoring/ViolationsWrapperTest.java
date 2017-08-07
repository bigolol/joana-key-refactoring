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
//                "data/jzip.pdg", "data/jzip_stateSaver.txt", "data/jzip_viols.txt", "JZipWithException", "JZipWithException.jar");
//        ViolationsWrapper violWrapper = disprovingProject.generateViolWrapper();
//        String saveStr = violWrapper.generateSaveString();
//        PrintWriter printWriter = new PrintWriter("data/jzip_violWrapper.txt");
//        printWriter.write(saveStr);
//        printWriter.close();
    }

    @Test
    public void testLoadFromSaveString() throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader("data/jzip_violWrapper.txt"));
        StringBuilder completeString = new StringBuilder();
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.trim().startsWith("//")) {
                continue;
            }
            completeString.append(line + '\n');
        }
        DisprovingProject disprovingProject = new DisprovingProject(
                "data/jzip.pdg", "data/jzip_stateSaver.txt", "data/jzip_viols.txt", "JZipWithException", "JZipWithException.jar");

        ViolationsWrapper.generateFromSaveString(completeString.toString(),
                disprovingProject.getSdg(), disprovingProject.getCallGraph());
    }

}
