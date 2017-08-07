package joanakeyrefactoring.slicer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * Slices the main method of a java program according to the
 *  variables and locations found in the first occurance of the println Method.
 * @author mihai
 *
 */
public class Main {

	//the slicing criterion - usually println
	private static final String CRITERION = "println";
	//the method to be sliced - usually main, not tested yet on other methods
	private static final String METHOD_TO_BE_SLICED = "main";

	public static void main(String[] args) throws IllegalArgumentException, WalaException, CancelException, IOException {

		String jarPath = args[0];
		String exlusionspath = args[1];
		String javaFile = args[2];

		File exclusions = new File(exlusionspath);

		Collection<Statement> slice = doSlicing(jarPath, exclusions);
		Set<Integer> lines = getLineNumbers(slice);
		
		printSlice(javaFile, lines);
	}
	private static void printSlice(String javaFile, Set<Integer> lines) throws FileNotFoundException, IOException {
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

	public static Set<Integer> getLineNumbers(Collection<Statement> slice){
		Set<Integer> result = new HashSet<Integer>();
		for(Statement s : slice){
			if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
				int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
				try {
					bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
					int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
					//System.out.println("Line: "+src_line_number);
					result.add(src_line_number);
				} catch (Exception e ) {
					//System.err.println("it's probably not a BT method (e.g. it's a fakeroot method)");
					//System.err.println(e.getMessage());
				}
			}
		}		
		return result;
	}

	public static Collection<Statement> doSlicing(String appJar, File exclusions) throws WalaException, IllegalArgumentException, CancelException, IOException {
		// create an analysis scope representing the appJar as a J2SE application
		System.out.println("Create Analysis scope");
		AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(appJar,exclusions);
		System.out.println("Create class hierarchy");
		ClassHierarchy cha = ClassHierarchy.make(scope);
		System.out.println("Compute entry points");
		Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);

		// build the call graph
		System.out.println("Build call graph");
		com.ibm.wala.ipa.callgraph.CallGraphBuilder cgb = Util.makeZeroCFABuilder(options, new AnalysisCache(),cha, scope, null, null);
		CallGraph cg = cgb.makeCallGraph(options, null);
		System.out.println("Create pointer analysis");
		PointerAnalysis pa = cgb.getPointerAnalysis();
		
		// find seed statement
		System.out.println("Find main method");
		Statement statement = findCallTo(findMainMethod(cg), CRITERION);

		Collection<Statement> slice;
		System.out.println("Compute slice ");
		
		DataDependenceOptions ddo = DataDependenceOptions.NO_EXCEPTIONS;
		slice = Slicer.computeBackwardSlice ( statement, cg, pa, ddo, ControlDependenceOptions.NO_EXCEPTIONAL_EDGES );
		
		
		/*
		 * Use this to perform a forward slice. You also need to change the creterion to "read" and insert 
		 * a line like 
		 *    a = System.in.read();
		 * at the beginning of the program. The slice will contain all statements which depend on a from this point on.
		 */
		//slice = Slicer.computeForwardSlice ( statement, cg, pa, ddo, ControlDependenceOptions.NO_EXCEPTIONAL_EDGES );
		
		return slice;

	}

	public static CGNode findMainMethod(CallGraph cg) {
		Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
		Atom name = Atom.findOrCreateUnicodeAtom(METHOD_TO_BE_SLICED);
		for (Iterator<? extends CGNode> it = cg.getSuccNodes(cg.getFakeRootNode()); it.hasNext();) {
			CGNode n = it.next();
			if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
				return n;
			}
		}
		Assertions.UNREACHABLE("failed to find main() method");
		return null;
	}

	public static Statement findCallTo(CGNode n, String methodName) {
		IR ir = n.getIR();
		for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();) {
			SSAInstruction s = it.next();
			if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction) {
				com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction) s;
				if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
					com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
					com.ibm.wala.util.debug.Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
					return new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
				}
			}
		}
		Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
		return null;
	}

	public static void dumpSlice(Collection<Statement> slice) {
		for (Statement s : slice) {
			System.err.println(s);
		}
	}
}
