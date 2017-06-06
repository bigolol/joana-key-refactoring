/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.intset.OrdinalSet;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import java.util.ArrayList;
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

    public static String generateKeyDescrForParamsViaListener(
            SDGNode sourceNode, SDG sdg, ParseJavaForKeyListener listener) {
        final String param = "<param>";
        final String formalIn = "FORMAL_IN";
        String srcNodeByteCodeName = sourceNode.getBytecodeName();
        String srcNodeKindName = sourceNode.getKind().name();
        if (!srcNodeByteCodeName.startsWith(param)
                && !srcNodeKindName.equals(formalIn)) {
            return null;
        }
        SDGNode methodNodeInSDG = sdg.getEntry(sourceNode);
        String methodNameBC = methodNodeInSDG.getBytecodeMethod();
        Set<SDGNode> formalInNodesOfProcedure = sdg.getFormalInsOfProcedure(methodNodeInSDG);
        StringBuilder stringBuilder = new StringBuilder();
        for (SDGNode currentFormalInNode : formalInNodesOfProcedure) {
            String currentNodeBC = currentFormalInNode.getBytecodeName();
            String nameOfKind = currentFormalInNode.getKind().name();
            if (currentFormalInNode == sourceNode
                    || (!nameOfKind.startsWith(param) && !nameOfKind.equals("FORMAL_IN"))) {
                continue;
            }
            if (currentNodeBC.startsWith(param)) {
                int paramPos = Integer.parseInt(currentNodeBC.substring(param.length() + 1)); //+ 1 for the trailing space
                String paramName = listener.getParamsOfMethodByByteCode(methodNameBC)[paramPos];
                stringBuilder.append(paramName).append(", ");
            } else {
                String[] forInNames = currentNodeBC.split("\\.");
                currentNodeBC = forInNames[forInNames.length - 1];
                stringBuilder.append(currentNodeBC).append(", ");
            }
        }

        String created = stringBuilder.toString();

        if (created.length() == 0) {
            return "\\nothing";
        } else {
            return created.substring(0, created.length() - 2);
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
            SDGNode sourceNode, SDG sdg, CallGraph callGraph, ParseJavaForKeyListener listener) {

        String srcNodeByteCodeName = sourceNode.getBytecodeName();
        String srcNodeKindName = sourceNode.getKind().name();
        if (!srcNodeByteCodeName.startsWith("<param>")
                && !srcNodeKindName.equals("FORMAL_IN")) {
            return null;
        }
        SDGNode methodNodeInSDG = sdg.getEntry(sourceNode);
        int cgNodeId = sdg.getCGNodeId(methodNodeInSDG);

        // get IR to get names of the parameters. Need to compile classes
        // with sufficient debug information for this.
        CGNode methodNodeInCG = callGraph.getNode(cgNodeId);
        IR intermedRep = methodNodeInCG.getIR(); //represents instructions of method, close to bytecode

        String delim = "";
        Set<SDGNode> formalInNodesOfProcedure = sdg.getFormalInsOfProcedure(methodNodeInSDG);

        StringBuilder stringBuilder = new StringBuilder();
        for (SDGNode currentFormalInNode : formalInNodesOfProcedure) {
            final String param = "<param>";
            String bytecodeName = currentFormalInNode.getBytecodeName();
            String nameOfKind = currentFormalInNode.getKind().name();
            if (currentFormalInNode == sourceNode
                    || (!nameOfKind.startsWith(param) && !nameOfKind.equals("FORMAL_IN"))) {
                continue;
            }
            if (bytecodeName.startsWith(param)) {
                int p_number = Integer.parseInt(bytecodeName.substring(param.length() + 1)); //+ 1 for the trailing space
                int valueNumber = intermedRep.getParameter(p_number); //starts at 1, meaning vn 1 is this for methods
                SSAInstruction[] instructions = intermedRep.getInstructions();
                String[] localNames = intermedRep.getLocalNames(0, valueNumber);
                if (localNames == null) {
                    return generateKeyDescrForParamsViaListener(sourceNode, sdg, listener);
                }
                String nameOfParameter = localNames[0];
                stringBuilder.append(delim).append(nameOfParameter);
            } else {
                String[] forInNames = bytecodeName.split("\\.");
                bytecodeName = forInNames[forInNames.length - 1];
                stringBuilder.append(delim).append(bytecodeName);
            }
            delim = ", ";
        }
        //if no other parameter is found, we need to insert "\\nothing" to
        //generate valid JML
        if (stringBuilder.toString().equals("")) {
            return "\\nothing";
        }

        return stringBuilder.toString();
    }

    /**
     * Creates loop invariants. Is not complete and only fills the determines
     * clause.
     *
     * @param descSinkG
     * @param descOtherParamsG
     * @param methodName
     * @param loopJava
     * @return loop invariant
     */
    public static String createLoopInvariant(String descSinkG,
            String descOtherParamsG, String methodName, String loopJava) {
        StringBuilder sb = new StringBuilder();
        String loopInvariant = "";

        String header = "\t/*\t@ loop_invariant " + "";
        String assignable = "\t \t @ assignable " + "";
        String descSink = descSinkG + "";
        String descOtherParams = descOtherParamsG + "";

        String determines = "\t \t @ determines " + descSink + " \\by "
                + descOtherParams + "; ";
        String decreases = "\t \t @ decreases " + "" + "*/";

        sb.append(header);
        sb.append(System.lineSeparator());
        if (assignable.length() > 11) {
            sb.append(assignable);
            sb.append(System.lineSeparator());
        }
        sb.append(determines);
        sb.append(System.lineSeparator());
        sb.append(decreases);
        loopInvariant = sb.toString();

        return loopInvariant;
    }

    /**
     * Calculates non-aliasing information for parameters of a method node using
     * JOANA's points-to information. From the paper: Definition 9 (Generation
     * of preconditions). Let o be a reference and P_o its points-to set. We
     * generate the following precondition: for all o' in P_o: o = o'
     *
     * @param sdg our SDG
     * @param methodNode method node to check
     * @return non-aliasing information as a string that can be used as
     * precondition
     */
    public static String generatePreconditionFromPointsToSet(SDG sdg, SDGNode methodNode, StateSaver stateSaver) {
        PointerAnalysis<? extends InstanceKey> pointerAnalyis = stateSaver.pointerAnalyis;
        //get the call graph node corresponding to the SDG method node
        CGNode methodNodeInCallGraph = stateSaver.callGraph.getNode(sdg.getCGNodeId(methodNode));
        // get IR for parameter names
        IR ir = methodNodeInCallGraph.getIR();
        Iterable<PointerKey> pointerKeys = pointerAnalyis.getPointerKeys();
        ArrayList<LocalPointerKey> localPointerKeys = new ArrayList<LocalPointerKey>();
        for (PointerKey currentPointerKey : pointerKeys) {
            if (currentPointerKey instanceof LocalPointerKey) {
                LocalPointerKey localPointerKey = (LocalPointerKey) currentPointerKey;
                if (localPointerKey.getNode() == methodNodeInCallGraph && localPointerKey.isParameter()) {
                    localPointerKeys.add(localPointerKey);
                }
            }
        }
        // calculate individual non-alias clauses
        ArrayList<String> pointsToResult = calculateNonAliases(localPointerKeys, pointerAnalyis, ir);
        StringBuilder stringBuilder = new StringBuilder();
        String delim = "";
        //chain clauses together by conjunction
        for (String nonAliased : pointsToResult) {
            stringBuilder.append(delim).append(nonAliased);
            delim = " && ";
        }
        /**
         * for simpler code, if we don't have any clauses, we return "true" here
         * instead of writing code that does not emit a aliasing precondition.
         * do it the proper way
         */
        if (stringBuilder.toString().equals("")) {
            return "true";
        }
        return stringBuilder.toString();
    }

    private static ArrayList<String> calculateNonAliases(
            ArrayList<LocalPointerKey> localPointerKeys,
            PointerAnalysis<? extends InstanceKey> pointerAnalysis, IR ir) {
        int amountLocalPointerKeys = localPointerKeys.size();
        ArrayList<String> result = new ArrayList<String>();
        // enumerate all two element subsets of pointer keys and check if those two have disjunct points-to sets
        for (int i = 0; i < amountLocalPointerKeys; i++) {
            OrdinalSet<? extends InstanceKey> pointsToSet = pointerAnalysis.getPointsToSet(localPointerKeys.get(i));
            for (int j = i + 1; j < amountLocalPointerKeys; j++) {
                if (disjunct(pointsToSet, pointerAnalysis.getPointsToSet(localPointerKeys.get(j)))) {
                    // get the names of the parameters associated with the pointer keys                     
                    String o1 = ir.getLocalNames(0, localPointerKeys.get(i).getValueNumber())[0];
                    String o2 = ir.getLocalNames(0, localPointerKeys.get(j).getValueNumber())[0];
                    // if points-to sets are disjunct, o1 and o2 cannot alias
                    result.add(o1 + " != " + o2);
                }
            }
        }
        return result;
    }

    /**
     * calculates whether two Ordinal sets are disjunct.
     */
    private static boolean disjunct(OrdinalSet<?> s1, OrdinalSet<?> s2) {
        for (Object e1 : s1) {
            for (Object e2 : s2) {
                if (e1.equals(e2)) {
                    return false;
                }
            }
        }
        return true;
    }

}
