/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.persistence;

import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;

/**
 *
 * @author hklein
 */
public class PersistentLocalPointerKey {
    
    private int valueNumber;
    private PersistentCGNode persistentCGNode;
    private boolean parameter;
    
    public PersistentLocalPointerKey(LocalPointerKey localPointerKey, PersistentCGNode persistentCGNode) {
        valueNumber = localPointerKey.getValueNumber();
        parameter = localPointerKey.isParameter();
        this.persistentCGNode = persistentCGNode;
    }
    
    public int getValueNumber() {
        return valueNumber;
    }
    
    public PersistentCGNode getNode() {
        return persistentCGNode;
    }

    public boolean isParameter() {
        return parameter;
    }
}
