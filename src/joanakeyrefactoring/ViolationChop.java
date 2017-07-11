/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author holgerklein
 */
public class ViolationChop {

    private final SDGNode violationSource;
    private final SDGNode violationSink;
    private Collection<SDGNode> violationChop;
    private SDG inducedSubgraph;
    private Chopper chopper;
    private Collection<SDGEdge> summaryEdges = new ArrayList<>();

    public ViolationChop(SDGNode violationSource, SDGNode violationSink, SDG sdg) {
        this.violationSource = violationSource;
        this.violationSink = violationSink;
        findSummaryEdges(sdg);
    }

    public SDGNode getViolationSource() {
        return violationSource;
    }

    public SDGNode getViolationSink() {
        return violationSink;
    }

    public boolean isEmpty() {
        return violationChop.isEmpty();
    }

    public Collection<SDGEdge> getSummaryEdges() {
        return summaryEdges;
    }

    public void findSummaryEdges(SDG sdg) {
        this.chopper = new RepsRosayChopper(sdg);
        violationChop = chopper.chop(violationSource, violationSink);
        if (violationChop.isEmpty()) {
            return;
        }
        inducedSubgraph = sdg.subgraph(violationChop);

        inducedSubgraph.edgeSet().forEach((e) -> {
            if (isSummaryEdge(e)) {
                summaryEdges.add(e);
            }
        });
    }

    private boolean isSummaryEdge(SDGEdge currentEdge) {
        return currentEdge.getKind() == SDGEdge.Kind.SUMMARY;
    }
}
