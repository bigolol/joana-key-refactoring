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
import java.util.List;

/**
 *
 * @author holger
 */
public class JoanaAndKeyCheckData {

    private List<String> annotationsSink;
    private List<String> annotationsSource;
    private String pathKeY;
    private String classPath;
    private String pathToJavaFile;
    private String entryMethodString;
    private String annotationPath;
    private JavaMethodSignature entryMethod;
    private boolean fullyAutomatic;
    private AnnotationAdder annoAdder;

    public JoanaAndKeyCheckData(List<String> annotationsSink, List<String> annotationsSource, String pathKeY, String classPath, String pathToJavaFile, String entryMethodString, String annotationPath, JavaMethodSignature entryMethod, boolean fullyAutomatic, AnnotationAdder annoAdder) {
        this.annotationsSink = annotationsSink;
        this.annotationsSource = annotationsSource;
        this.pathKeY = pathKeY;
        this.classPath = classPath;
        this.pathToJavaFile = pathToJavaFile;
        this.entryMethodString = entryMethodString;
        this.annotationPath = annotationPath;
        this.entryMethod = entryMethod;
        this.fullyAutomatic = fullyAutomatic;
        this.annoAdder = annoAdder;
    }

    public boolean isFullyAutomatic() {
        return fullyAutomatic;
    }

    public JavaMethodSignature getEntryMethod() {
        return entryMethod;
    }

    public List<String> getAnnotationsSink() {
        return annotationsSink;
    }

    public List<String> getAnnotationsSource() {
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
