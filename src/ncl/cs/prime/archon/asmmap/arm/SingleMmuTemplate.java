package ncl.cs.prime.archon.asmmap.arm;

import java.io.PrintStream;

import ncl.cs.prime.archon.asmmap.AsmMapToken;
import ncl.cs.prime.archon.asmmap.InstructionParser;

public class SingleMmuTemplate extends ArmTemplate {

	private static final String[] NAMES = {
		"LDR", "STR"
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
		if(!parser.getToken().is('[')) {
			parser.error("[ expected");
			return;
		}
		
		// pre-index
		parser.next();
		String addr;
		String rn;
		boolean postIndex = false;
		boolean writeBack = false;
		if(parser.isConst()) {
			addr = parser.asConst();
			rn = null;
			
			parser.next();
			if(!parser.getToken().is(']')) {
				parser.error("] expected");
				return;
			}
		}
		else {
			rn = parser.getRegister();
			if(rn==null) return;
			addr = rn;
			
			parser.next();
			String op = "add";
			if(parser.getToken().is('+')) {
				parser.next();
			}
			else if(parser.getToken().is('-')) {
				op = "sub";
				parser.next();
			}
			
			if(parser.isRegister()) {
				String rm = parser.getRegister();
				if(rm==null) return;

				String op2 = rm;
				parser.next();
				if(parser.getToken().type==AsmMapToken.IDENTIFIER) {
					String sh = (String) parser.getToken().value;
					if(!ArmTemplate.isShift(sh)) {
						parser.error("bad shift operator");
						return;
					}
					parser.next();
					if(!parser.isConst()) {
						parser.error("not a constant");
						return;
					}
					
					out.println("shift("+sh.toLowerCase()+")");
					out.println("shift.m = "+rm);
					out.println("shift.shift = "+parser.asConst());
					out.println("!gox");
					op2 = "shift";
				}
				else if(parser.getToken().is(']')) {
				}
				else {
					parser.error("bad index expression");
					return;
				}
				
				out.println("offs("+op+")");
				out.println("offs.n = "+rn);
				out.println("offs.m = "+op2);
				out.println("!gox");
				writeBack = true;
			}
			else if(parser.isConst()) {
				out.println("offs(add)");
				out.println("offs.n = "+rn);
				out.println("offs.m = "+parser.asConst());
				out.println("!gox");
				writeBack = true;
			}
			else if(parser.getToken().is(']')) {
				postIndex = true;
			}
			else {
				parser.error("bad index expression");
				return;
			}
		}
		if(writeBack) {
			parser.next();
			if(!parser.getToken().is(']')) {
				parser.error("] expected");
				return;
			}
			parser.next();
			if(parser.getToken().is('!')) {
				out.println(rn+" = offs");
				out.println("!gox");
			}
		}
		
		if(base==0) {
			out.println("mmu(read)");
			out.println("mmu.addr = "+addr);
			out.println("!gox");
			out.println(rd+" = mmu");
			out.println("!gox");
		}
		else if(base==1) {
			out.println("mmu(write)");
			out.println("mmu.n = "+rd);
			out.println("mmu.addr = "+addr);
			out.println("!gox");
		}
		
		if(postIndex) {
			parser.next();
			String op = "add";
			if(parser.getToken().is('+')) {
				parser.next();
			}
			else if(parser.getToken().is('-')) {
				op = "sub";
				parser.next();
			}
			
			if(parser.isRegister()) {
				String rm = parser.getRegister();
				if(rm==null) return;

				String op2 = rm;
				parser.next();
				if(parser.getToken().type==AsmMapToken.IDENTIFIER) {
					String sh = (String) parser.getToken().value;
					if(!ArmTemplate.isShift(sh)) {
						parser.error("bad shift operator");
						return;
					}
					parser.next();
					if(!parser.isConst()) {
						parser.error("not a constant");
						return;
					}
					
					out.println("shift("+sh.toLowerCase()+")");
					out.println("shift.m = "+rm);
					out.println("shift.shift = "+parser.asConst());
					out.println("!gox");
					op2 = "shift";
				}
				else if(parser.getToken().is(']')) {
				}
				else {
					parser.error("bad index expression");
					return;
				}
				
				out.println("offs("+op+")");
				out.println("offs.n = "+rn);
				out.println("offs.m = "+op2);
				out.println("!gox");
			}
			else if(parser.isConst()) {
				out.println("offs(add)");
				out.println("offs.n = "+rn);
				out.println("offs.m = "+parser.asConst());
				out.println("!gox");
			}
			else if(parser.getToken().type==AsmMapToken.NEWLINE) {
				return;
			}
			else {
				parser.error("bad index expression");
				return;
			}
			
			out.println(rn+" = offs");
			out.println("!gox");
		}
	}

}
