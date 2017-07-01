/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGMethod;
import joanakeyrefactoring.CustomListener.ParseJavaForKeyListener;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.util.JavaType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import joanakeyrefactoring.staticCG.JCallGraph;
import joanakeyrefactoring.staticCG.javamodel.StaticCGJavaMethod;

/**
 *
 * @author holgerklein
 */
public class ViolationsWrapper {

    private Collection<? extends IViolation<SecurityNode>> violations;
    private SDG sdg;
    private Collection<ViolationChop> violationChops = new ArrayList<>();
    private ParseJavaForKeyListener javaForKeyListener;
    private Collection<SDGEdge> checkedEdges = new ArrayList<>();
    private Map<SDGEdge, ArrayList<ViolationChop>> summaryEdgesAndContainingChops = new HashMap<>();
    private Map<SDGEdge, StaticCGJavaMethod> summaryEdgesAndCorresJavaMethods = new HashMap<>();
    private JCallGraph callGraph = new JCallGraph();
        

    public ViolationsWrapper(Collection<? extends IViolation<SecurityNode>> violations,
            SDG sdg, ParseJavaForKeyListener forKeyListener, AutomationHelper automationHelper,
            String pathToJar, IFCAnalysis ana, JCallGraph callGraph) throws IOException {
        this.javaForKeyListener = forKeyListener;
        this.violations = violations;
        this.sdg = sdg;
        this.callGraph = callGraph;

        violations.forEach((v) -> {
            violationChops.add(createViolationChop(v, sdg));
        });

        putEdgesAndChopsInMap();


        findCGMethodsForSummaryEdges(sdg, ana, callGraph);
    }

    private void findCGMethodsForSummaryEdges(SDG sdg1, IFCAnalysis ana, JCallGraph callGraph) {
        for (SDGEdge summaryEdge : summaryEdgesAndContainingChops.keySet()) {
            Collection<SDGNodeTuple> allFormalPairs = sdg1.getAllFormalPairs(summaryEdge.getSource(), summaryEdge.getTarget());
            SDGNodeTuple firstPair = allFormalPairs.iterator().next();
            SDGNode methodNode = sdg1.getEntry(firstPair.getFirstNode());
            String bytecodeMethod = methodNode.getBytecodeMethod();
            SDGMethod method = ana.getProgram().getMethod(bytecodeMethod);
            List<JavaType> argumentTypes = method.getSignature().getArgumentTypes();
            String types = "";
            for (JavaType currType : argumentTypes) {
                if (argumentTypes.indexOf(currType) != 0) {
                    types += ",";
                }
                types += currType.toHRString();
            }
            String methodName = method.getSignature().getMethodName();
            String fullyQualifiedMethodName = method.getSignature().getFullyQualifiedMethodName();
            int classNameEndIndex = fullyQualifiedMethodName.lastIndexOf(".");
            String className = fullyQualifiedMethodName.substring(0, classNameEndIndex);
            StaticCGJavaMethod callGraphMethod = callGraph.getMethodFor(className, methodName, types);
            summaryEdgesAndCorresJavaMethods.put(summaryEdge, callGraphMethod);
        }
    }

    public StaticCGJavaMethod getMethodCorresToSummaryEdge(SDGEdge e) {
        return summaryEdgesAndCorresJavaMethods.get(e);
    }
    
    private void putEdgesAndChopsInMap() {
        summaryEdgesAndContainingChops = new HashMap<>();
        violationChops.forEach((vc) -> {
            vc.getSummaryEdges().forEach((se) -> {
                Collection<ViolationChop> chopList = summaryEdgesAndContainingChops.get(se);
                if (chopList == null) {
                    ArrayList<ViolationChop> arrayList = new ArrayList<>();
                    arrayList.add(vc);
                    summaryEdgesAndContainingChops.put(se, arrayList);
                } else {
                    chopList.add(vc);
                }
            });
        });
    }

    private ViolationChop createViolationChop(IViolation<SecurityNode> violationNode, SDG sdg) {
        ViolationPath violationPath = getViolationPath(violationNode);
        LinkedList<SecurityNode> violationPathList = violationPath.getPathList();
        SDGNode violationSource = violationPathList.get(0);
        SDGNode violationSink = violationPathList.get(1);
        return new ViolationChop(violationSource, violationSink, sdg);
    }

    private ViolationPath getViolationPath(IViolation<SecurityNode> v) {
        return ((ClassifiedViolation) v).getChops().iterator().next().getViolationPathes().getPathesList().get(0);
    }

    public boolean allDisproved() {
        for (ViolationChop vc : violationChops) {
            if (!vc.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean allCheckedOrDisproved() {
        for (ViolationChop vc : violationChops) {
            if (!vc.isEmpty()) {
                for (SDGEdge se : vc.getSummaryEdges()) {
                    if (!checkedEdges.contains(se)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void removeEdge(SDGEdge e) {
        sdg.removeEdge(e);
        summaryEdgesAndContainingChops.get(e).forEach((vc) -> {
            vc.findSummaryEdges();
            if (vc.isEmpty()) {
                violationChops.remove(vc);
            }
        });
    }

    public void checkedEdge(SDGEdge e) {
        checkedEdges.add(e);
    }

    public SDGEdge nextSummaryEdge() {
        SDGEdge next = summaryEdgesAndContainingChops.keySet().iterator().next();
        return next;
    }
}
