package ncl.cs.prime.archon.asmmap;

import java.io.File;
import java.io.IOException;

import ncl.cs.prime.archon.parse.AsmToken;
import ncl.cs.prime.archon.parse.AsmTokeniser;

public class AsmMapParserBase {

	protected AsmMapTokeniser tokeniser = new AsmMapTokeniser();
	protected AsmMapToken token;
	protected boolean success;

	public AsmMapToken getToken() {
		return token;
	}
	
	public void error(String msg, boolean skipToNewline) {
		System.err.println("Parser error on line "+tokeniser.getLineIndex()+": "+msg);
		(new RuntimeException()).printStackTrace();
		success = false;
		
		if(skipToNewline)
			while(token!=null && token.type!=AsmToken.NEWLINE) {
				try {
					token = tokeniser.getNextToken();
				}
				catch(AsmTokeniser.UnknownTokenException e) {
				}
			}
	}
	
	public void error(String msg) {
		error(msg, true);
	}

	public void syntaxError() {
		error("syntax error", true);
	}

	public void next() {
		try {
			token = tokeniser.getNextToken();
		}
		catch(AsmTokeniser.UnknownTokenException e) {
			error("unknown symbol "+tokeniser.getIndex());
			next();
		}
	}

	protected boolean start(File f) {
		try {
			success = true;
			tokeniser.start(f);
		}
		catch(IOException e) {
			success = false;
			System.err.println("Cannot read file.");
		}
		return success;
	}
}
