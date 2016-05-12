package ncl.cs.prime.archon.asmmap.arm;

import java.io.PrintStream;

import ncl.cs.prime.archon.asmmap.AsmMapToken;
import ncl.cs.prime.archon.asmmap.InstructionParser;

public class BranchTemplate extends ArmTemplate {

	private static final String[] NAMES = {
		"B"
	};

	@Override
	protected String[] getBaseNames() {
		return NAMES;
	}

	@Override
	protected void process(int base, int suffix, InstructionParser parser, PrintStream out) {
		parser.next();
		if(parser.getToken().type==AsmMapToken.IDENTIFIER) {
			out.println("!jump @"+(String) parser.getToken().value);
		}
		else {
			parser.error("address expected");
		}
	}

}
