package joanakeyrefactoring;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.CGConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * simple class to save intermediate results of SDG constructions (i.e.
 * points-to and call graph) for later use
 */
public class StateSaver implements CGConsumer {    
    
    private CallGraph callGraph;
    private PointerAnalysis<? extends InstanceKey> pointerAnalyis;
    private List<PointerKey> keys = new ArrayList<>();
    private Map<PointerKey, OrdinalSet<? extends InstanceKey>> pointsToSets = new HashMap<>();  
    private Map<PointerKey, CGNode> pkeysToCGNode = new HashMap<>();
    
    

    public String getSaveString() {
        StringBuilder created = new StringBuilder();        
        return created.toString();
    }       
    @Override
    public void consume(CallGraph callGraph, PointerAnalysis<? extends InstanceKey> pointerAnalyis) {
        this.callGraph = callGraph;
        this.pointerAnalyis = pointerAnalyis;   
    }
    
    public OrdinalSet<? extends InstanceKey> getPointsToSet(PointerKey pk) {
        return pointerAnalyis.getPointsToSet(pk);
    }
    
    public Iterable<PointerKey> getPointerKeys() {
        return pointerAnalyis.getPointerKeys();
    }
    
    public CGNode getNode(int nodeID) {
        return callGraph.getNode(nodeID);
    }

}
