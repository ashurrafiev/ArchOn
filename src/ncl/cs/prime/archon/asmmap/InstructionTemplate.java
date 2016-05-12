package ncl.cs.prime.archon.asmmap;

import java.io.PrintStream;

public abstract class InstructionTemplate {

	public abstract boolean match(InstructionParser parser, PrintStream out);
	
}
