/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.util.JavaType;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private List<String> keyCompatibleJavaFeatures = new ArrayList<>();

    public ViolationsWrapper(Collection<? extends IViolation<SecurityNode>> violations,
            SDG sdg, AutomationHelper automationHelper,
            IFCAnalysis ana, JCallGraph callGraph) throws IOException {
        this.uncheckedViolations = violations;
        this.sdg = sdg;
        this.callGraph = callGraph;
        this.ana = ana;

        InputStream is = new FileInputStream("dep/JAVALANG.txt");
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));
        String line = buf.readLine();
        while (line != null) {
            keyCompatibleJavaFeatures.add(line);
            line = buf.readLine();
        }
        buf.close();

        prepareNextSummaryEdges();
    }

    private void prepareNextSummaryEdges() {
        Collection<? extends IViolation<SecurityNode>> nextViolationsToHandle = getNextViolationsToHandle();
        nextViolationsToHandle.forEach((v) -> {
            violationChops.add(createViolationChop(v, sdg));
        });
        putEdgesInSet();
        findCGMethodsForSummaryEdgesIfKeyCompatible();
        putEdgesAndChopsInMap();

        sortedEdgesToCheck = new ArrayList<>();
        for (SDGEdge e : summaryEdgesAndCorresJavaMethods.keySet()) {
            sortedEdgesToCheck.add(e);
        }
        sortedEdgesToCheck.sort(new SummaryEdgeComparator(this));
    }

    private void putEdgesInSet() {
        for (ViolationChop vc : violationChops) {
            Collection<SDGEdge> summaryEdges = vc.getSummaryEdges();
            for (SDGEdge summaryEdge : summaryEdges) {
                edgesToCheck.add(summaryEdge);
            }
        }
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

    private void findCGMethodsForSummaryEdgesIfKeyCompatible() {
        for (SDGEdge summaryEdge : edgesToCheck) {
            Collection<SDGNodeTuple> allFormalPairs = sdg.getAllFormalPairs(summaryEdge.getSource(), summaryEdge.getTarget());
            SDGNodeTuple firstPair = allFormalPairs.iterator().next();
            SDGNode methodNode = sdg.getEntry(firstPair.getFirstNode());
            String bytecodeMethod = methodNode.getBytecodeMethod();
            SDGMethod method = ana.getProgram().getMethod(bytecodeMethod);
            List<JavaType> argumentTypes = method.getSignature().getArgumentTypes();
            String types = "";
            for (JavaType currType : argumentTypes) {
                String toHRString = currType.toHRString();
                int lastIndexOfDot = toHRString.lastIndexOf(".");
                toHRString = toHRString.substring(lastIndexOfDot + 1, toHRString.length());
                types += toHRString + ",";
            }
            if (!types.isEmpty()) {
                types = types.substring(0, types.length() - 1);
            }
            String methodName = method.getSignature().getMethodName();
            String fullyQualifiedMethodName = method.getSignature().getFullyQualifiedMethodName();
            int classNameEndIndex = fullyQualifiedMethodName.lastIndexOf(".");
            String className = fullyQualifiedMethodName.substring(0, classNameEndIndex);
            StaticCGJavaMethod callGraphMethod = callGraph.getMethodFor(className, methodName, types);
            if (isIndepOfLibs(callGraphMethod)) {
                summaryEdgesAndCorresJavaMethods.put(summaryEdge, callGraphMethod);
            }
        }
        edgesToCheck.clear();
    }

    public StaticCGJavaMethod getMethodCorresToSummaryEdge(SDGEdge e) {
        return summaryEdgesAndCorresJavaMethods.get(e);
    }

    private void putEdgesAndChopsInMap() {
        summaryEdgesAndContainingChops = new HashMap<>();
        for (ViolationChop vc : violationChops) {
            Collection<SDGEdge> summaryEdges = vc.getSummaryEdges();
            for (SDGEdge summaryEdge : summaryEdges) {
                if (summaryEdgesAndCorresJavaMethods.containsKey(summaryEdge)) {
                    if (summaryEdgesAndContainingChops.containsKey(summaryEdge)) {
                        summaryEdgesAndContainingChops.get(summaryEdge).add(vc);
                    } else {
                        ArrayList<ViolationChop> vcList = new ArrayList<>();
                        vcList.add(vc);
                        summaryEdgesAndContainingChops.put(summaryEdge, vcList);
                    }
                }
            }
        }
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

    private boolean isIndepOfLibs(StaticCGJavaMethod m) {
        String packageName = callGraph.getPackageName();
        try {
            String classPackageName = m.getContainingClass().getPackageString();
            if (!classPackageName.startsWith(packageName)) {
                return false;
            }
            Set<StaticCGJavaMethod> allMethodsCalledByMethodRec = m.getCalledFunctionsRec();
            for (StaticCGJavaMethod calledM : allMethodsCalledByMethodRec) {
                if (!calledM.getContainingClass().getPackageString().startsWith(packageName)) {
                    if (!isKeyFeature(calledM)) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean isKeyFeature(StaticCGJavaMethod calledM) {
        String onlyClassName = calledM.getContainingClass().getOnlyClassName();
        for (String compatible : keyCompatibleJavaFeatures) {
            if (compatible.endsWith(onlyClassName)) {
                return true;
            }
        }
        return false;
    }
}
