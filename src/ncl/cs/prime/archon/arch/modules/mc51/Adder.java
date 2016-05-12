package ncl.cs.prime.archon.arch.modules.mc51;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Adder extends Module {

	private static final double TIME = 1.81;
	private static final double ENERGY = 2.93;
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"x", "y"};
		d.outputNames = new String[] {"out"};
		d.flagNames = new String[] {"z"};
		d.configNames = new String[] {"test", "add", "sub"};
		return d;
	}
	
	protected InPort<Integer> x = new InPort<>(this);
	protected InPort<Integer> y = new InPort<>(this);
	protected OutPort<Integer> out = new OutPort<Integer>(this, -1);
	
	protected FlagOutPort z = new FlagOutPort(this);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {x, y};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}
	
	@Override
	protected FlagOutPort[] initFlags() {
		return new FlagOutPort[] {z};
	}

	private Integer calc(int func, Integer x, Integer y) {
		switch(func) {
		
		case 0: // test
			return x;
		
		case 1: // add
			return x+y;
			
		case 2: // sub
			return x-y;
			
		default:
			throw new UnsupportedOperationException(); // TODO proper operation exception
			
		}
	}
	
	@Override
	protected void estimate(Estimation est, boolean enabled) {
		if(enabled && config>0) {
			((Estimate) est).forModule(TIME, ENERGY);
		}
	}
	
	@Override
	protected void update() {
		out.value = calc(config, x.getValue(), y.getValue());
		z.value = (out.value==0);
	}

}
