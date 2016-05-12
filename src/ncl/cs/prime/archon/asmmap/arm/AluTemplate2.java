package ncl.cs.prime.archon.asmmap.arm;

import java.io.PrintStream;

import ncl.cs.prime.archon.asmmap.InstructionParser;

public class AluTemplate2 extends AluTemplateBase {

	private static final String[] NAMES = {
		"AND", "EOR", "SUB", "RSB", "ADD", "ADC", "SBC", "RSC", "TST", "TEQ", "CMP", "CMN", "ORR", "BIC"
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
		
		parser.next();
		String rn = parser.getRegister();
		if(rn==null) return;
		
		String op2 = decodeOp2(parser, out);
		if(op2==null) return;
		
		out.println("alu("+NAMES[base].toLowerCase()+")");
		out.println("alu.n = "+rn);
		out.println("alu.m = "+op2);
		out.println("!gox");
		out.println(rd+" = alu");
		out.println("!gox");
	}

}
