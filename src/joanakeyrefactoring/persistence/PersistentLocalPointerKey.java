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
    
    public PersistentLocalPointerKey(LocalPointerKey localPointerKey) {
        valueNumber = localPointerKey.getValueNumber();
    }
    
    public int getValueNumber() {
        return valueNumber;
    }
}
