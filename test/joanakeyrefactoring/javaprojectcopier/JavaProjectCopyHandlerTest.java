/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaprojectcopier;

import joanakeyrefactoring.javaforkeycreator.JavaProjectCopyHandler;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import joanakeyrefactoring.staticCG.JCallGraph;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaClass;
import org.antlr.v4.runtime.misc.OrderedHashSet;
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
public class JavaProjectCopyHandlerTest {

    public JavaProjectCopyHandlerTest() {
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
     * Test of copyClasses method, of class JavaProjectCopyHandler.
     */
    @Test
    public void testCopyClasses() throws IOException {
        String pathToSource = "testdata/multipleClassesFalsePos/MultipleClassesFalsePos/src";
        String pathToNew = "testdata/multipleClassesFalsePosKey";

        JCallGraph callGraph = new JCallGraph();
        String pathtoJar = "testdata/multipleClassesFalsePos/MultipleClassesFalsePos/dist/MultipleClassesFalsePos.jar";
        File f = new File(pathtoJar);
        callGraph.generateCG(f);

        OrderedHashSet<StaticCGJavaClass> alreadyFoundClasses = callGraph.getAlreadyFoundClasses();

        JavaProjectCopyHandler copyHandler = new JavaProjectCopyHandler(
                pathToSource, pathToNew);
        copyHandler.copyClasses(alreadyFoundClasses);     
        
        copyHandler.clearFolder();
    }

}
