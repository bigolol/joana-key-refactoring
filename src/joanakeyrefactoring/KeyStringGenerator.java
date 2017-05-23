/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import java.util.Set;

/**
 *
 * @author holger
 */
public class KeyStringGenerator {

    /**
     * describe the sink of a flow within a method. Currently only available for
     * result nodes. Returns null otherwise.
     *
     * @param sinkNode sink node
     * @param sdg the SDG for our program
     * @return Description of sink node (null if no description possible)
     */
    public static String generateKeyDescriptionForSinkOfFlowWithinMethod(SDGNode sinkNode, SDG sdg) {
        if (sinkNode.getKind() == SDGNode.Kind.EXIT) {
            return "\\result";
        } else {
            return "this";
        }
    }

    /**
     * describe the params except the source of a flow within a method.
     * Currently only available for true parameter nodes (explicit parameters
     * and this). Returns null otherwise.
     *
     * @param sourceNode source node
     * @param sdg the SDG for our program
     * @return Description of params except source node (null if no description
     * possible)
     */
    public static String generateKeyDecsriptionForParamsExceptSourceNode(
            SDGNode sourceNode, SDG sdg, CallGraph callGraph) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            if (!sourceNode.getBytecodeName().startsWith("<param> ")
                    && !sourceNode.getKind().name().equals("FORMAL_IN")) {
                return null;
            }
            SDGNode methodNodeInSDG = sdg.getEntry(sourceNode);
            CGNode methodNodeInCG = callGraph.getNode(sdg.getCGNodeId(methodNodeInSDG));
            // get IR to get names of the parameters. Need to compile classes
            // with sufficient debug information for this.

            IR ir = methodNodeInCG.getIR();

            String delim = "";
            Set<SDGNode> formalInNodesOfProcedure = sdg.getFormalInsOfProcedure(methodNodeInSDG);
            for (SDGNode currentFormalInNode : formalInNodesOfProcedure) {

                /**
                 * only describe real parameters
                 */
                if (currentFormalInNode == sourceNode
                        || (!currentFormalInNode.getBytecodeName().startsWith("<param> ")
                        && !currentFormalInNode.getKind().name().equals("FORMAL_IN"))) {
                    continue;
                }
                if (currentFormalInNode.getBytecodeName().startsWith("<param> ")) {
                    int p_number = Integer.parseInt(currentFormalInNode.getBytecodeName()
                            .substring(8));
                    //find out parameter name through IR
                    stringBuilder.append(delim).append(ir.getLocalNames(0, ir.getParameter(p_number))[0]);
                } else {
                    String forInName = currentFormalInNode.getBytecodeName();
                    String[] forInNames = forInName.split("\\.");
                    forInName = forInNames[forInNames.length - 1];
                    stringBuilder.append(delim).append(forInNames);
                }
                delim = ", ";
            }
            //if no other parameter is found, we need to insert "\\nothing" to
            //generate valid JML
        } catch (Exception e) {
            if (stringBuilder.toString().equals("")) {
                return "\\nothing";
            }
        }
        return stringBuilder.toString();
    }


}
