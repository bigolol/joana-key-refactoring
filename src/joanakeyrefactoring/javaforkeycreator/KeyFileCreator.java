/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaforkeycreator;

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
        PrintWriter writer;
        File proofObFile = new File(pathToSave + "/" + "proofObligationIF.key");
        if (!proofObFile.exists()) {
            proofObFile.createNewFile();
        }

        final String profileStr = "Java Profile";
        final String javaSourceStr = "/";
        final String proofObligationTemplateString
                = "#Proof Obligation Settings\n"
                + "name=CLASSNAME[CLASSNAME\\\\:\\\\:METHODNAME].Non-interference contract.0\n"
                + "contract=CLASSNAME[CLASSNAME\\\\:\\\\:METHODNAME].Non-interference contract.0\n"
                + "class=de.uka.ilkd.key.informationflow.po.InfFlowContractPO\n";
        final String proofObligationString = proofObligationTemplateString
                .replaceAll("METHODNAME", method.getId())
                .replaceAll("CLASSNAME", method.getContainingClass().getId());

        generateKeyFileFrom(profileStr, javaSourceStr, proofObligationString, proofObFile);
    }

    /**
     * Creates the Functional Proof Obligation for KeY
     *
     * @param javaFile
     * @param method
     */
    public static void createKeYFileFunctional(String javaFile, String method) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        File proofObFile = new File("proofObs/proofObFunc.key");
        if (!proofObFile.exists()) {
            proofObFile.createNewFile();
        }
        PrintWriter writer;
        writer = new PrintWriter("proofObs/proofObFunc.key", "UTF-8");
        String firstRow = "\\profile \"Java Profile\";";
        writer.println(firstRow);
        // Java Source
        String js = "\\javaSource \"proofs\";";
        writer.println(js);
        // Proof Obligation
        String p1 = "\\proofObligation \"#Proof Obligation Settings";
        writer.println(p1);
        String obliName = "name = " + javaFile + "[" + javaFile
                + "\\\\:\\\\:" + method + "].JML operation contract.0";
        writer.println(obliName);
        String obliContract = "contract = " + javaFile + "[" + javaFile
                + "\\\\:\\\\:" + method + "].JML operation contract.0";
        writer.println(obliContract);

        String obliClass = "class=de.uka.ilkd.key.proof.init.FunctionalOperationContractPO";
        writer.println(obliClass);
        String end = "\";";
        writer.println(end);
        writer.close();
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
