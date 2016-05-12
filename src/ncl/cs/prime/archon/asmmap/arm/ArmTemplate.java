package ncl.cs.prime.archon.asmmap.arm;

import java.io.PrintStream;

import ncl.cs.prime.archon.asmmap.AsmMapToken;
import ncl.cs.prime.archon.asmmap.InstructionParser;
import ncl.cs.prime.archon.asmmap.InstructionTemplate;

public abstract class ArmTemplate extends InstructionTemplate {

	public static final String[] SHIFT_NAMES = {"LSL", "LSR", "ASR", "ROR"};
	
	private static final String[] CONDITIONS = {"", "EQ", "NE", "MI", "PL"};
	private static final String[] NFLAGS = {"", "^alu.z", "alu.z", "^alu.n", "alu.n"};
	
	@Override
	public boolean match(InstructionParser parser, PrintStream out) {
		String name = (String) parser.getToken().value;
		for(int j=0; j<getSuffixes().length; j++)
			for(int i=0; i<getBaseNames().length; i++)
				for(int k=0; k<CONDITIONS.length; k++) {
					if(name.equals(getBaseNames()[i]+CONDITIONS[k]+getSuffixes()[j])) {
						out.println();
						String label = "@Op"+parser.getOpIndexCounter()+"_skip";
						if(k>0)
							out.println("["+NFLAGS[k]+"] #jump "+label);
						process(i, j, parser, out);
						if(k>0)
							out.println(label);
						if(parser.getToken().type!=AsmMapToken.NEWLINE)
							parser.next();
						return true;
					}
				}
		return false;
	}
	
	protected abstract String[] getBaseNames();
	protected String[] getSuffixes() {
		return new String[] {""};
	}
	protected abstract void process(int base, int suffix, InstructionParser parser, PrintStream out);

	public static boolean isShift(String s) {
		for(String str : SHIFT_NAMES)
			if(str.equals(s))
				return true;
		return false;
	}
	
}
