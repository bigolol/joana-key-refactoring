/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.staticCG;

import java.io.File;
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
public class JCallGraphTest {
    
    public JCallGraphTest() {
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
     * Test of generateCG method, of class JCallGraph.
     */
    @Test
    public void testGenerateCG() throws Exception {
        JCallGraph jCallGraph = new JCallGraph();
        jCallGraph.generateCG(new File("testdata/multipleClassesFalsePos/MultipleClassesFalsePos/dist/MultipleClassesFalsePos.jar"));
    }
    
}
