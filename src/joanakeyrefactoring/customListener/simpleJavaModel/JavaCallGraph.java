/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.customListener.simpleJavaModel;

import java.util.List;
import joanakeyrefactoring.CustomListener.ExtractJavaProjModelListener;

/**
 *
 * @author holger
 */
public class JavaCallGraph {

    private List<JavaMethod> extractedMethods;
    private List<JavaClass> extractedClasses;

    public JavaCallGraph(String projectAllClassesInOneString) {
        ExtractJavaProjModelListener extractJavaProjModelListener
                = new ExtractJavaProjModelListener();
        extractJavaProjModelListener.extractDataFromProject(projectAllClassesInOneString);
        extractedClasses = extractJavaProjModelListener.getExtractedClasses();
        extractedMethods = extractJavaProjModelListener.getExtractedMethods();
    }

    public List<JavaMethod> getAllMethods() {
        return extractedMethods;
    }

    public List<JavaMethod> getAllMethodsCalledByMethod(JavaMethod callee) {
        return callee.getCalledMethods();
    }

}
