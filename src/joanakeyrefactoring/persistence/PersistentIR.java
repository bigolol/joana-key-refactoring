/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.persistence;

import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ssa.IR;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hklein
 */
public class PersistentIR {
    
    private Map<Integer, String> vnsToLocalNames = new HashMap<>();
    
    public PersistentIR(ArrayList<LocalPointerKey> localPointerKeys, IR ir) {
        for(LocalPointerKey localPointerKey : localPointerKeys) {
            vnsToLocalNames.put(localPointerKey.getValueNumber(), ir.getLocalNames(0, localPointerKey.getValueNumber())[0]);
        }
    }
    
    public String getLocalName(int vn) {
        return vnsToLocalNames.get(vn);
    }
}
