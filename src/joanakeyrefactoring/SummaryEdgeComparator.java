/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import java.util.ArrayList;
import java.util.Comparator;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;

/**
 *
 * @author holger
 */
public class SummaryEdgeComparator implements Comparator<SDGEdge> {

    private ViolationsWrapper violationsWrapper;

    public SummaryEdgeComparator(ViolationsWrapper violationsWrapper) {
        this.violationsWrapper = violationsWrapper;
    }

    @Override
    public int compare(SDGEdge e1, SDGEdge e2) {
        StaticCGJavaMethod e1Method = violationsWrapper.getMethodCorresToSummaryEdge(e1);
        StaticCGJavaMethod e2Method = violationsWrapper.getMethodCorresToSummaryEdge(e2);

        if (e1Method.getContainingClass().getPackageString().startsWith("java")
                && !e2Method.getContainingClass().getPackageString().startsWith("java")) {
            return Comparator.<Integer>naturalOrder().compare(2, 1);
        } else if (!e1Method.getContainingClass().getPackageString().startsWith("java")
                && e2Method.getContainingClass().getPackageString().startsWith("java")) {
            return Comparator.<Integer>naturalOrder().compare(1, 2);
        }

        ArrayList<ViolationChop> e1Chop = violationsWrapper.getChopsContaining(e1);
        ArrayList<ViolationChop> e2Chop = violationsWrapper.getChopsContaining(e2);
        return Comparator.<Integer>naturalOrder().compare(e1Chop.size(), e2Chop.size());
    }
}
