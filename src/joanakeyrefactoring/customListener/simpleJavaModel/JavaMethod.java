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
public class JavaMethod implements Comparable<JavaMethod> {

    private List<JavaMethodArgument> args = new ArrayList<>();
    private String name;
    private boolean isStatic;
    private List<JavaMethod> calledMethods = new ArrayList<>();
    private JavaClass containingClass;

    public JavaMethod(String name, boolean isStatic, JavaClass containingClass) {
        this.name = name;
        this.isStatic = isStatic;
        this.containingClass = containingClass;
    }

    public List<JavaMethod> getCalledMethods() {
        return calledMethods;
    }

    public JavaClass getContainingClass() {
        return containingClass;
    }   

    public List<JavaMethodArgument> getArgs() {
        return args;
    }

    public String getName() {
        return name;
    }

    public boolean isIsStatic() {
        return isStatic;
    }
    
    public void addArgument(JavaMethodArgument arg) {
        args.add(arg);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JavaMethod other = (JavaMethod) obj;
        JavaMethod oth = (JavaMethod) obj;
        
        if(!oth.name.equals(name)) {
            return false;
        }
        
        if(oth.args.size() != args.size()) {
            return false;
        }
        
        if(!containingClass.equals(oth.containingClass)) {
            return false;
        }
        
        for(int i = 0; i < args.size(); ++i) {
            if(!args.get(i).equals(oth.args.get(i))) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public int compareTo(JavaMethod o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public String toString() {
        String s = name;
        for(JavaMethodArgument arg : args) {
            s += arg.toString();
        }
        return containingClass.getName() + s;
    }

    
    
}
