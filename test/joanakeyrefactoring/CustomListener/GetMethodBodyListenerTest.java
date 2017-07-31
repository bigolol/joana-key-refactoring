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
        String java
                = "package p;"
                + "class ClassA {"
                + "void func(int i, String s, ClassA a) {}"
                + "}";
        StaticCGJavaClass cGJavaClass = new StaticCGJavaClass("p.ClassA");
        StaticCGJavaMethod cGJavaMethod = new StaticCGJavaMethod(cGJavaClass, "func", "int,String,ClassA", false, "void");
        GetMethodBodyListener bodyListener = new GetMethodBodyListener();
        bodyListener.parseFile(java, cGJavaMethod);

        assertEquals(3, bodyListener.getExtractedMethodParamNames().size());
        assertEquals("i", bodyListener.getExtractedMethodParamNames().get(0));
        assertEquals("s", bodyListener.getExtractedMethodParamNames().get(1));
        assertEquals("a", bodyListener.getExtractedMethodParamNames().get(2));
    }

    @Test
    public void testParseProblemFunc() {
        String java
                = "package p;"
                + "class ClassA {"
                + "private void unZipItExtract(byte[] outputFolder, MyZipInputStream myZis,\n"
                + "			MyFileOutputStream fos) {\n"
                + "		byte[] buffer = new byte[1024];\n"
                + "		byte[] content = new byte[512];\n"
                + "		content = myZis.read();\n"
                + "		for (int i = 0; i < buffer.length / 2; i++) {\n"
                + "				buffer[i] = content[i];\n"
                + "				buffer[i + buffer.length / 2] = outputFolder[i];\n"
                + "		}\n"
                + "		fos.write(buffer);\n"
                + "	}"
                + "}";
        StaticCGJavaClass cGJavaClass = new StaticCGJavaClass("p.ClassA");
        StaticCGJavaMethod cGJavaMethod
                = new StaticCGJavaMethod(
                        cGJavaClass, "unZipItExtract", "byte[],MyZipInputStream,MyFileOutputStream", false, "void");
        GetMethodBodyListener bodyListener = new GetMethodBodyListener();
        bodyListener.parseFile(java, cGJavaMethod);

        assertEquals(3, bodyListener.getExtractedMethodParamNames().size());
        assertEquals("outputFolder", bodyListener.getExtractedMethodParamNames().get(0));
        assertEquals("myZis", bodyListener.getExtractedMethodParamNames().get(1));
        assertEquals("fos", bodyListener.getExtractedMethodParamNames().get(2));
    }

    @Test
    public void testCtor() {
        String java = "package multipleclassesfalsepos;\n"
                + "\n"
                + "/**\n"
                + " *\n"
                + " * @author holger\n"
                + " */\n"
                + "public class ClassB {\n"
                + "    public int[] arr;   \n"
                + "    ClassB(int x) {\n"
                + "        this.arr = new int[3];\n"
                + "        this.arr[2] = x;\n"
                + "    }\n"
                + "    \n"
                + "    ClassB() {}\n"
                + "    \n"
                + "    int[] putDataInArr(int high) {\n"
                + "        arr[4] = high;\n"
                + "        return arr;\n"
                + "    }\n"
                + "    \n"
                + "}";
        StaticCGJavaClass c = new StaticCGJavaClass("multipleclassesfalsepos.ClassB");
        StaticCGJavaMethod m = new StaticCGJavaMethod(c, "<init>", "int", false, "void");
        GetMethodBodyListener bodyListener = new GetMethodBodyListener();
        bodyListener.parseFile(java, m);
        String methodDeclWithNullable = bodyListener.getMethodDeclWithNullable();
    }

}
