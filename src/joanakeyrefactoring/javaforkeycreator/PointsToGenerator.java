/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.javaforkeycreator;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.intset.OrdinalSet;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import java.util.ArrayList;
import joanakeyrefactoring.StateSaver;

/**
 *
 * @author holger
 */
public class PointsToGenerator {
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
        //get the call graph node corresponding to the SDG method node
        CGNode methodNodeInCallGraph = stateSaver.getNode(sdg.getCGNodeId(methodNode));
        // get IR for parameter names
        IR ir = methodNodeInCallGraph.getIR();
        Iterable<PointerKey> pointerKeys = stateSaver.getPointerKeys();
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
        ArrayList<String> pointsToResult = calculateNonAliases(localPointerKeys, stateSaver, ir);
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
            StateSaver stateSaver, IR ir) {
        int amountLocalPointerKeys = localPointerKeys.size();
        ArrayList<String> result = new ArrayList<String>();
        // enumerate all two element subsets of pointer keys and check if those two have disjunct points-to sets
        for (int i = 0; i < amountLocalPointerKeys; i++) {
            OrdinalSet<? extends InstanceKey> pointsToSet = stateSaver.getPointsToSet(localPointerKeys.get(i));
            for (int j = i + 1; j < amountLocalPointerKeys; j++) {
                if (disjunct(pointsToSet, stateSaver.getPointsToSet(localPointerKeys.get(j)))) {
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
