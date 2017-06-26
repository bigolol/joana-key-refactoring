/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.CustomListener;

import java.util.List;
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
public class ExtractJavaProjModelListenerTest {

    public ExtractJavaProjModelListenerTest() {
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
    public void testExtractOneSimpleClass() {
        String java
                = "package org.student.simple;"
                + "public class SimpleClass {\n"
                + "private int a;\n"
                + "	public void foo() {		\n"
                + "	}\n"
                + "	public void bar() {\n"
                + "	}\n"
                + "}";
        ExtractJavaProjModelListener listener = new ExtractJavaProjModelListener();
        listener.extractDataFromProject(java);
        List<JavaClass> classes = listener.getExtractedClasses();
        List<JavaMethod> methods = listener.getExtractedMethods();
        assertEquals(classes.get(0).toString(), "JavaClass[name=SimpleClass, package=org.student.simple]");
        assertEquals(methods.get(0).toString(), "javaMethod[name=foo, args=]");
        assertEquals(methods.get(1).toString(), "javaMethod[name=bar, args=]");
        assertEquals(methods.get(0).getContainingClass(), classes.get(0));
    }

    @Test
    public void testExtractClassMethodWithParams() {
        String java
                = "package org.student.simple;"
                + "public class SimpleClass {\n"
                + "private int a;\n"
                + "	public void foo(int i, String s, SimpleClass other) {		\n"
                + "	}\n"
                + "}";
        ExtractJavaProjModelListener listener = new ExtractJavaProjModelListener();
        listener.extractDataFromProject(java);
        List<JavaMethod> methods = listener.getExtractedMethods();
        assertEquals(methods.get(0).toString(), "javaMethod[name=foo, args=javaMethodParam[type=int, name=i], javaMethodParam[type=String, name=s]]");
    }

    @Test
    public void testExtractClassOverloadedMethods() {
        String java
                = "package org.student.simple;"
                + "public class SimpleClass {\n"
                + "private int a;\n"
                + "	public void foo(int s) {		\n"
                + "	}\n"
                + "	public void foo(String s) {\n"
                + "	}\n"
                + "}";
        ExtractJavaProjModelListener listener = new ExtractJavaProjModelListener();
        listener.extractDataFromProject(java);
        List<JavaMethod> methods = listener.getExtractedMethods();
        assertEquals(methods.size(), 2);
        assertEquals(methods.get(0).toString(), "javaMethod[name=foo, args=javaMethodParam[type=int, name=s]]");
        assertEquals(methods.get(1), "javaMethod[name=foo, args=javaMethodParam[type=String, name=s]]");
    }

    @Test
    public void testExtractTwoClassesSameName() {
        String java
                = "package org.student.simple;"
                + "public class SimpleClass {\n"
                + "private int a;\n"
                + "	public void foo(int s) {		\n"
                + "	}\n"
                + "	public void foo(String s) {\n"
                + "	}\n"
                + "}"
                + "package org.student.simple.other;"
                + "public class SimpleClass {\n"
                + "private int a;\n"
                + "	public void foo(int s) {		\n"
                + "	}\n"
                + "	public void foo(String s) {\n"
                + "	}\n"
                + "}";
        ExtractJavaProjModelListener listener = new ExtractJavaProjModelListener();
        listener.extractDataFromProject(java);
    }

}
