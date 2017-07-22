/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaforkeycreator;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaClass;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;
import org.apache.bcel.generic.F2D;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author holger
 */
public class JavaProjectCopyHandler {

    private String pathToSource;
    private String pathToNew;
    private File newFile;
    private CopyKeyCompatibleListener copyKeyCompatibleListener;

    public JavaProjectCopyHandler(String pathToSource, String pathToNew, CopyKeyCompatibleListener copyKeyCompatibleListener) throws IOException {
        this.pathToSource = pathToSource;
        this.pathToNew = pathToNew;
        this.copyKeyCompatibleListener = copyKeyCompatibleListener;
        newFile = new File(pathToNew);
        clearFolder();
        if (!newFile.exists()) {
            newFile.mkdirs();
        }
    }

    public void clearFolder() throws IOException {
        FileUtils.deleteDirectory(newFile);
    }

    public static String getRelPathForJavaClass(StaticCGJavaClass cgjs) {
        String packageString = cgjs.getPackageString();
        String packageToRelPathString = "";
        if (packageString != null) {
            packageToRelPathString = "/" + packageString.replaceAll("\\.", "/") + "/";
        }
        return packageToRelPathString;
    }

    public void addClassToTest(List<String> classContent, StaticCGJavaClass javaClass) throws FileNotFoundException, IOException {
        String relPathForJavaClass = getRelPathForJavaClass(javaClass);
        String className = javaClass.getOnlyClassName();
        File relPathFile = new File(pathToNew + "/" + relPathForJavaClass);
        relPathFile.mkdirs();
        File javaFile = new File(pathToNew + relPathForJavaClass + "/" + className + ".java");
        javaFile.createNewFile();
        String classFileAsOneString = "";
        for (String l : classContent) {
            classFileAsOneString += l + System.lineSeparator();
        }
        PrintWriter out = new PrintWriter(javaFile);
        out.print(classFileAsOneString);
        out.close();
    }

    public void copyClasses(Map<StaticCGJavaClass, Set<StaticCGJavaMethod>> classesToCopy) throws IOException {
        for (StaticCGJavaClass currentClassToCopy : classesToCopy.keySet()) {
            String relPathForJavaClass = getRelPathForJavaClass(currentClassToCopy);
            String className = currentClassToCopy.getOnlyClassName();

            File folderPathNewNew = new File(pathToNew + relPathForJavaClass);
            if (!folderPathNewNew.exists()) {
                folderPathNewNew.mkdirs();
            }
            try {
                File classFileToCopyTo = new File(pathToNew + relPathForJavaClass + className + ".java");
                File classFileToCopyFrom = new File(pathToSource + relPathForJavaClass + className + ".java");

                String contents = new String(java.nio.file.Files.readAllBytes(classFileToCopyFrom.toPath()));

                String keyCompatibleContents = copyKeyCompatibleListener.generateKeyCompatible(contents, classesToCopy.get(currentClassToCopy));

                FileUtils.writeStringToFile(classFileToCopyTo, keyCompatibleContents);

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }
}
