/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaforkeycreator;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import joanakeyrefactoring.CustomListener.GetMethodBodyListener;
import joanakeyrefactoring.StateSaver;
import joanakeyrefactoring.staticCG.JCallGraph;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaClass;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;

/**
 *
 * @author holger
 */
public class JavaForKeyCreator {

    private String pathToJavaSource;
    private String pathToTestJava = "proofObs/proofs";
    private JCallGraph callGraph;
    private SDG sdg;
    private StateSaver stateSaver;
    private IFCAnalysis analysis;
    private JavaProjectCopyHandler javaProjectCopyHandler;

    private GetMethodBodyListener methodBodyListener = new GetMethodBodyListener();

    public JavaForKeyCreator(String pathToJavaSource, JCallGraph callGraph, SDG sdg, StateSaver stateSaver, IFCAnalysis analysis) throws IOException {
        this.pathToJavaSource = pathToJavaSource;
        this.callGraph = callGraph;
        this.sdg = sdg;
        this.stateSaver = stateSaver;
        this.analysis = analysis;
    }

    public String generateJavaForFormalNodeTuple(
            SDGNodeTuple formalNodeTuple,
            StaticCGJavaMethod methodCorresToSE) throws IOException {
        SDGNode formalInNode = formalNodeTuple.getFirstNode();
        StaticCGJavaClass containingClass = methodCorresToSE.getContainingClass();
        String relPathForJavaClass
                = JavaProjectCopyHandler.getRelPathForJavaClass(containingClass);
        File javaClassFile = new File(pathToJavaSource + relPathForJavaClass + containingClass.getOnlyClassName() + ".java");

        if (!javaClassFile.exists()) { //it is a library class since it doesnt exist in the project
            throw new FileNotFoundException();
        }

        String contents = new String(Files.readAllBytes(javaClassFile.toPath()));
        methodBodyListener.parseFile(contents, methodCorresToSE);

        String inputDescrExceptFormalIn = getInputExceptFormalIn(formalInNode, methodCorresToSE);
        String sinkDescr = generateSinkDescr(formalNodeTuple.getSecondNode());
        String pointsToDecsr = PointsToGenerator.generatePreconditionFromPointsToSet(
                sdg, sdg.getEntry(formalInNode), stateSaver);

        Set<StaticCGJavaClass> allNecessaryClasses = getAllNecessaryClasses(methodCorresToSE);

        javaProjectCopyHandler = new JavaProjectCopyHandler(pathToJavaSource, pathToTestJava);
        javaProjectCopyHandler.copyClasses(allNecessaryClasses);

        List<String> classFileForKey = generateClassFileForKey(inputDescrExceptFormalIn, sinkDescr, pointsToDecsr, contents);

        javaProjectCopyHandler.addClassToTest(classFileForKey, containingClass);

        KeyFileCreator.createKeYFileIF(methodCorresToSE, pathToTestJava);
        KeyFileCreator.createKeYFileFunctional(methodCorresToSE, pathToTestJava);

        return pathToTestJava;
    }

    private List<String> generateClassFileForKey(
            String inputDescrExceptFormalIn,
            String sinkDescr,
            String pointsToDecsr,
            String classContents) {

        List<String> lines = new ArrayList<>();
        for (String l : classContents.split("\n")) {
            lines.add(l);
        }
        int methodStartLine = methodBodyListener.getMethodStartLine();

        //insert nullable between passed variables
        lines.remove(methodStartLine - 1);
        lines.add(methodStartLine - 1, methodBodyListener.getMethodDeclWithNullable() + " {");

        String descriptionForKey
                = "\t/*@ requires "
                + pointsToDecsr
                + ";\n\t  @ determines " + sinkDescr + " \\by "
                + inputDescrExceptFormalIn + "; */";

        lines.add(methodStartLine - 1, descriptionForKey);

        return lines;
    }

    private Set<StaticCGJavaClass> getAllNecessaryClasses(StaticCGJavaMethod method) {
        Set<StaticCGJavaMethod> allMethodsCalledByMethodRec = callGraph.getAllMethodsCalledByMethodRec(method);
        Set<StaticCGJavaClass> created = new HashSet<>();
        for (StaticCGJavaMethod m : allMethodsCalledByMethodRec) {
            if (m.getContainingClass() != method.getContainingClass()
                    && !created.contains(m.getContainingClass())) {
                created.add(m.getContainingClass());
            }
        }
        return created;
    }

    private String generateSinkDescr(SDGNode sinkNode) {
        if (sinkNode.getKind() == SDGNode.Kind.EXIT) {
            return "\\result";
        } else {
            return "this";
        }
    }

    private String getInputExceptFormalIn(SDGNode formalInNode, StaticCGJavaMethod methodCorresToSE) {
        SDGNode methodNodeInSDG = sdg.getEntry(formalInNode);
        Set<SDGNode> formalInNodesOfProcedure = sdg.getFormalInsOfProcedure(methodNodeInSDG);
        String created = "";
        final String param = "<param>";
        for (SDGNode currentFormalInNode : formalInNodesOfProcedure) {
            String nameOfKind = currentFormalInNode.getKind().name();
            if (currentFormalInNode == formalInNode
                    || (!nameOfKind.startsWith(param) && !nameOfKind.equals("FORMAL_IN"))) {
                continue;
            }
            String bytecodeName = currentFormalInNode.getBytecodeName();
            if (bytecodeName.startsWith(param)) {
                int p_number = Integer.parseInt(bytecodeName.substring(param.length() + 1)); //+ 1 for the trailing space
                if (!methodCorresToSE.isStatic()) {
                    if (p_number == 0) {
                        created += "this, ";
                    } else {
                        created += methodBodyListener.getExtractedMethodParamNames().get(p_number - 1) + ", ";
                    }
                } else {
                    created += methodBodyListener.getExtractedMethodParamNames().get(p_number) + ", ";
                }
            } else {
                String[] forInNames = bytecodeName.split("\\.");
                String forInName = forInNames[forInNames.length - 1];
                if (!bytecodeName.equals("<[]>")) {
                    created += forInName + ", ";
                }
            }
        }
        if (created.isEmpty()) {
            return "\\nothing";
        } else {
            return created.substring(0, created.length() - 2);
        }
    }

}
