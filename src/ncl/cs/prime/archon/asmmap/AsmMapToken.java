package ncl.cs.prime.archon.asmmap;

public class AsmMapToken {

	public static final int NEWLINE = 0; 
	public static final int SYMBOL = 1;
	public static final int NUMBER = 2;
	public static final int IDENTIFIER = 3;
	public static final int VARIABLE = 4;
	public static final int CONSTANT = 5;
	public static final int LABEL = 6;

	public int type;
	public Object value;
	
	public AsmMapToken(int type, Object value) {
		this.type = type;
		this.value = value;
	}

	public AsmMapToken(int type) {
		this.type = type;
		value = null;
	}
	
	public AsmMapToken(char c) {
		type = SYMBOL;
		value = c;
	}
	
	public boolean is(char c) {
		return type==SYMBOL && c==(char)value;
	}
	
}
