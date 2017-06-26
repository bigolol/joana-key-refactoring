/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring.customListener.simpleJavaModel;

/**
 *
 * @author holger
 */
public class JavaMethodArgument {
    private String type;
    private String name;
    
    public JavaMethodArgument(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
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
        final JavaMethodArgument other = (JavaMethodArgument) obj;
        if (!this.type.equals(other.type)) {
            return false;
        }
        if (!this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "javaMethodParam[type=TYPE, name=NAME]".replace("TYPE", type).replace("NAME", name); 
    }
    
    
    

    
}
