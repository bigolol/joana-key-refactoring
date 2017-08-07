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
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author holgerklein
 */
public class ViolationChop {

    private SDGNode violationSource;
    private SDGNode violationSink;
    private Collection<SDGNode> nodesInChop;
    private SDG inducedSubgraph;
    private Chopper chopper;
    private Collection<SDGEdge> summaryEdges = new ArrayList<>();

    public ViolationChop(SDGNode violationSource, SDGNode violationSink, SDG sdg) {
        this.violationSource = violationSource;
        this.violationSink = violationSink;
        findSummaryEdges(sdg);
    }

    private ViolationChop() {
    }

    public String generateSaveString() {
        StringBuilder created = new StringBuilder();
        int lengthOfLineSep = System.lineSeparator().length();

        created.append("\"src\" : ").append(violationSource.getId()).append(",").append(System.lineSeparator());
        created.append("\"sink\" : ").append(violationSink.getId()).append(",").append(System.lineSeparator());

        created.append("\"nodes_in_chop\" : [").append(System.lineSeparator());
        for (SDGNode n : nodesInChop) {
            created.append(n.getId()).append(",").append(System.lineSeparator());
        }
        if (created.lastIndexOf("[") != created.length() - 1) {
            created.replace(created.length() - lengthOfLineSep - 1, created.length(), "");
        }
        created.append("]");
        return created.toString();
    }

    public static ViolationChop generateFromSaveString(String s, SDG sdg) {
        JSONObject jSONObject = new JSONObject(s);
        int srcId = jSONObject.getInt("src");
        int sinkId = jSONObject.getInt("sink");
        ViolationChop created = new ViolationChop();
        created.violationSource = sdg.getNode(srcId);
        created.violationSink = sdg.getNode(sinkId);
        created.nodesInChop = new ArrayList<SDGNode>();
        JSONArray nodesInChopArr = jSONObject.getJSONArray("nodes_in_chop");
        for (int i = 0; i < nodesInChopArr.length(); ++i) {
            created.nodesInChop.add(sdg.getNode(nodesInChopArr.getInt(i)));
        }
        created.inducedSubgraph = sdg.subgraph(created.nodesInChop);
        created.inducedSubgraph.edgeSet().forEach((e) -> {
            if (isSummaryEdge(e)) {
                created.summaryEdges.add(e);
            }
        });
        return created;
    }

    public SDGNode getViolationSource() {
        return violationSource;
    }

    public SDGNode getViolationSink() {
        return violationSink;
    }

    public boolean isEmpty() {
        return nodesInChop.isEmpty();
    }

    public Collection<SDGEdge> getSummaryEdges() {
        return summaryEdges;
    }

    public void findSummaryEdges(SDG sdg) {
        this.chopper = new RepsRosayChopper(sdg);
        nodesInChop = chopper.chop(violationSource, violationSink);
        if (nodesInChop.isEmpty()) {
            return;
        }
        inducedSubgraph = sdg.subgraph(nodesInChop);

        inducedSubgraph.edgeSet().forEach((e) -> {
            if (isSummaryEdge(e)) {
                summaryEdges.add(e);
            }
        });
    }

    private static boolean isSummaryEdge(SDGEdge currentEdge) {
        return currentEdge.getKind() == SDGEdge.Kind.SUMMARY;
    }
}
