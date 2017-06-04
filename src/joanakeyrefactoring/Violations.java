/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author holgerklein
 */
public class Violations {

    private Collection<? extends IViolation<SecurityNode>> violations;
    private SDG sdg;
    private Collection<ViolationChop> violationChops;
    private ParseJavaForKeyListener javaForKeyListener;
    private ArrayList<String> keyFeatures = new ArrayList<String>();
    private Collection<ViolationChop> chopsContainingCurrentlyCheckedEdge = new ArrayList<>();
    private Collection<SDGEdge> checkedEdges = new ArrayList<>();
    private Map<SDGEdge, ArrayList<ViolationChop>> summaryEdgesAndContainingChops = new HashMap<>();

    public Violations(Collection<? extends IViolation<SecurityNode>> violations,
            SDG sdg, ParseJavaForKeyListener forKeyListener, AutomationHelper automationHelper) {
        loadAndAddListOfKeyFeatures(automationHelper);
        this.javaForKeyListener = forKeyListener;
        this.violations = violations;
        this.sdg = sdg;
        violations.forEach((v) -> {
            violationChops.add(createViolationChop(v, sdg));
        });       
        putEdgesAndChopsInMap();
    }

    private void putEdgesAndChopsInMap() {
        violationChops.forEach((vc) -> {
            vc.getSummaryEdges().forEach((se) -> {
                Collection<ViolationChop> chopList = summaryEdgesAndContainingChops.get(se);
                if (chopList == null) {
                    summaryEdgesAndContainingChops.put(se, new ArrayList<>());
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

    public void removeEdge(EdgeMetric em) {
        chopsContainingCurrentlyCheckedEdge.forEach((vc) -> {
            vc.removeEdge(em.edge);
            if (vc.isEmpty()) {
                violationChops.remove(vc);
            }
        });
        sdg.removeEdge(em.edge);
    }
    
    public void checkedEdge(SDGEdge e) {
        checkedEdges.add(e);
    }
}
