package ncl.cs.prime.archon.parse;

import java.util.regex.Pattern;

public class AsmTokeniser extends Tokeniser<AsmToken> {

	public AsmTokeniser() {
		super(new Pattern[] {
				Pattern.compile("\\s*[\\n\\r]+", Pattern.MULTILINE), // 0: newline
				Pattern.compile("\\s+"), // 1: whitespace
				Pattern.compile("\\/\\*.*?\\*\\/", Pattern.MULTILINE+Pattern.DOTALL), // 2: multiline comment
				Pattern.compile("\\/\\/.*?$", Pattern.MULTILINE+Pattern.DOTALL), // 3: line comment
				Pattern.compile("\\-?\\d+"), // 4: number
				Pattern.compile("\\\".*?\\\""), // 5: string
				Pattern.compile("[\\!\\#@][A-Za-z0-9_]*"), // 6: directive or command 
				Pattern.compile("[A-Za-z_][A-Za-z0-9_\\$\\.]*"), // 7: identifier
				Pattern.compile("\\=X"), // 8: complex operators
				Pattern.compile(".") // symbol
			});
	}
	
	@Override
	protected AsmToken evaluateToken(int match, String raw) {
//		System.out.println("\t\tSRC: "+raw);
		switch(match) {
		case 0:
			return new AsmToken(AsmToken.NEWLINE);
		case 1:
		case 2:
		case 3:
			return null; // ignore whitespace and comments

		case 4:
			return new AsmToken(AsmToken.INTEGER, Integer.parseInt(raw));
		case 5:
			return new AsmToken(AsmToken.STRING, raw.substring(1, raw.length()-1));
		case 6:
			if(raw.charAt(0)=='#')
				return new AsmToken(AsmToken.DIRECTIVE, raw.substring(1));
			else if(raw.charAt(0)=='!')
				return new AsmToken(AsmToken.COMMAND, raw.substring(1));
			else
				return new AsmToken(AsmToken.LABEL, raw.substring(1));
		case 7:
			return new AsmToken(AsmToken.IDENTIFIER, raw);
		case 8:
			return new AsmToken(raw);
			
		default:
			return new AsmToken(raw.charAt(0));
	}
	}

}
