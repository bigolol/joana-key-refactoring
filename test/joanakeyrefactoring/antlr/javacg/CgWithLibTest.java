/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.antlr.javacg;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import joanakeyrefactoring.staticCG.ClassVisitor;
import joanakeyrefactoring.staticCG.JCallGraph;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaClass;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;
import org.antlr.v4.runtime.misc.OrderedHashSet;
import org.apache.bcel.classfile.ClassParser;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author holger
 */
public class CgWithLibTest {

    @Test
    public void testCreateCG() throws IOException {
        JCallGraph callGraph = new JCallGraph();
        String pathtoJar = "testdata/plusMinusFalsePositive/build/PlusMinusFalsePos.jar";
        File f = new File(pathtoJar);
        callGraph.generateCG(f);      
        OrderedHashSet<StaticCGJavaClass> alreadyFoundClasses = callGraph.getAlreadyFoundClasses();
        OrderedHashSet<StaticCGJavaMethod> alreadyFoundMethods = callGraph.getAlreadyFoundMethods();        
    }
    
    @Test
    public void testCreateCGMultipleClasses() throws IOException {
        JCallGraph callGraph = new JCallGraph();
        String pathtoJar = "testdata/multipleClassesFalsePos/MultipleClassesFalsePos/dist/MultipleClassesFalsePos.jar";
        File f = new File(pathtoJar);
        callGraph.generateCG(f);      
        OrderedHashSet<StaticCGJavaClass> alreadyFoundClasses = callGraph.getAlreadyFoundClasses();
        alreadyFoundClasses.forEach((c) -> {
            System.out.println(c.getId());
        });
        System.out.println("");
        OrderedHashSet<StaticCGJavaMethod> alreadyFoundMethods = callGraph.getAlreadyFoundMethods();   
        alreadyFoundMethods.forEach((fm) -> {
            System.out.println(fm.getContainingClass().getId() + fm.getId());
            fm.getCalledMethods().forEach((cm) -> {
                System.out.println("    " + cm.getContainingClass().getId() + cm.getId());
            });
        });
        StaticCGJavaClass classA = alreadyFoundClasses.get(0);
        StaticCGJavaMethod classAFalsePos = classA.getContainedMethods().get(1);
        Assert.assertTrue(classA == classAFalsePos.getContainingClass());
        OrderedHashSet<StaticCGJavaMethod> calledMethods = classAFalsePos.getCalledMethods();
        StaticCGJavaMethod classBPutDatumInArr = calledMethods.get(0);
        Assert.assertTrue(alreadyFoundClasses.get(1) == classBPutDatumInArr.getContainingClass());
        Set<StaticCGJavaMethod> allMethodsCalledByMethodRec = callGraph.getAllMethodsCalledByMethodRec(classAFalsePos);
    }
}
