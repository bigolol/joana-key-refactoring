package joanakeyrefactoring;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;

import edu.kit.joana.wala.core.CGConsumer;

/**
 * simple class to save intermediate results of SDG constructions (i.e.
 * points-to and call graph) for later use
 */
public class StateSaver implements CGConsumer {

    public CallGraph callGraph;
    public PointerAnalysis<? extends InstanceKey> pointerAnalyis;

    @Override
    public void consume(CallGraph callGraph, PointerAnalysis<? extends InstanceKey> pointerAnalyis) {
        this.callGraph = callGraph;
        this.pointerAnalyis = pointerAnalyis;
    }

}
