package ncl.cs.prime.archon.arch.modules;

import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.Module;

public class IntAlu extends Alu<Integer> {

	public static final int ZERO = 0;
	public static final int SIGN = 1;
	public static final int OVERFLOW = 2;
	
	public IntAlu() {
		super(0);
	}
	
	@Override
	protected FlagOutPort[] initFlags() {
		return initFlags(3);
	}
	
	@Override
	protected void calcFlags(Integer out) {
		flags[ZERO].value = (out==0);
		flags[SIGN].value = (out<0);
	}
	
	@Override
	protected Integer calc(int func, Integer x, Integer y) {
		flags[OVERFLOW].value = false;
		switch(func) {
		
		case 0: // test
			return x;
		
		case 1: // add
			flags[OVERFLOW].value = (x>Integer.MAX_VALUE-y);
			return x+y;
			
		case 2: // sub
			flags[OVERFLOW].value = (x<Integer.MIN_VALUE+y);
			return x-y;
			
		case 3: // inc
			flags[OVERFLOW].value = (x>Integer.MAX_VALUE-1);
			return x+1;
			
		case 4: // dec
			flags[OVERFLOW].value = (x<Integer.MIN_VALUE+1);
			return x-1;

		case 5: // xor
			flags[OVERFLOW].value = false;
			return x ^ y;
			
		// TODO more operations
			
		default:
			throw new UnsupportedOperationException(); // TODO proper operation exception
		}
	}
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"x", "y"};
		d.outputNames = new String[] {"out"};
		d.flagNames = new String[] {"z", "s", "o"};
		d.configNames = new String[] {"test", "add", "sub", "inc", "dec", "xor"};
		return d;
	}


}
