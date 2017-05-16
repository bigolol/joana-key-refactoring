/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author holger
 */
public class JoanaAndKeyCheckData {

    private ArrayList<String> annotationsSink = new ArrayList<>();
    private ArrayList<String> annotationsSource = new ArrayList<>();
    private String pathKeY;
    private String classPath;
    private String pathToJavaFile;
    private String entryMethodString;
    private String annotationPath;
    private JavaMethodSignature entryMethod;
    private boolean fullyAutomatic;
    private AnnotationAdder annoAdder;

    public JoanaAndKeyCheckData(
            String pathKeY, String classPath, String pathToJavaFile, String entryMethodString,
            String annotationPath, JavaMethodSignature entryMethod,
            boolean fullyAutomatic, AnnotationAdder annoAdder) {
        this.pathKeY = pathKeY;
        this.classPath = classPath;
        this.pathToJavaFile = pathToJavaFile;
        this.entryMethodString = entryMethodString;
        this.annotationPath = annotationPath;
        this.entryMethod = entryMethod;
        this.annoAdder = annoAdder;
        this.fullyAutomatic = fullyAutomatic;
    }

    public boolean isFullyAutomatic() {
        return fullyAutomatic;
    }

    public JavaMethodSignature getEntryMethod() {
        return entryMethod;
    }

    public ArrayList<String> getAnnotationsSink() {
        return annotationsSink;
    }

    public ArrayList<String> getAnnotationsSource() {
        return annotationsSource;
    }

    public String getPathKeY() {
        return pathKeY;
    }

    public String getClassPath() {
        return classPath;
    }

    public String getPathToJavaFile() {
        return pathToJavaFile;
    }

    public String getEntryMethodString() {
        return entryMethodString;
    }

    public String getAnnotationPath() {
        return annotationPath;
    }

    public void addAnnotations(IFCAnalysis analysis) throws CouldntAddAnnoException {
        this.annoAdder.addAnnotations(analysis, this);
    }
}
