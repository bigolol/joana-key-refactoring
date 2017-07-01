/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaClass;

/**
 *
 * @author holger
 */
public class KeyCheckableProjCreator {
    public static void createProject(String pathToJava, Set<StaticCGJavaClass> neededClasses) throws IOException {
        File parentFolder = new File(pathToJava);
        File newProjFile = new File("testProj/");
        newProjFile.createNewFile();   
    }
    
    private static void addAllFolderPathsRel(File source, File dest, File rel) {
        
    }
}
