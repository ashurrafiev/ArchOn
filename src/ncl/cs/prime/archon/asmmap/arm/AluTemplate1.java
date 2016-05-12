package ncl.cs.prime.archon.asmmap.arm;

import java.io.PrintStream;

import ncl.cs.prime.archon.asmmap.InstructionParser;

public class AluTemplate1 extends AluTemplateBase {

	private static final String[] NAMES = {
		"MOV", "MVN"
	};

	@Override
	protected String[] getBaseNames() {
		return NAMES;
	}

	@Override
	protected void process(int base, int suffix, InstructionParser parser, PrintStream out) {
		parser.next();
		String rd = parser.getRegister();
		if(rd==null) return;
		
		String op2 = decodeOp2(parser, out);
		if(op2==null) return;
		
		out.println("alu("+NAMES[base].toLowerCase()+")");
		out.println("alu.m = "+op2);
		out.println("!gox");
		out.println(rd+" = alu");
		out.println("!gox");
	}

}
