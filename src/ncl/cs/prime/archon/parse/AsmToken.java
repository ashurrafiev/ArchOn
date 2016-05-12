package ncl.cs.prime.archon.parse;

public class AsmToken {

	public static final int NEWLINE = 0; 
	public static final int GENERIC_CHAR = 1;
	public static final int GENERIC_STRING = 2;
	public static final int INTEGER = 3;
	public static final int STRING = 4;
	public static final int IDENTIFIER = 5;
	public static final int DIRECTIVE = 6;
	public static final int COMMAND = 7;
	public static final int LABEL = 8;

	public int type;
	public Object value;
	
	public AsmToken(int type, Object value) {
		this.type = type;
		this.value = value;
	}

	public AsmToken(int type) {
		this.type = type;
		value = null;
	}

	public AsmToken(String s) {
		type = GENERIC_STRING;
		value = s;
	}

	public AsmToken(char c) {
		type = GENERIC_CHAR;
		value = c;
	}
	
	public boolean is(String s) {
		return (s.length()==1) && type==GENERIC_CHAR && s.charAt(0)==(char)value ||
			type==GENERIC_STRING && s.equals((String) value);
	}

}
