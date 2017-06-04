/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasicSumEdgeComparator implements SummaryEdgeComparator {

    private ArrayList<String> keyFeatures = new ArrayList<String>();
    private SDG sdg;
    private Map<SDGEdge, Collection<ViolationChop>> edgesToChops;
    private ParseJavaForKeyListener javaForKeyListener;

    public BasicSumEdgeComparator(AutomationHelper automationHelper, SDG sdg, 
            Map<SDGEdge, Collection<ViolationChop>> edgesToChops, ParseJavaForKeyListener javaForKeyListener) {
        this.sdg = sdg;
        this.edgesToChops = edgesToChops;
        this.javaForKeyListener = javaForKeyListener;
        loadAndAddListOfKeyFeatures(automationHelper);
    }
    
    private List<EdgeMetric> getSummaryEdges(SDG flowSDG) {
        List<EdgeMetric> summaryEdges = new ArrayList<EdgeMetric>();
        //clone() is needed because removing/adding edges to check for bridges throws the iterator off
        Collection<SDGEdge> clonedEdgesFromflowSDG = flowSDG.clone().edgeSet();
        for (SDGEdge currentEdge : clonedEdgesFromflowSDG) {
            if (isSummaryEdge(currentEdge) && !checkedEdges.contains(currentEdge)) {
                SDGNode callee = sdg.getEntry(currentEdge.getSource());
                String calleeByteCodeMethod = callee.getBytecodeMethod();
                Boolean isPartOfJavaLibrary = false;
                if (calleeByteCodeMethod.contains("java.") || calleeByteCodeMethod.contains(".lang.")) {
                    isPartOfJavaLibrary = true;
                }
                String methodName = getMethodNameFromBytecode(calleeByteCodeMethod);
                boolean isKeYCompatible = isKeyCompatible(calleeByteCodeMethod);

                boolean machtesPattern = isKeYCompatible;
                boolean isBridge = false;
                int containedSummary = 0;
                if (neueHeuristic) {
                    summaryEdges.add(new EdgeMetric(currentEdge, machtesPattern,
                            isBridge, containedSummary));
                } else {
                    summaryEdges.add(new EdgeMetric(currentEdge, isBridge,
                            containedSummary));
                }
            }
        }
        Collections.sort(summaryEdges);
        return summaryEdges;
    }

    private String getMethodNameFromBytecode(String byteCodeMethod) {
        String[] a2 = byteCodeMethod.split("\\.");
        String[] a3 = a2[a2.length - 1].split("\\(");
        String methodName = a3[0];
        if (byteCodeMethod.contains("<init>")) {
            methodName += "." + a2[a2.length - 2].split("\\(")[0];
        }
        return methodName;
    }

    private boolean isKeyCompatible(String byteCodeMethodName) {
        if (isJavaLibrary(byteCodeMethodName)) {
            return false;
        }
        String methodName = getMethodNameFromBytecode(byteCodeMethodName);

        if (methodName.contains("<init>.")) {
            methodName = methodName.split("\\.")[1];
        }
        List<String> methodFeatures = javaForKeyListener.getCreatedNames(methodName);
        if (methodFeatures == null) {
            methodFeatures = new ArrayList<String>();
        }

        methodFeatures.add(methodName);
        return keyFeatures.containsAll(methodFeatures);
    }

    private Boolean isJavaLibrary(String calledMethodByteCode) {
        if (calledMethodByteCode.contains("java.") || calledMethodByteCode.contains("lang")) {
            return true;
        }
        return false;
    }

    private void loadAndAddListOfKeyFeatures(AutomationHelper automationHelper) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                    "dep/JAVALANG.txt"));
            String line = br.readLine();
            String[] entrys;
            String entry;
            while (line != null) {
                entrys = line.split("\\.");
                entry = entrys[entrys.length - 1].trim();
                keyFeatures.add(entry);
                line = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        keyFeatures.addAll(automationHelper.getClassNames());
        Set<String> methods = javaForKeyListener.getMethods();
        keyFeatures.addAll(methods);
    }

    @Override
    public int compare(SDGEdge o1, SDGEdge o2) {
        if (machtesPattern && !other.machtesPattern) {
            return -1;
        }
        if (containedEdges != other.containedEdges) {
            return containedEdges - other.containedEdges;
        }
        if (isBridge && !other.isBridge) {
            return -1;
        }
        if (!isBridge && other.isBridge) {
            return 1;
        }
        if (!machtesPattern && other.machtesPattern) {
            return 1;
        }

        /**
         * TODO: i would like to compare the edges themselves here, but the
         * comparator for edges already built into JOANA is not public. And i
         * don't want to copy and paste the code to use it here.
         */
        return 0;
    }

}
