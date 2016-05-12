package ncl.cs.prime.archon.arch.modules;

import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.Module;

public class Shifter extends Alu<Integer> {

	public static final int ZERO = 0;
	
	public Shifter() {
		super(0);
	}
	
	@Override
	protected FlagOutPort[] initFlags() {
		return initFlags(1);
	}
	
	@Override
	protected void calcFlags(Integer out) {
		flags[ZERO].value = (out==0);
	}
	
	@Override
	protected Integer calc(int func, Integer x, Integer y) {
		switch(func) {
		
		case 0: // left 
			return x << y;
			
		case 2: // right
			return x >> y;
			
		default:
			throw new UnsupportedOperationException(); // TODO proper operation exception
		}
	}
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"x", "y"};
		d.outputNames = new String[] {"out"};
		d.flagNames = new String[] {"z"};
		d.configNames = new String[] {"left", "right"};
		return d;
	}


}
