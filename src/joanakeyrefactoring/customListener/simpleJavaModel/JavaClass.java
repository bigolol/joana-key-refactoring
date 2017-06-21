/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.customListener.simpleJavaModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author holger
 */
public class JavaClass implements Comparable<JavaClass> {

    private String name;
    private String containingPackage;
    private List<JavaMethod> methods = new ArrayList<>();
    private List<JavaMethod> dependantMethods = new ArrayList<>();

    public JavaClass(String name, String containingPackage) {
        this.name = name;
        this.containingPackage = containingPackage;
    }

    public String getName() {
        return name;
    }

    public String getContainingPackage() {
        return containingPackage;
    }

    public List<JavaMethod> getMethods() {
        return methods;
    }

    public void addMethod(JavaMethod m) {
        methods.add(m);
    }
    
    public void addDependentMethod(JavaMethod m) {
        dependantMethods.add(m);
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
        final JavaClass other = (JavaClass) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.containingPackage, other.containingPackage)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(JavaClass o) {
        return (containingPackage + "." + name).compareTo(o.containingPackage + "." + name);
    }

}
