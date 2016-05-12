package ncl.cs.prime.archon.asmmap;

import java.util.regex.Pattern;

import ncl.cs.prime.archon.parse.Tokeniser;

public class AsmMapTokeniser extends Tokeniser<AsmMapToken> {

	public AsmMapTokeniser() {
		super(new Pattern[] {
				Pattern.compile("\\s*[\\n\\r]+", Pattern.MULTILINE), // 0: newline
				Pattern.compile("\\s*\\,?\\s+"), // 1: whitespace
				Pattern.compile("\\;.*?$", Pattern.MULTILINE+Pattern.DOTALL), // 2: line comment
				Pattern.compile("\\#\\-?\\d+"), // 3: const number
				Pattern.compile("[\\$\\#]?[A-Za-z_][A-Za-z0-9_\\.]*\\:?"), // 4: identifier
				Pattern.compile(".") // symbol
			});
	}
	
	@Override
	protected AsmMapToken evaluateToken(int match, String raw) {
		switch(match) {
			case 0:
				return new AsmMapToken(AsmMapToken.NEWLINE);
			case 1:
			case 2:
				return null; // ignore whitespace and comments
	
			case 3:
				return new AsmMapToken(AsmMapToken.NUMBER, Integer.parseInt(raw.substring(1)));
			case 4:
				if(raw.startsWith("$"))
					return new AsmMapToken(AsmMapToken.VARIABLE, raw.substring(1));
				else if(raw.startsWith("#"))
					return new AsmMapToken(AsmMapToken.CONSTANT, raw.substring(1).toUpperCase());
				else if(raw.endsWith(":"))
					return new AsmMapToken(AsmMapToken.LABEL, raw.substring(0, raw.length()-1).toUpperCase());
				else
					return new AsmMapToken(AsmMapToken.IDENTIFIER, raw.toUpperCase());
				
			default:
				return new AsmMapToken(raw.charAt(0));
		}
	}

}
