/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joanakeyrefactoring;

import edu.kit.joana.api.sdg.SDGInstruction;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGPhi;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Operation;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.ICFGChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.IntraproceduralChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.Opt1Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.SummaryMergedChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import joanakeyrefactoring.staticCG.JCallGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.Decoder;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;

/**
 *
 * @author holgerklein
 */
public class ViolationChop {

	public final static int BOTH_CASES = 1;

	public final static int TRUE_CASE  = 2;

	public final static int FALSE_CASE = 3;

	private final SDGNode violationSource;
	private final SDGNode violationSink;
	private Collection<SDGNode> violationChop;
	private SDG inducedSubgraph;
	private Chopper chopper;
	private Collection<SDGEdge> summaryEdges = new ArrayList<>();
	private Set<Integer> visitedNodes = new HashSet<>();
	private SDGProgram program;
	private StateSaver saver;
	private String pathToJavaSource;
	private Map<String,IMethod> methods;

	public ViolationChop(SDGNode violationSource, SDGNode violationSink, SDG sdg, SDGProgram program, StateSaver saver, String pathToJavaSource) {
		this.violationSource = violationSource;
		this.violationSink = violationSink;
		this.program = program;
		this.saver = saver;
		this.pathToJavaSource = pathToJavaSource;
		findSummaryEdges(sdg);	
		methods = getAllMethods();
		printProgram();

		//printCallGraph();

		//getChopNodes(sdg);
		//printChop(sdg);

		createSlice(sdg);


	}

	private void printCallGraph() {
		CallGraph cg = saver.callGraph;
		System.out.println("CG class: "+cg.getClass().getName());

		CGNode root = cg.getFakeRootNode();

		for(CGNode cgNode : cg){
			IMethod method = cgNode.getMethod();



			IR ir = cgNode.getIR();

			System.out.println("\nMethod: "+method.getSignature());



			Iterator<SSAInstruction> it = ir.iterateAllInstructions();

			while(it.hasNext()){
				SSAInstruction instr =it.next();

				System.out.println(instr.iindex + ":"+ instr.toString(ir.getSymbolTable()));


			}


		}

		//		System.out.println("\n\n\n");
		//		IClassHierarchy classHierarchy = cg.getClassHierarchy();
		//		for(IClass cl : classHierarchy){
		//			
		//			
		//			if(cl.getClassLoader().getReference().getName().toString().equals("Primordial")){
		//				continue;
		//			}
		//			System.out.println("Class: "+cl+" "+cl.getClassLoader().getName());
		//			for(IMethod method : cl.getAllMethods()){
		//				
		//				
		//				if(method.getDeclaringClass().getClassLoader().getReference().getName().toString().equals("Primordial")){
		//					continue;
		//				}
		//				
		//				System.out.println("Method: "+method.getSignature());
		//				
		//				
		//				
		//			}
		//		}	
	}

	public Map<String,IMethod> getAllMethods(){

		Map<String, IMethod> result = new HashMap<>();

		CallGraph cg = saver.callGraph;

		IClassHierarchy classHierarchy = cg.getClassHierarchy();
		for(IClass cl : classHierarchy){


			if(cl.getClassLoader().getReference().getName().toString().equals("Primordial")){
				continue;
			}

			for(IMethod method : cl.getAllMethods()){


				if(method.getDeclaringClass().getClassLoader().getReference().getName().toString().equals("Primordial")){
					continue;
				}


				result.put(method.getSignature(), method);


			}
		}

		return result;


	}

	public SDGNode getViolationSource() {
		return violationSource;
	}

	public SDGNode getViolationSink() {
		return violationSink;
	}

	public boolean isEmpty() {
		return violationChop.isEmpty();
	}



	public Collection<SDGNode> getViolationChop() {
		return violationChop;
	}

	public Collection<SDGEdge> getSummaryEdges() {
		return summaryEdges;
	}

	public void findSummaryEdges(SDG sdg) {
		this.chopper = new RepsRosayChopper(sdg);
		violationChop = chopper.chop(violationSource, violationSink);
		if (violationChop.isEmpty()) {
			return;
		}
		inducedSubgraph = sdg.subgraph(violationChop);

		inducedSubgraph.edgeSet().forEach((e) -> {
			if (isSummaryEdge(e)) {
				summaryEdges.add(e);
			}
		});
	}

	private Set<SDGNode> next(SDGNode node, SDG sdg){
		Set<SDGNode> nodes = new HashSet<>();

		for(SDGEdge edge : sdg.outgoingEdgesOf(node)){
			if( true){
				//printEdge(edge);
				SDGNode target  =sdg.getEdgeTarget(edge);
				nodes.add(target);
			}

		}

		return nodes;
	}

	public Collection<SDGInstruction> instructionOfNode(SDGNode node){

		Collection<SDGInstruction> instr  =new LinkedList<>();
		try{
			JavaMethodSignature sig = JavaMethodSignature.fromString(node.getBytecodeMethod());
			instr = program.getInstruction(sig, node.getBytecodeIndex());
		}catch(Exception e){}



		return instr;


	}

	Set<List<SDGNode>> allPaths = new HashSet<>();
	private void computePaths(Set<List<SDGNode>> allPaths,List<SDGNode> currentPath, SDGNode start, SDG sdg){
		if(!isIgnored(start)){			
			currentPath.add(start);			
		}    	
		allPaths.add(currentPath);
		for(SDGEdge edge : sdg.outgoingEdgesOf(start)){
			SDGNode child = edge.getTarget();
			if(currentPath.contains(child)){
				continue;
			}
			allPaths.remove(currentPath);
			List<SDGNode> newPath = new LinkedList<>(currentPath);

			computePaths(allPaths, newPath, child, sdg);    		
		}
	}

	public Set<SDGNode> getPHINodes(){
		Set<SDGNode> nodes = new HashSet<>();

		for(List<SDGNode> path : allPaths){
			for(SDGNode node : path){
				if(node.getLabel().contains("PHI")){
					nodes.add(node);
				}
			}
		}

		return nodes;
	}


	public Set<SDGNode> getNodesOfKind(SDG sdg, Kind kind){
		Set<SDGNode> nodes = new HashSet<>();    	
		for(SDGNode node : sdg.vertexSet()){    		
			if(node.getKind().equals(kind)){
				nodes.add(node);
			}    		
		}    	
		return nodes;
	}

	public void printProgram(){
		System.out.println("Program: \n");



		for(SDGMethod method: program.getAllMethods()){
			System.out.println("\nMethod: ");

			System.out.println(method.getSignature());

			for(SDGInstruction instr : method.getInstructions()){
				System.out.println(instr);				
			}

			for(SDGPhi phi : method.getPhis()){
				System.out.println(phi);
			}


		}


	}

	public Set<File> getJavaFiles( String path) {

		File root = new File( path );
		File[] list = root.listFiles();

		if (list == null){
			return new HashSet<File>();
		}
		Set<File> result = new HashSet<>();
		for ( File f : list ) {
			if ( f.isDirectory() ) {
				result.addAll(getJavaFiles( f.getAbsolutePath() ));                
			}
			else {
				result.add(f);
			}
		}

		return result;
	}


	public void createSlice(SDG sdg){

		

		Set<SDGNode> chopNodes = getChopNodes(sdg);
        reconstructChop(chopNodes, sdg);
		
		
		Map<String,Set<Integer>> chopLineNumbers = computeSourceLineNumbers(chopNodes);

		System.out.println("Chop:\n"+chopLineNumbers);

		Map<String,Set<Integer>> sdgLineNumbers = computeSourceLineNumbers(sdg.vertexSet());

		System.out.println("SDG:\n"+sdgLineNumbers);
		//Map<String, Map<Integer, Integer>> predicateConditions = getConditionsForPreds(chopNodes, sdg);
		for(String sourceFile : chopLineNumbers.keySet()){
			List<String> originalLines = readLinesOfFile(sourceFile);
			Set<Integer> chopLines = chopLineNumbers.get(sourceFile);
			Set<Integer> sdgLines = sdgLineNumbers.get(sourceFile);
            //Map<Integer,Integer> predCond = predicateConditions.get(sourceFile);

			for(int i = 0; i < originalLines.size(); i++){
				String line = originalLines.get(i);
				int lineNumber = i + 1;
				if((!chopLines.contains(lineNumber)) && sdgLines.contains(lineNumber)){
					System.out.println("//sliced:"+line);
				}
				else{
					
//					if(predCond != null && predCond.containsKey(lineNumber)){
//						
//						int cond = predCond.get(lineNumber);
//						
//						if(cond == TRUE_CASE){
//							String assString = "//Condition must be true";
//							line = assString + "\n"+line;
//						}
//						else if(cond == FALSE_CASE){
//							String assString = "//Condition must be false";
//							line = assString + "\n"+line;
//						}
//						
//						
//					}
					
					
					System.out.println(line);
				}

			}





		}
	}



	private List<String> readLinesOfFile(String path) {
		List<String> originalLines = new LinkedList<>();
		try{
			File javaFile = new File(pathToJavaSource+File.separator+path);			
			BufferedReader br = new BufferedReader(new FileReader(javaFile));
			String origLine = null;				
			while((origLine = br.readLine())!=null){					
				originalLines.add(origLine);
			}
			br.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return originalLines;
	}


	private static final String ASSERT_METHOD = "Assertions.assert";


	public Map<String,Set<Integer>> computeSourceLineNumbers(Set<SDGNode> nodes){

		Map<String,Set<Integer>> result = new HashMap<>();
		

		for(SDGNode node : nodes){
			if(node.getLabel().startsWith("goto")){
				continue;
			}
			
			
			
			int lineNumber = computeLineNumber(node);
			if(lineNumber >=0){
				String source = node.getSource();

				if(!result.containsKey(source)){
					result.put(source, new HashSet<Integer>());
				}
				Set<Integer> lineNumbers = result.get(source);
				lineNumbers.add(lineNumber);
				
			}
			
			



		}


		return result;

	}

	private int computeLineNumber(SDGNode node){
		int bcIndex = node.getBytecodeIndex();
		if(bcIndex>=0){
			IMethod method = methods.get(node.getBytecodeMethod());
            if(method != null){
            	return method.getLineNumber(bcIndex); 
            }
		}
		return -1;
	}



	private void printSlice(String javaFile, Set<Integer> lines) throws FileNotFoundException, IOException {
		List<Integer> lst = new ArrayList<Integer>(lines);		
		Collections.sort(lst);

		File jFile = new File(javaFile);
		BufferedReader br = new BufferedReader(new FileReader(jFile));
		List<String> origLines = new ArrayList<String>();
		String origLine = null;
		System.out.println("==============\nOriginal code: ");
		while((origLine = br.readLine())!=null){
			System.out.println(origLine);
			origLines.add(origLine.trim());
		}
		br.close();
		System.out.println("\n\nSlice:\n---------- ");
		for(int i : lst){
			if(i<origLines.size()-1){
				System.out.println(origLines.get(i-1));//line numbers start with 1!
			}

		}
	}
	
	private boolean isIsolatedNode(SDGNode node,Collection<SDGNode> nodes, SDG sdg){
		boolean hasIncoming = false;
		for(SDGEdge edge : sdg.incomingEdgesOf(node)){
			if(nodes.contains(edge.getSource())){
				hasIncoming = true;
			}
		}
		boolean hasOutgoing = false;
		for(SDGEdge edge : sdg.outgoingEdgesOf(node)){
			if(nodes.contains(edge.getTarget())){
				hasOutgoing = true;
			}
		}
		
		
		return !hasOutgoing || !hasIncoming ;
	}
	
	private void reconstructChop(Collection<SDGNode> violationChop2, SDG sdg){
		
		
		inducedSubgraph = sdg.subgraph(violationChop2);
		
		
		//Compute paths of the chop
		allPaths = new HashSet<>();
		computePaths(allPaths, new LinkedList<>(), violationSource, inducedSubgraph);
		//add method call nodes to paths
		for(List<SDGNode> path : allPaths){
			addMethodCallsToPath(path, sdg);
		}
		
		
		
		
	}


	public Set<SDGNode> getChopNodes(SDG sdg){
		this.chopper = new RepsRosayChopper(sdg);
		//this.chopper = new Opt1Chopper(sdg);
		violationChop = chopper.chop(violationSource, violationSink);
		if (violationChop.isEmpty()) {
			return new HashSet<>();
		}
		reconstructChop(violationChop, sdg);

		//Map<String,IMethod> methods = getAllMethods();

		Set<SDGNode> chopNodes = new HashSet<>();
		chopNodes.addAll(inducedSubgraph.vertexSet());
		//add all relevant method calls
		Set<SDGNode> calls = getRelevantCalls(chopNodes, sdg);
		chopNodes.addAll(calls);
		
		
		reconstructChop(chopNodes, sdg);
		
		//add all relevant predicates
		Set<SDGNode> predicates = getRelevantPreds(chopNodes, sdg);
		chopNodes.addAll(predicates);
		Set<SDGNode> predicatesSlice = computeSliceForPredicates(chopNodes, sdg);
		chopNodes.addAll(predicatesSlice);
		
		reconstructChop(chopNodes, sdg);
		return chopNodes;


		//		Map<SDGNode, Integer> conditions = getConditionsForPreds(chopNodes, sdg);
		//		
		//		System.out.println("\n\n\nSlice:");
		//		for(SDGNode node : chopNodes){
		//			
		//			int bcIndex = node.getBytecodeIndex();
		//			if(bcIndex >= 0){
		//				
		//				IMethod method = methods.get(node.getBytecodeMethod());
		//				System.out.println("\n"+bcIndex+" "+node.getLabel());
		//				if(method != null){	
		//					
		//					System.out.println("Src: "+node.getSource()+" Line: "+method.getLineNumber(bcIndex)+" "+method.getName());
		//					if(conditions.containsKey(node)){
		//						
		//						int cond = conditions.get(node);
		//						
		//						switch(cond){
		//						
		//						case TRUE_CASE: System.out.println("Only the true case");break;
		//						case FALSE_CASE: System.out.println("Only the false case");break;
		//						case BOTH_CASES: System.out.println("Both cases");break;
		//						
		//						}
		//						
		//					}
		//				}
		//				
		//				
		//				
		//			}
		//			
		//			
		//		}		
	}

	public Set<SDGNode> computeSliceForPredicates(Set<SDGNode> chopNodes, SDG sdg){

		Set<SDGNode> result = new HashSet<>();

		Slicer bwSlicer = new IntraproceduralSlicerBackward(sdg);
		Set<SDGNode> predicates = filterNodesOfKind(chopNodes, Kind.PREDICATE);

		result.addAll(bwSlicer.slice(predicates));


		return result;


	}

	public Set<SDGNode> filterNodesOfKind(Set<SDGNode> nodes, Kind kind){

		Set<SDGNode> result = new HashSet<>();

		for(SDGNode node : nodes){
			if(node.getKind().equals(kind)){
				result.add(node);
			}
		}

		return result;


	}

	public Map<String, Map<Integer, Integer>> getConditionsForPreds(Set<SDGNode> predicates, SDG sdg){

		Map<String, Map<Integer, Integer>> result = new HashMap<>();

		for(SDGNode pred : predicates){
			int res = getPrediateConditions(pred, sdg);
			if(res != -1){				
				String src = pred.getSource();				
				if(!result.containsKey(src)){
					Map<Integer,Integer> srcLineConds = new HashMap<>();
					result.put(src, srcLineConds);
				}
				Map<Integer,Integer> lineconds = result.get(src);				
				int line = computeLineNumber(pred);
				lineconds.put(line, res);
			}
		}

		return result;

	}

	public boolean isLoopCondition(SDGNode node){

		if(!node.getKind().equals(Kind.PREDICATE)){
			return false;
		}

		String gotoTarget = node.getLabel().substring(node.getLabel().lastIndexOf(" ")+1);
		int targetNumber  =Integer.parseInt(gotoTarget);
		if(node.getBytecodeIndex() > targetNumber){
			return true;
		}
		else{
			return false;
		}


	}

	public int getPrediateConditions(SDGNode predicate, SDG sdg){
		if(!predicate.getKind().equals(Kind.PREDICATE)){
			return -1;
		}
		if(isLoopCondition(predicate)){
			return -1;
		}
		if(!sdg.containsVertex(predicate)){
			return -1;
		}
		
		
		
		
		Set<List<SDGNode>> relevantPaths = new HashSet<>();
		System.out.println("Compute paths for: "+predicate.getLabel());
		computePaths(relevantPaths, new LinkedList<>(), predicate, sdg);
		
		
		
		SDGNode target = gotoTarget(predicate,sdg);
		if(target == null || relevantPaths.size() == 0){								
			return -1;
		}
		
		System.out.println("Checking: "+predicate.getLabel());
		
		int pathsContainingTarget = 0;
		for(List<SDGNode> path : relevantPaths){
			if(path.contains(target)){
				pathsContainingTarget++;
			}
		}
		
		System.out.println(" Paths: "+relevantPaths.size()+" Containing goto: "+pathsContainingTarget);
		if(pathsContainingTarget == relevantPaths.size()){
			return FALSE_CASE;
		}
		else if(pathsContainingTarget == 0){
			return TRUE_CASE;
		}
		else{
			return BOTH_CASES;
		}
	}


	public Set<List<SDGNode>> relevantPathsForPredicate(SDGNode predicate, SDG sdg){

		Set<List<SDGNode>> result = new HashSet<>();

		Set<SDGNode> controlDeps = getCDSuccessors(predicate, sdg);
		for(List<SDGNode> path : allPaths){

			for(SDGNode succ : controlDeps){

				if(path.contains(succ)){
					result.add(path);
					System.out.println(succ.getLabel()+" is successor of "+predicate.getLabel());
					break;
				}

			}

		}
		return result;

	}

	public SDGNode gotoTarget(SDGNode predicate, SDG sdg){
		Set<SDGNode> successors = getCDSuccessors(predicate, sdg);
		for(SDGNode node : successors){			
			if(predicate.getLabel().endsWith(" "+node.getBytecodeIndex())){
				return node;
			}			
		}		
		return null;
	}

	public Set<SDGNode> getCDSuccessors(SDGNode predicate, SDG sdg){
		Set<SDGNode> result  = new HashSet<>();

		for(SDGEdge edge : sdg.outgoingEdgesOf(predicate)){
			if(edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_COND)){
				
				SDGNode target = edge.getTarget();
				
				if(target.getBytecodeIndex() >= 0){
					result.add(target);
				}
				
				
			}
		}

		return result;


	}



	public void printChop(SDG sdg) {
		this.chopper = new RepsRosayChopper(sdg);
		//this.chopper = new Opt1Chopper(sdg);
		violationChop = chopper.chop(violationSource, violationSink);
		if (violationChop.isEmpty()) {
			return;
		}
		inducedSubgraph = sdg.subgraph(violationChop);

        allPaths = new HashSet<>();
		computePaths(allPaths,new LinkedList<>(), violationSource, inducedSubgraph);
		//removeUnnecessaryPaths();
		//removeUnnecessaryLongPaths();

		Set<SDGNode> predNodes = getNodesOfKind(sdg, Kind.PREDICATE);

		int pathNo  = 1;
		for(List<SDGNode> path : allPaths){
			path = addMethodCallsToPath(path, sdg);
			System.out.println("\n\nPath "+pathNo+":");
			pathNo++;
			for(SDGNode node : path){

				if(node.getBytecodeIndex() < 0){
					//continue;
				}

				System.out.println(node+" " + node.getLabel()+ " "+node.getKind()+ " "+node.getBytecodeIndex());

			}

			//System.out.println("\n\nRElevant preds: ");

			//identifyConditions(path, sdg);

			//			for(SDGNode node : getRelevantIfsforPath(predNodes, path, sdg)){
			//				
			//				System.out.println(node+" " + node.getLabel()+ " "+node.getKind()+" "+node.getBytecodeIndex());
			//				
			//				
			//				//printSSAMethod(sdg, node);
			//				
			//                
			//				
			//			}


		}





	}

	private void printSSAMethod(SDG sdg, SDGNode node) {
		int cgId = sdg.getCGNodeId(sdg.getEntry(node));
		if(cgId == SDG.UNDEFINED_CGNODEID){
			return;
		}
		CGNode cgNode = saver.callGraph.getNode(cgId);
		int ssaInstrIndex = sdg.getInstructionIndex(node);
		SSAInstruction[] instructions = cgNode.getIR().getInstructions();

		if(0 <= ssaInstrIndex && ssaInstrIndex < instructions.length){
			System.out.println(node+" " + node.getLabel());
			System.out.println("Instruction: "+instructions[ssaInstrIndex]);

		}
	}


	public Set<SDGNode> getRelevantIfsforPath(Set<SDGNode> predNodes, List<SDGNode> path, SDG sdg){
		Set<SDGNode> result = new HashSet<>();

		for(SDGNode pred : predNodes){
			for(SDGNode predChild : next(pred,sdg)){

				if(path.contains(predChild)){
					//System.out.println("Added because of: "+predChild);

					result.add(pred);

				}

			}
		}


		return result;
	}

	public void printChildrenOfNode(SDGNode node, SDG sdg){

		for(SDGEdge edge : sdg.outgoingEdgesOf(node)){

			System.out.println("EDGE: "+edge+" "+edge.getKind());

			SDGNode child = edge.getTarget();

			System.out.println(child+" " + child.getLabel()+ " "+child.getKind());


		}



	}

	public Set<SDGNode> getRelevantCalls(Set<SDGNode> chopNodes, SDG sdg){

		Set<SDGNode> result = new HashSet<>();
		Set<SDGNode> ignoredActualIns  =new HashSet<>();

		for(SDGNode node : chopNodes){

			if(node.getKind().equals(Kind.ACTUAL_IN)&& !ignoredActualIns.contains(node)){
				for(SDGNode call : getMethodCallsSuccessors(node, sdg)){
					result.add(call);
					ignoredActualIns.addAll(getActualInPredecessors(call, sdg));
				}
			}

		}


		return result;

	}

	public List<SDGNode> addMethodCallsToPath(List<SDGNode> path, SDG sdg){

		List<SDGNode> newPath = new LinkedList<>();
		Set<SDGNode> ignoredActualIns  =new HashSet<>();

		for(SDGNode node : path){
			if(node.getKind().equals(Kind.ACTUAL_IN) && !ignoredActualIns.contains(node)){
				for(SDGNode call : getMethodCallsSuccessors(node, sdg)){
					newPath.add(call);
					ignoredActualIns.addAll(getActualInPredecessors(call, sdg));
				}
			}
			else if(!node.getKind().equals(Kind.ACTUAL_OUT)){
				newPath.add(node);
			}


		}

		return newPath;



	}
	public Set<SDGNode> getActualInPredecessors(SDGNode node, SDG sdg){
		Set<SDGNode> result = new HashSet<>();

		for(SDGEdge edge : sdg.incomingEdgesOf(node)){

			SDGNode pred = edge.getSource();

			if(pred.getKind().equals(Kind.ACTUAL_IN)){
				result.add(pred);
			}

		}

		return result;
	}
	public Set<SDGNode> getMethodCallsSuccessors(SDGNode node, SDG sdg){
		Set<SDGNode> result = new HashSet<>();
		for(SDGEdge edge : sdg.outgoingEdgesOf(node)){

			SDGNode succ = edge.getTarget();
			if(succ.getKind().equals(Kind.CALL)){
				result.add(succ);
			}


		}

		return result;


	}

	public boolean isInChop(SDGNode node){

		for(List<SDGNode> path : allPaths){
			if(path.contains(node)){
				return true;
			}
		}

		return false;
	}

	public void removeUnnecessaryLongPaths(){

		Set<List<SDGNode>> unnecessaryPaths  =new HashSet<>();

		for(List<SDGNode> path1 : allPaths){

			for(List<SDGNode> path2 : allPaths){

				if(path1 != path2){


					List<SDGNode> shoterPath = path1.size() < path2.size() ? path1 :path2;
					List<SDGNode> longerPath = path1.size() < path2.size() ? path2 :path1;

					boolean necessary = false;
					for(SDGNode node : shoterPath){

						if(!longerPath.contains(node)){
							necessary = true;
						}

					}
					if(!necessary){
						unnecessaryPaths.add(longerPath);
					}




				}



			}

		}

		allPaths.removeAll(unnecessaryPaths);


	}



	public void removeUnnecessaryPaths(){

		Set<List<SDGNode>> unnecessaryPaths  =new HashSet<>();

		for(List<SDGNode> path1 : allPaths){

			for(List<SDGNode> path2 : allPaths){

				if(path1 != path2){


					List<SDGNode> shoterPath = path1.size() < path2.size() ? path1 :path2;
					List<SDGNode> longerPath = path1.size() < path2.size() ? path2 :path1;

					boolean necessary = false;
					for(SDGNode node : shoterPath){

						if(!longerPath.contains(node)){
							necessary = true;
						}

					}
					if(!necessary){
						unnecessaryPaths.add(shoterPath);
					}




				}



			}

		}

		allPaths.removeAll(unnecessaryPaths);


	}

	public void printAllNodes(SDG sdg){
		for(SDGNode node : sdg.vertexSet()){

			System.out.println(node+" " + node.getLabel()+ " "+node.getKind());

		}
	}

	public Set<SDGNode> predNodes(SDG sdg){
		Set<SDGNode> result = new HashSet<>();

		for(SDGNode node : sdg.vertexSet()){
			if(node.getKind().equals(Kind.PREDICATE)){
				result.add(node);
			}
		}

		return result;
	}

	public void identifyConditions(List<SDGNode> path, SDG sdg){

		for(SDGNode node : path){

			if(node.getBytecodeIndex() < 0){
				continue;
			}

			for(SDGEdge edge : sdg.incomingEdgesOf(node)){
				boolean cond  = false;
				if(edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_COND)){

					SDGNode pred = edge.getSource();
					if(pred.getKind().equals(Kind.PREDICATE)){
						System.out.println(pred + " "+pred.getLabel()+" "+pred.getBytecodeIndex());
						System.out.println("TO: "+node+" "+node.getBytecodeIndex());
						if(pred.getLabel().endsWith(" "+node.getBytecodeIndex()+"")){
							System.out.println("Condition met!!");
						}
						else{
							System.out.println("Condition not met!!");
						}

					}					
				}				
			}
		}



	}



	public Set<SDGNode> getRelevantPreds(Set<SDGNode> chopNodes, SDG sdg){

		Set<SDGNode> result = new HashSet<>();

		for(SDGNode chopNode : chopNodes){

			if(chopNode.getBytecodeIndex() < 0){
				continue;
			}

			for(SDGEdge edge : sdg.incomingEdgesOf(chopNode)){
				if(edge.getKind().equals(SDGEdge.Kind.CONTROL_DEP_COND)){

					SDGNode pred = edge.getSource();
					if(!chopNodes.contains(pred) && pred.getKind().equals(Kind.PREDICATE)){

						result.add(pred);

					}				
				}
			}



		}



		return result;




	}


	public Set<SDGNode> getPredecessorPredNodes(SDGNode node, SDG sdg){
		Set<SDGNode> result = new HashSet<>();

		for(SDGEdge edge: sdg.incomingEdgesOf(node)){
			if(isDataDependencyEdge(edge)){
				SDGNode pred = edge.getSource();
				result.add(pred);
			}


		}

		return result;
	}

	private boolean isIgnored(SDGNode node){
		Set<Kind> ignored = new HashSet<>();
		ignored.add(Kind.ACTUAL_IN);
		ignored.add(Kind.ACTUAL_OUT);
		ignored.add(Kind.EXIT);
		return false;


		//return ignored.contains(node.kind);


	}

	public void printEdge(SDGEdge edge){
		System.out.print("Edge: "+edge.getKind()+" "+edge.getSource().getId()+ " -> "+edge.getTarget());
	}
	private int count = 0;
	public void printNode(SDGNode node, SDG sdg, int i){

		String tab =  "";
		for(int j = 0; j <= i; j++){
			tab += " ";
		}
		if(!isIgnored(node)){
			System.out.println(tab +node+" " + node.getLabel()+ " "+node.getKind());

		}
		//count++;
		for(SDGNode child : next(node, sdg)){
			if(count < 100){
				printNode(child, sdg, i+3);
			}
		}




	}

	private boolean isSummaryEdge(SDGEdge currentEdge) {
		return currentEdge.getKind() == SDGEdge.Kind.SUMMARY;
	}

	private boolean isControlDEpendencyEdge(SDGEdge edge){
		return SDGEdge.Kind.controlFlowEdges().contains(edge.getKind());    	   
	}

	private boolean isDataDependencyEdge(SDGEdge edge){
		return SDGEdge.Kind.dataflowEdges().contains(edge.getKind());

	}
}
