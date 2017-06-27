/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package joanakeyrefactoring.staticCG;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaClass;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;
import org.antlr.v4.runtime.misc.OrderedHashSet;

import org.apache.bcel.classfile.ClassParser;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives
 * into a single call graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 *
 */
public class JCallGraph {
    private OrderedHashSet<StaticCGJavaClass> alreadyFoundClasses;
    private OrderedHashSet<StaticCGJavaMethod> alreadyFoundMethods;

    public OrderedHashSet<StaticCGJavaClass> getAlreadyFoundClasses() {
        return alreadyFoundClasses;
    }

    public OrderedHashSet<StaticCGJavaMethod> getAlreadyFoundMethods() {
        return alreadyFoundMethods;
    }
    
    public Set<StaticCGJavaMethod> getAllMethodsCalledByMethodRec(StaticCGJavaMethod m) {
        Set<StaticCGJavaMethod> created = new HashSet<>();
        addCalledToSetRec(m, created);
        return created;
    }
    
    private void addCalledToSetRec(StaticCGJavaMethod m, Set<StaticCGJavaMethod> created) {
        for(StaticCGJavaMethod called : m.getCalledMethods()) {
            if(!created.contains(called)) {
                created.add(called);
                addCalledToSetRec(called, created);
            }
        } 
    }
    
    public void generateCG(File jarFile) throws IOException {
        ClassParser cp;
        JarFile jar = new JarFile(jarFile);
        Enumeration<JarEntry> entries = jar.entries();
        alreadyFoundClasses = new OrderedHashSet<>();
        alreadyFoundMethods = new OrderedHashSet<>();       
        
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }

            if (!entry.getName().endsWith(".class")) {
                continue;
            }

            cp = new ClassParser(jarFile.getAbsolutePath(), entry.getName());
            ClassVisitor visitor = new ClassVisitor(cp.parse());
            visitor.start(alreadyFoundClasses, alreadyFoundMethods);             
        }
    }
}
