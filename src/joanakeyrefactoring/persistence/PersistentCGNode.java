/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.persistence;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import java.util.List;

/**
 *
 * @author hklein
 */
public class PersistentCGNode {
    private PersistentIR persistentIR;
    
    
    public PersistentCGNode() {
    }
    
    public void createPersistentIR(CGNode n, List<LocalPointerKey> localPointerKeys) {
        persistentIR = new PersistentIR(localPointerKeys, n.getIR());
    }
        
    public PersistentIR getIR() {
        return persistentIR;
    }
}
