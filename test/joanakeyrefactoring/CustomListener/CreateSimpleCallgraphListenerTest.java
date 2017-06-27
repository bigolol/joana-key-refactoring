/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.CustomListener;

import java.util.List;
import java.util.stream.Stream;
import joanakeyrefactoring.antlr.java8.Java8Parser;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaClass;
import joanakeyrefactoring.customListener.simpleJavaModel.JavaMethod;
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
public class CreateSimpleCallgraphListenerTest {

    public CreateSimpleCallgraphListenerTest() {
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
    public void testExtractGraphWithoutEdges() {
        String java = "package org.student.simple;"
                + "public class SimpleClass {\n"
                + "private int a;\n"
                + "	public void foo(int s) {		\n"
                + "	}\n"
                + "	public void foo(String s) {\n"
                + "	}\n"
                + "}";
        ExtractJavaProjModelListener listener = new ExtractJavaProjModelListener();
        listener.extractDataFromProject(Stream.of(java));
        CreateSimpleCallgraphListener callgraphListener = new CreateSimpleCallgraphListener();
        callgraphListener.generateCallGraph(
                listener.getExtractedClasses(), listener.getExtractedMethods(), Stream.of(java));
        List<JavaMethod> extractedMethods = listener.getExtractedMethods();
        assertEquals(extractedMethods.get(0).getCalledMethods().size(), 0);
        assertEquals(extractedMethods.get(1).getCalledMethods().size(), 0);
    }

    @Test
    public void testExtractGraphWithOneEdgeInClass() {
        String java = "package org.student.simple;"
                + "public class SimpleClass {\n"
                + "private int a;\n"
                + "	public void foo(int s) {		\n"
                + "          foo(\"hello\");"
                + "	}\n"
                + "	public void foo(String s) {\n"
                + "	}\n"
                + "}";
        ExtractJavaProjModelListener listener = new ExtractJavaProjModelListener();
        listener.extractDataFromProject(Stream.of(java));
        CreateSimpleCallgraphListener callgraphListener = new CreateSimpleCallgraphListener();
        callgraphListener.generateCallGraph(
                listener.getExtractedClasses(), listener.getExtractedMethods(), Stream.of(java));
        List<JavaMethod> extractedMethods = listener.getExtractedMethods();
        assertEquals(1, extractedMethods.get(0).getCalledMethods().size());
        assertEquals(0, extractedMethods.get(1).getCalledMethods().size());
        assertEquals(extractedMethods.get(1), extractedMethods.get(0).getCalledMethods().get(0));
    }

    @Test
    public void testExtractGraphWithOneEdgeInGraphFuncCallInFuncCall() {
        String java = "package org.student.simple;"
                + "public class SimpleClass {\n"
                + "private int a;\n"
                + "	public void foo(int s) {		\n"
                + "          foo(getInt(3));"
                + "	}\n"
                + "	public int getInt(int i) {\n"
                + "           return i;"
                + "	}\n"
                + "}";
        ExtractJavaProjModelListener listener = new ExtractJavaProjModelListener();
        listener.extractDataFromProject(Stream.of(java));
        CreateSimpleCallgraphListener callgraphListener = new CreateSimpleCallgraphListener();
        callgraphListener.generateCallGraph(
                listener.getExtractedClasses(), listener.getExtractedMethods(), Stream.of(java));
        List<JavaMethod> extractedMethods = listener.getExtractedMethods();
        assertEquals(1, extractedMethods.get(0).getCalledMethods().size());
        assertEquals(0, extractedMethods.get(1).getCalledMethods().size());
        assertEquals(extractedMethods.get(1), extractedMethods.get(0).getCalledMethods().get(0));
    }

}
