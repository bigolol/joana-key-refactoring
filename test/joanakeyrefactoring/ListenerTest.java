/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author holgerklein
 */
public class ListenerTest {

    @Test
    public void testEnterFieldDecl() {
        String allClasses = "public class C {"
                + "private static int i;"
                + "}";
        ParseJavaForKeyListener listener = new ParseJavaForKeyListener(allClasses);
    }

    @Test
    public void testEnterCtorDecl() {
        String allClasses = "public class C {"
                + "public C(int x, String c){}"
                + "}";
        ParseJavaForKeyListener listener = new ParseJavaForKeyListener(allClasses);
    }

    @Test
    public void testEnterMethodDecl() {
        String allClasses = "public class C {"
                + "public String func(int x, String c){ return c; }"
                + "}";
        ParseJavaForKeyListener listener = new ParseJavaForKeyListener(allClasses);
    }

    @Test
    public void testEnterEmptyMethodDecl() {
        String allClasses = "public class C {"
                + "public String func(){\n return c;\n }"
                + "}";
        ParseJavaForKeyListener listener = new ParseJavaForKeyListener(allClasses);
    }

    @Test
    public void getFieldsCorrectAsStringTest() {
        String allClasses = "public class C {"
                + "private static int i;"
                + "private static String s;"
                + "private static char c;"
                + "}";
        ParseJavaForKeyListener listener = new ParseJavaForKeyListener(allClasses);
        String fieldsCorrectAsString = listener.getFieldsWithNullableAsString();
        Assert.assertEquals(
                "int /*@nullable*/ i; \n"
                + "String /*@nullable*/ s; \n"
                + "char /*@nullable*/ c; \n", fieldsCorrectAsString);
    }

}
