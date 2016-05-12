package ncl.cs.prime.archon.asmmap.arm;

import java.io.PrintStream;

import ncl.cs.prime.archon.asmmap.AsmMapToken;
import ncl.cs.prime.archon.asmmap.InstructionParser;


public abstract class AluTemplateBase extends ArmTemplate {

	@Override
	protected String[] getSuffixes() {
		return new String[] {"", "S"};
	}

	protected String decodeOp2(InstructionParser parser, PrintStream out) {
		parser.next();
		String op2;
		if(parser.isConst()) {
			op2 = parser.asConst();
		}
		else {
			String rm = parser.getRegister();
			if(rm==null) return null;
			
			parser.next();
			if(parser.getToken().type==AsmMapToken.IDENTIFIER) {
				String sh = (String) parser.getToken().value;
				if(!ArmTemplate.isShift(sh)) {
					parser.error("bad shift operator");
					return null;
				}
				parser.next();
				if(!parser.isConst()) {
					parser.error("not a constant");
					return null;
				}
				
				out.println("shift("+sh.toLowerCase()+")");
				out.println("shift.m = "+rm);
				out.println("shift.shift = "+parser.asConst());
				out.println("!gox");
				op2 = "shift";
			}
			else if(parser.getToken().type==AsmMapToken.NEWLINE) {
				op2 = rm;
			}
			else {
				parser.error("bad operand expression");
				return null;
			}
		}
		return op2;
	}
	
}
