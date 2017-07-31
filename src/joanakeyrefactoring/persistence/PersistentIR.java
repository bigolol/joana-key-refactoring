/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.persistence;

import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ssa.IR;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author hklein
 */
public class PersistentIR {

    private Map<Integer, String> vnsToLocalNames = new HashMap<>();

    public PersistentIR(List<LocalPointerKey> localPointerKeys, IR ir) {
        for (LocalPointerKey localPointerKey : localPointerKeys) {
            String[] localNames = ir.getLocalNames(0, localPointerKey.getValueNumber());
            if (localNames != null && localNames.length != 0) {
                vnsToLocalNames.put(localPointerKey.getValueNumber(), localNames[0]);
            }
        }
    }
    
    public String generateSaveString() {
        StringBuilder sb = new StringBuilder();
        vnsToLocalNames.forEach((i, s) -> {
            sb.append(i.toString()).append(" -> ").append(s).append('\n');
        });
        return sb.toString();
    }

    public String getLocalName(int vn) {
        return vnsToLocalNames.get(vn);
    }
}
