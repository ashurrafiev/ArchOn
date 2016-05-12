package ncl.cs.prime.archon.asmmap;

import java.io.File;
import java.io.PrintStream;

import ncl.cs.prime.archon.parse.AsmToken;

public abstract class InstructionParser extends AsmMapParserBase {

	protected RegisterMap registerMap;
	protected InstructionTemplate[] templates;
	
	private int opIndexCounter;
	
	public InstructionParser(InstructionTemplate[] templates) {
		this.templates = templates;
	}
	
	protected abstract boolean createRegisterMap(File f, PrintStream out);
	
	public int getOpIndexCounter() {
		return opIndexCounter++;
	}
	
	public String getRegister() {
		String s = (String) token.value;
		if(token.type==AsmMapToken.IDENTIFIER && registerMap.isRegister(s))
			return s.toLowerCase();
		else if(token.type==AsmMapToken.VARIABLE)
			return registerMap.findRegister(s).toLowerCase();
		else {
			error("reg/var expected");
			return null;
		}
	}
	
	public boolean isConst() {
		return token.type==AsmMapToken.NUMBER || token.type==AsmMapToken.CONSTANT;
	}

	public boolean isRegister() {
		return token.type==AsmMapToken.IDENTIFIER || token.type==AsmMapToken.VARIABLE;
	}

	public String asConst() {
		if(token.type==AsmMapToken.NUMBER)
			return "C_"+(Integer) token.value;
		else if(token.type==AsmMapToken.CONSTANT)
			return (String) token.value;
		return null;
	}

	private void reset() {
		opIndexCounter = 0;
	}
	
	private boolean match(PrintStream out) {
		for(InstructionTemplate t : templates) {
			if(t.match(this, out))
				return true;
		}
		return false;
	}
	
	private void parse(PrintStream out) {
		for(;;) {
			next();
			if(token==null) // end of input
				return;
			
			if(token.type==AsmMapToken.LABEL) {
				out.println("\n@"+((String) token.value));
				next();
			}
			
			switch(token.type) {
				case AsmMapToken.NEWLINE:
					continue;

				case AsmMapToken.IDENTIFIER:
					if(!match(out))
						error("no matching template for "+((String) token.value));
					break;
					
				default:
					error("bad token ("+token.type+") "+token.value.toString());
			}
			
			if(token!=null && token.type!=AsmToken.NEWLINE) {
				error("newline expected");
			}
		}
	}

	public boolean parse(File f, PrintStream out) {
		if(!createRegisterMap(f, out))
			return false;
	
		reset();
		if(!start(f))
			return false;
		System.out.println("Parsing instructions in file "+f.getName());
		
		parse(out);
		
		if(!success) {
			System.out.println("Failed.\n");
			return false;
		}
		System.out.println("Done.\n");

		// TODO
		
		return true;
	}
	
}
