package ncl.cs.prime.archon.asmmap;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class RegisterMap extends AsmMapParserBase {

	protected HashMap<String, String> varMap = new HashMap<>();
	protected HashSet<String> namedConsts = new HashSet<>();
	protected HashSet<Integer> numberConsts = new HashSet<>();
	protected HashSet<Integer> usedRegs = new HashSet<>();
	
	public abstract String[] getRegisterNames();
	
	public int getRegisterCount() {
		return getRegisterNames().length;
	}
	
	private void reset() {
		varMap.clear();
		namedConsts.clear();
		usedRegs.clear();
	}
	
	public String findRegister(String s) {
		return varMap.get(s);
	}
	
	public boolean isRegister(String s) {
		for(int i=0; i<getRegisterNames().length; i++) {
			if(getRegisterNames()[i].equals(s)) {
				return true;
			}
		}
		return false;
	}
	
	private HashSet<String> parse() {
		String[] regNames = getRegisterNames();
		HashSet<String> varList = new HashSet<>();
		for(;;) {
			next();
			if(token==null) // end of input
				return varList;
			
			switch(token.type) {
				case AsmMapToken.VARIABLE:
					varList.add((String) token.value);
					break;
				case AsmMapToken.IDENTIFIER:
					for(int i=0; i<regNames.length; i++) {
						if(regNames[i].equals((String) token.value)) {
							usedRegs.add(i);
						}
					}
					break;
				case AsmMapToken.CONSTANT:
					namedConsts.add((String) token.value);
					break;
				case AsmMapToken.NUMBER:
					numberConsts.add((Integer) token.value);
					break;

				default:
					// skip
			}
		}
	}
	
	public boolean scanVars(File f, PrintStream out) {
		reset();
		if(!start(f))
			return false;
		System.out.println("Scanning variables from file "+f.getName());
		HashSet<String> varList = parse();

		if(!success) {
			System.out.println("Failed.\n");
			return false;
		}
		System.out.println("Done.\n");
		
		String[] regNames = getRegisterNames();
		int nextReg = -1;
		for(String var : varList) {
			do {
				nextReg++;
				if(nextReg>=regNames.length) {
					System.err.println("Need more registers to map all variables.");
					return false;
				}
			} while(usedRegs.contains(nextReg));
			varMap.put(var, regNames[nextReg]);
		}
		
		out.println("\n/* --- Named constants --- */");
		for(String s : namedConsts) {
			out.println("#assign "+s+" \".Const\"\n// TODO #init "+s);
		}
		out.println("\n/* --- Anonymous constants --- */");
		for(Integer n : numberConsts) {
			out.println("#assign C_"+n+" \".Const\"\n#init C_"+n+" "+n);
		}
		out.println("\n/* --- Var mapping:");
		for(Map.Entry<String, String> e : varMap.entrySet()) {
			out.println("$"+e.getKey()+" -> "+e.getValue().toLowerCase());
		}
		out.println(" --- */");
		
		return true;
	}
	
}
