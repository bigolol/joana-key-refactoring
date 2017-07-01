/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.CustomListener;

import joanakeyrefactoring.antlr.java8.Java8Parser;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaClass;
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
public class GetMethodBodyListenerTest {
    
    public GetMethodBodyListenerTest() {
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
     * Test of parseFile method, of class GetMethodBodyListener.
     */
    @Test
    public void testParseFile() {
        String java = 
                "package p;"
                + "class ClassA {"
                + "void func(int i, String s, ClassA a) {}"
                + "}";
        StaticCGJavaClass cGJavaClass = new StaticCGJavaClass("p.ClassA");
        StaticCGJavaMethod cGJavaMethod = new StaticCGJavaMethod(cGJavaClass, "func", "int,String,ClassA", false);
        GetMethodBodyListener bodyListener = new GetMethodBodyListener();
        bodyListener.parseFile(java, cGJavaMethod);
        
        assertEquals(3, bodyListener.getExtractedMethodParamNames().size());
        assertEquals("i", bodyListener.getExtractedMethodParamNames().get(0));
        assertEquals("s", bodyListener.getExtractedMethodParamNames().get(1));
        assertEquals("a", bodyListener.getExtractedMethodParamNames().get(2));
    }

   
    
}
