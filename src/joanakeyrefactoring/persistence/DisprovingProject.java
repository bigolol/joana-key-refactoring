/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.persistence;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author hklein
 */
public class DisprovingProject {

    private String pathToSDG;
    private String pathToCGJson;
    private IFCAnalysis ana;
    

    public DisprovingProject(String pathToSDG, String pathToCGJson) throws IOException {
        this.pathToSDG = pathToSDG;
        this.pathToCGJson = pathToCGJson;
        SDGProgram program = SDGProgram.loadSDG(pathToSDG);
        ana = new IFCAnalysis(program);        
    }

}
