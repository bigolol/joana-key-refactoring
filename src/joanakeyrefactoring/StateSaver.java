package joanakeyrefactoring;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.util.intset.OrdinalSet;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;

import edu.kit.joana.wala.core.CGConsumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import joanakeyrefactoring.persistence.PersistentCGNode;
import joanakeyrefactoring.persistence.PersistentLocalPointerKey;

/**
 * simple class to save intermediate results of SDG constructions (i.e.
 * points-to and call graph) for later use
 */
public class StateSaver implements CGConsumer {

    private CallGraph callGraph;
    private PointerAnalysis<? extends InstanceKey> pointerAnalyis;
    private List<PersistentLocalPointerKey> persistentLocalPointerKeys = new ArrayList<>();
    private Map<Integer, PersistentCGNode> cgNodeIdToPersistentCGNodes = new HashMap<>();
    private Map<CGNode, PersistentCGNode> cgNodesToPersistentCGNodes = new HashMap<>();
    private Map<PersistentLocalPointerKey, List<PersistentLocalPointerKey>> disjunctPointsToSets
            = new HashMap<>();

    @Override
    public void consume(CallGraph callGraph, PointerAnalysis<? extends InstanceKey> pointerAnalyis) {
        this.callGraph = callGraph;
        this.pointerAnalyis = pointerAnalyis;
    }

    public String getSaveString() {
        StringBuilder created = new StringBuilder();
        
        return created.toString();
    }

    public List<PersistentLocalPointerKey> getLocalPointerKeys() {
        return persistentLocalPointerKeys;
    }

    public boolean pointsToSetsAreDisjunct(PersistentLocalPointerKey n1, PersistentLocalPointerKey n2) {
        return true;
    }

    public List<PersistentLocalPointerKey> getDisjunctLPKs(PersistentLocalPointerKey persistentLocalPointerKey) {
        return disjunctPointsToSets.get(persistentLocalPointerKey);
    }

    public void generatePersistenseStructures(SDG sdg) {

        ArrayList<LocalPointerKey> localPointerKeys = new ArrayList<>();
        ArrayList<Integer> cgNodeIds = new ArrayList<>();

        //after this loop: all localpointerkeys in corr arr, all cgNodeids in corr arr, cgNode -> PersistentCGNode map filled
        for (PointerKey pk : pointerAnalyis.getPointerKeys()) {
            if (pk instanceof LocalPointerKey) {
                LocalPointerKey localPointerKey = (LocalPointerKey) pk;
                CGNode corresCgNode = localPointerKey.getNode();
                if (!cgNodesToPersistentCGNodes.containsKey(corresCgNode)) {
                    cgNodesToPersistentCGNodes.put(corresCgNode, new PersistentCGNode());
                }
                int cgNodeId = callGraph.getNumber(corresCgNode);
                localPointerKeys.add(localPointerKey);
                cgNodeIds.add(cgNodeId);
            }
        }

        generateIRsAndPersistentLocalPointerKeys(localPointerKeys, cgNodeIds);
        calculateDisjunctPointsToKeys(localPointerKeys);
    }

    private void generateIRsAndPersistentLocalPointerKeys(ArrayList<LocalPointerKey> localPointerKeys, ArrayList<Integer> cgNodeIds) {
        for (int i = 0; i < localPointerKeys.size(); ++i) {
            LocalPointerKey currentLocalPointerKey = localPointerKeys.get(i);
            CGNode cgNode = currentLocalPointerKey.getNode();
            PersistentCGNode persistentCGNode = cgNodesToPersistentCGNodes.get(cgNode);
            persistentCGNode.createPersistentIR(cgNode, localPointerKeys);

            Integer currentCGNodeId = cgNodeIds.get(i);
            cgNodeIdToPersistentCGNodes.put(currentCGNodeId, persistentCGNode);

            PersistentLocalPointerKey persistentLocalPointerKey = new PersistentLocalPointerKey(currentLocalPointerKey, persistentCGNode);
            persistentLocalPointerKeys.add(persistentLocalPointerKey);
        }
    }

    private void calculateDisjunctPointsToKeys(ArrayList<LocalPointerKey> localPointerKeys) {
        for (int i = 0; i < localPointerKeys.size(); ++i) {
            OrdinalSet<? extends InstanceKey> currentPointsToset = pointerAnalyis.getPointsToSet(localPointerKeys.get(i));
            for (int j = 0; j < localPointerKeys.size(); ++j) {
                if (i == j) {
                    continue;
                }
                OrdinalSet<? extends InstanceKey> otherPointsToSet = pointerAnalyis.getPointsToSet(localPointerKeys.get(j));
                if (disjunct(currentPointsToset, otherPointsToSet)) {
                    if (disjunctPointsToSets.containsKey(persistentLocalPointerKeys.get(i))) {
                        disjunctPointsToSets.get(persistentLocalPointerKeys.get(i)).add(persistentLocalPointerKeys.get(j));
                    } else {
                        List<PersistentLocalPointerKey> list = new ArrayList<>();
                        list.add(persistentLocalPointerKeys.get(j));
                        disjunctPointsToSets.put(persistentLocalPointerKeys.get(i), list);
                    }
                }
            }
        }
    }

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

    public PersistentCGNode getNode(int nodeID) {
        return cgNodeIdToPersistentCGNodes.get(nodeID);
    }

    public List<PersistentLocalPointerKey> getPersistentLocalPointerKeys(PersistentCGNode cGNode) {
        List<PersistentLocalPointerKey> created = new ArrayList<>();
        for (PersistentLocalPointerKey persistentLocalPointerKey : persistentLocalPointerKeys) {
            if (persistentLocalPointerKey.getNode().equals(cGNode) && persistentLocalPointerKey.isParameter()) {
                created.add(persistentLocalPointerKey);
            }
        }
        return created;
    }

    public OrdinalSet<? extends InstanceKey> getPointsToSet(PointerKey pk) {
        return pointerAnalyis.getPointsToSet(pk);
    }

    public Iterable<PointerKey> getPointerKeys() {
        return pointerAnalyis.getPointerKeys();
    }

}
