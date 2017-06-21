/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.customListener.simpleJavaModel;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author holger
 */
public class JavaScopeHandler {

    private List<JavaScope> currentScope = new ArrayList<>();

    public JavaScopeHandler() {
        currentScope.add(new JavaScope());
    }

    public String getTypeForVar(String varid) {
        for (int i = currentScope.size() - 1; i >= 0; --i) {
            String type = currentScope.get(i).getTypeForVar(varid);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    public void addVar(String id, String type) {

    }

    public void enterNewScope() {
        currentScope.add(new JavaScope());
    }

    public void exitScope() {
        currentScope.remove(currentScope.size() - 1);
    }
}
