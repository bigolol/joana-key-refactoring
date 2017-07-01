/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaforkeycreator;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;

/**
 *
 * @author holger
 */
public class KeyFileCreator {

    /**
     * Creates the Information flow Proof Obligation for KeY.
     *
     * @param javaFile
     * @param method
     */
    public static void createKeYFileIF(StaticCGJavaMethod method, String pathToSave) throws IOException {
        File proofObFile = new File(pathToSave + "/" + "proofObligationIF.key");
        if (!proofObFile.exists()) {
            proofObFile.createNewFile();
        }

        String methodnameKey = getMethodnameKey(method);

        final String profileStr = "Java Profile";
        final String javaSourceStr = "./";
        final String proofObligationTemplateString
                = "#Proof Obligation Settings\n"
                + "name=CLASSNAME[CLASSNAME\\\\:\\\\:METHODNAME].Non-interference contract.0\n"
                + "contract=CLASSNAME[CLASSNAME\\\\:\\\\:METHODNAME].Non-interference contract.0\n"
                + "class=de.uka.ilkd.key.informationflow.po.InfFlowContractPO\n";
        final String proofObligationString = proofObligationTemplateString
                .replaceAll("METHODNAME", methodnameKey)
                .replaceAll("CLASSNAME", method.getContainingClass().getId());

        generateKeyFileFrom(profileStr, javaSourceStr, proofObligationString, proofObFile);
    }

    public static void createKeYFileFunctional(StaticCGJavaMethod method, String pathToSave) throws IOException {
        File proofObFile = new File(pathToSave + "/" + "proofObligationFunctional.key");
        if (!proofObFile.exists()) {
            proofObFile.createNewFile();
        }

        final String profileStr = "Java Profile";
        final String javaSourceStr = "./";

        String methodnameKey = getMethodnameKey(method);

        final String proofObligationTemplateString
                = "#Proof Obligation Settings\n"
                + "name=CLASSNAME[CLASSNAME\\\\:\\\\:METHODNAME].JML operation contract.0\n"
                + "contract=CLASSNAME[CLASSNAME\\\\:\\\\:METHODNAME].JML operation contract.0\n"
                + "class=de.uka.ilkd.key.proof.init.FunctionalOperationContractPO\n";
        final String proofObligationString = proofObligationTemplateString
                .replaceAll("METHODNAME", methodnameKey)
                .replaceAll("CLASSNAME", method.getContainingClass().getId());

        generateKeyFileFrom(profileStr, javaSourceStr, proofObligationString, proofObFile);
    }

    private static String getMethodnameKey(StaticCGJavaMethod method) {
        return method.getId() + "(" + method.getParameter().replaceAll("int\\[\\]", "\\[I") + ")";
    }  
   

    private static void generateKeyFileFrom(
            String profileString, String javaSourceString,
            String proofObligationString, File f) throws IOException {

        String profileTempStr = "\\profile PROFILE;\n";
        String javaSourceTempStr = "\\javaSource JAVASRC;\n";
        String proofOblTempStr = "\\proofObligation PROOFOBL;\n";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(profileTempStr.replace("PROFILE", surroundWithApos(profileString)));
        stringBuilder.append('\n');
        stringBuilder.append(javaSourceTempStr.replace("JAVASRC", surroundWithApos(javaSourceString)));
        stringBuilder.append('\n');
        stringBuilder.append(proofOblTempStr.replace("PROOFOBL", surroundWithApos(proofObligationString)));

        PrintWriter writer = new PrintWriter(f, "UTF-8");
        writer.print(stringBuilder.toString());
        writer.close();
    }

    private static String surroundWithApos(String s) {
        return "\"" + s + "\"";
    }

}
