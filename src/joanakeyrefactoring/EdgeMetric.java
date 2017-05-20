package joanakeyrefactoring;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;

/**
 * metric describing the method selection strategy.
 *
 * First sorting criteria is number of contained summary edges, which should
 * lead to easier methods being checked first. If this number is equal for two
 * edges, bridges are preferred.
 *
 */
public class EdgeMetric implements Comparable<EdgeMetric> {

    /**
     * SDGEdge encapsulated in this EDGEMetric instance
     */
    public SDGEdge edge;
    private boolean machtesPattern = false;
    private boolean regardsLow = false;
    private boolean isBridge = false;
    private int containedEdges;

    public EdgeMetric(SDGEdge e, boolean isBridge, int containedEdges) {
        super();
        this.edge = e;
        this.isBridge = isBridge;
        this.containedEdges = containedEdges;
    }

    public EdgeMetric(SDGEdge e, boolean machtesPattern, boolean isBridge, int containedEdges) {
        super();
        this.edge = e;
        this.machtesPattern = machtesPattern;
        this.isBridge = isBridge;
        this.containedEdges = containedEdges;
    }

    @Override
    public int compareTo(EdgeMetric other) {
        if (machtesPattern && !other.machtesPattern) {
            return -1;
        }
        if (containedEdges != other.containedEdges) {
            return containedEdges - other.containedEdges;
        }
        if (isBridge && !other.isBridge) {
            return -1;
        }
        if (!isBridge && other.isBridge) {
            return 1;
        }
        if (!machtesPattern && other.machtesPattern) {
            return 1;
        }

        /**
         * TODO: i would like to compare the edges themselves here, but the
         * comparator for edges already built into JOANA is not public. And i
         * don't want to copy and paste the code to use it here.
         */
        return 0;
    }

}
