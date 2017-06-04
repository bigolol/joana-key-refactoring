/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;

/**
 *
 * @author holgerklein
 */
class BasicMetric {

    public boolean machtesPattern = false;
    public boolean regardsLow = false;
    public boolean isBridge = false;
    public int containedEdges;


    public BasicMetric(boolean machtesPattern, boolean isBridge, int containedEdges) {
        this.machtesPattern = machtesPattern;
        this.isBridge = isBridge;
        this.containedEdges = containedEdges;
    }
}
