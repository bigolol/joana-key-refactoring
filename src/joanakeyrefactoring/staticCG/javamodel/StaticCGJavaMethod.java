/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.staticCG.javamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.antlr.v4.runtime.misc.OrderedHashSet;

/**
 *
 * @author holger
 */
public class StaticCGJavaMethod {

    private StaticCGJavaClass containingClass;
    private String id;
    private String parameterTypes;
    private OrderedHashSet<StaticCGJavaMethod> calledMethods = new OrderedHashSet<>();
    private boolean isStatic;

    public StaticCGJavaMethod(
            StaticCGJavaClass containingClass, String id,
            String parameterTypes, boolean isStatic) {
        this.containingClass = containingClass;
        this.id = id;
        this.parameterTypes = parameterTypes;
        this.isStatic = isStatic;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setIsStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public StaticCGJavaClass getContainingClass() {
        return containingClass;
    }

    public void setContainingClass(StaticCGJavaClass containingClass) {
        this.containingClass = containingClass;
    }

    public String getId() {
        return id;
    }

    public String getParameter() {
        return parameterTypes;
    }

    public void addCalledMethod(StaticCGJavaMethod m) {
        calledMethods.add(m);
    }

    public OrderedHashSet<StaticCGJavaMethod> getCalledMethods() {
        return calledMethods;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + Objects.hashCode(this.containingClass);
        hash = 19 * hash + Objects.hashCode(this.id);
        hash = 19 * hash + Objects.hashCode(this.parameterTypes);
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
        final StaticCGJavaMethod other = (StaticCGJavaMethod) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.parameterTypes, other.parameterTypes)) {
            return false;
        }
        if (!Objects.equals(this.containingClass, other.containingClass)) {
            return false;
        }
        return true;
    }

}
