/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.util.JavaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

    private Collection<? extends IViolation<SecurityNode>> uncheckedViolations;
    private SDG sdg;
    private Collection<ViolationChop> violationChops = new ArrayList<>();
    private HashSet<SDGEdge> checkedEdges = new HashSet<>();
    private HashSet<SDGEdge> edgesToCheck = new HashSet<>();
    private Map<SDGEdge, ArrayList<ViolationChop>> summaryEdgesAndContainingChops = new HashMap<>();
    private Map<SDGEdge, StaticCGJavaMethod> summaryEdgesAndCorresJavaMethods = new HashMap<>();
    private List<SDGEdge> sortedEdgesToCheck = new ArrayList<>();
    private JCallGraph callGraph = new JCallGraph();
    private IFCAnalysis ana;
    private StateSaver saver;
    private String pathToJavaSource;

    public ViolationsWrapper(Collection<? extends IViolation<SecurityNode>> violations,
            SDG sdg, AutomationHelper automationHelper,
            String pathToJar, IFCAnalysis ana, JCallGraph callGraph, StateSaver stateSaver, String pathToJavaSource) throws IOException {
        this.uncheckedViolations = violations;
        this.sdg = sdg;
        this.callGraph = callGraph;
        this.ana = ana;
        this.saver  = stateSaver;
        this.pathToJavaSource = pathToJavaSource;
        prepareNextSummaryEdges();
    }

    private void prepareNextSummaryEdges() {
        Collection<? extends IViolation<SecurityNode>> nextViolationsToHandle = getNextViolationsToHandle();
        nextViolationsToHandle.forEach((v) -> {
        	ViolationChop chop = createViolationChop(v, sdg, ana.getProgram());
        	
        	ViolationPath path = getViolationPath(v);
        	
        	
        	
//        	for(SDGMethod method : ana.getProgram().getAllMethods()){
//        		
//        		System.out.println("Method: \n"+method);
//        		for(SDGInstruction instr :  method.getInstructions()){
//        			System.out.println(instr);
//        		}
//        		
//        		
//        	}
        	
//       	System.out.println("Violation chop:" + v.getClass().getName());
//        	System.out.println("Source: "+chop.getViolationSource() + "-> Sink: "+chop.getViolationSink());
//        	
//        	for(SDGNode node : chop.getViolationChop()){
//        		System.out.println(node + " "+node.getBytecodeName()+" "+node.getBytecodeMethod());
//        	}
            violationChops.add(chop);
        });
        putEdgesAndChopsInMap();
        findCGMethodsForSummaryEdges(sdg, ana, callGraph);
        for (SDGEdge e : edgesToCheck) {
            if (summaryEdgesAndCorresJavaMethods.get(e) != null) {
                sortedEdgesToCheck.add(e);
            }
        }
        sortedEdgesToCheck.sort(new SummaryEdgeComparator(this));
    }

    private Collection<IViolation<SecurityNode>> getNextViolationsToHandle() {
        int amt_viols = 1;
        List<IViolation<SecurityNode>> created = new ArrayList<>();
        int i = 0;
        for (IViolation<SecurityNode> v : uncheckedViolations) {
            if (i == amt_viols) {
                break;
            }
            created.add(v);
            ++i;
        }
        for (IViolation<SecurityNode> v : created) {
            uncheckedViolations.remove(v);
        }
        return created;
    }

    private void findCGMethodsForSummaryEdges(SDG sdg1, IFCAnalysis ana, JCallGraph callGraph) {
        for (SDGEdge summaryEdge : edgesToCheck) {
            Collection<SDGNodeTuple> allFormalPairs = sdg1.getAllFormalPairs(summaryEdge.getSource(), summaryEdge.getTarget());
            SDGNodeTuple firstPair = allFormalPairs.iterator().next();
            SDGNode methodNode = sdg1.getEntry(firstPair.getFirstNode());
            String bytecodeMethod = methodNode.getBytecodeMethod();
            SDGMethod method = ana.getProgram().getMethod(bytecodeMethod);
            List<JavaType> argumentTypes = method.getSignature().getArgumentTypes();
            String types = "";
            for (JavaType currType : argumentTypes) {
                types += currType.toHRString() + ",";
            }
            if (!types.isEmpty()) {
                types = types.substring(0, types.length() - 1);
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
                if (edgesToCheck.contains(se)) {
                    summaryEdgesAndContainingChops.get(se).add(vc);
                } else {
                    edgesToCheck.add(se);
                    ArrayList<ViolationChop> arrayList = new ArrayList<>();
                    arrayList.add(vc);
                    summaryEdgesAndContainingChops.put(se, arrayList);
                }
            });
        });
    }

    private ViolationChop createViolationChop(IViolation<SecurityNode> violationNode, SDG sdg, SDGProgram program) {
        ViolationPath violationPath = getViolationPath(violationNode);
        LinkedList<SecurityNode> violationPathList = violationPath.getPathList();
        SDGNode violationSource = violationPathList.get(0);
        SDGNode violationSink = violationPathList.get(1);
        return new ViolationChop(violationSource, violationSink, sdg,program, saver, pathToJavaSource);
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
        edgesToCheck.remove(e);
        sortedEdgesToCheck.remove(e);
        summaryEdgesAndContainingChops.get(e).forEach((vc) -> {
            vc.findSummaryEdges(sdg);
            if (vc.isEmpty()) {
                violationChops.remove(vc);
            }
        });
        summaryEdgesAndContainingChops.remove(e);
        summaryEdgesAndCorresJavaMethods.remove(e);
        if (sortedEdgesToCheck.isEmpty()) {
            prepareNextSummaryEdges();
        }
    }

    public void checkedEdge(SDGEdge e) {
        checkedEdges.add(e);
        edgesToCheck.remove(e);
        sortedEdgesToCheck.remove(e);
        if (sortedEdgesToCheck.isEmpty()) {
            prepareNextSummaryEdges();
        }
    }

    public SDGEdge nextSummaryEdge() {
        return sortedEdgesToCheck.get(0);
    }

    ArrayList<ViolationChop> getChopsContaining(SDGEdge e) {
        return summaryEdgesAndContainingChops.get(e);
    }
}
