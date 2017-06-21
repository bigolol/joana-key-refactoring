/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.customListener.simpleJavaModel;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author holger
 */
public class JavaScope {
    private Map<String, String> idsToType = new HashMap<>();
    
    public void addVar(String id, String type) {
        idsToType.put(id, type);
    }
    
    public String getTypeForVar(String id) {
        if(idsToType.containsKey(id)) {
            return idsToType.get(id);
        } else {
            return null;
        }
    }
}
