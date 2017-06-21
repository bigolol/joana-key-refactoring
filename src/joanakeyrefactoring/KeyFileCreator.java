/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import joanakeyrefactoring.CustomListener.ParseJavaForKeyListener;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author holgerklein
 */
public class KeyFileCreator {

    /**
     * creates the files which Key uses to disprove information flow at the
     * semantic level. Uses the automationhelper to generate one .java file
     * containing the method for which the summary edge needs to be disproved.
     * Creates two .key files using this .java file. One is an IF contract, the
     * other is a functional contract
     *
     * @param formalNodeTuple
     * @param sdg
     * @param automationHelper
     * @param stateSaver
     * @param javaForKeyListener
     * @throws IOException
     */
    public static void createKeyFiles(
            SDGNodeTuple formalNodeTuple, SDG sdg,
            AutomationHelper automationHelper,
            StateSaver stateSaver,
            ParseJavaForKeyListener javaForKeyListener) throws IOException {
        String methodNameKeY = createJavaFileForKey(formalNodeTuple, sdg, stateSaver, javaForKeyListener, automationHelper);
        String newJavaFile = "proofs.sourceFile";
        automationHelper.createKeYFileIF(newJavaFile, methodNameKeY);
        automationHelper.createKeYFileFunctional(newJavaFile, methodNameKeY);
    }

    public static String createJavaFileForKey(
            SDGNodeTuple formalNodeTuple, SDG sdg,
            StateSaver stateSaver,
            ParseJavaForKeyListener javaForKeyListener,
            AutomationHelper automationHelper) throws UnsupportedEncodingException, IOException {
        SDGNode formalInNode = formalNodeTuple.getFirstNode();
        SDGNode formalOutNode = formalNodeTuple.getSecondNode();
        SDGNode calledMethodNode = sdg.getEntry(formalInNode);

        String descAllFormalInNodes
                = KeyStringGenerator.generateKeyDecsriptionForParamsExceptSourceNode(
                        formalInNode, sdg, stateSaver.callGraph, javaForKeyListener);
        String descOfFormalOutNode
                = KeyStringGenerator.generateKeyDescriptionForSinkOfFlowWithinMethod(formalOutNode, sdg);
        String descrPointsTo = KeyStringGenerator.generatePreconditionFromPointsToSet(sdg, calledMethodNode, stateSaver);

        String calledMethodByteCode = calledMethodNode.getBytecodeMethod();
        String methodName = getMethodNameFromBytecode(calledMethodByteCode);

        String[] paramInClass = automationHelper.createJavaFileForKeyToDisproveMEthod(
                descrPointsTo, methodName, descOfFormalOutNode, descAllFormalInNodes);
        String params = "";
        for (String s : paramInClass) {
            if (s.equals("int[]")) {
                s = "[I";
            }
            params += s + ",";
        }
        params = params.substring(0, params.length() - 1);
        return methodName + "(" + params + ")";
    }

    public static String getMethodNameFromBytecode(String byteCodeMethod) {
        String[] a2 = byteCodeMethod.split("\\.");
        String[] a3 = a2[a2.length - 1].split("\\(");
        String methodName = a3[0];
        if (byteCodeMethod.contains("<init>")) {
            methodName += "." + a2[a2.length - 2].split("\\(")[0];
        }
        return methodName;
    }
}
