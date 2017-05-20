/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import java.io.File;
import java.util.Collection;
import java.util.List;
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
public class AutomationHelperTest {

    public AutomationHelperTest() {
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
    public void testListFilesForFolder() {
        final String path = "/home/holger/Code/hiwi/joana-api-refactoring/JZipWithException/jzip";
        AutomationHelper automationHelper = new AutomationHelper(path);
        Collection<File> filesInFolder = automationHelper.listAllJavaFilesInFolder(new File(path));
        String[] names = {"MyZipInputStream.java", "JZip.java", "MyFileOutputStream.java"};
        Object[] filesnames = filesInFolder.stream().map(f -> {return f.getName();}).toArray();
        assertArrayEquals(names, filesnames);
    }
    
    @Test
    public void testSummarizeSourceFiles() {
        final String path = "/home/holger/Code/hiwi/joana-api-refactoring/JZipWithException/jzip";
        AutomationHelper automationHelper = new AutomationHelper(path);
        String sourceFiles = automationHelper.readAllSourceFilesIntoOneStringAndFillClassMap();
        //System.out.println(sourceFiles);
    }
    
    @Test
    public void testExportJava() {
    }
    
    

}
