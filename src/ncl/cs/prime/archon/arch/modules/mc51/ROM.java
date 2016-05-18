package ncl.cs.prime.archon.arch.modules.mc51;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class ROM extends Module {

	private static final double TIME_READ = 3.16;
	private static final double ENERGY_READ = 13.06;
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"addr"};
		d.outputNames = new String[] {"data"};
		return d;
	}
	
	protected InPort<Integer> addr = new InPort<>(this);
	protected OutPort<Integer> out = new OutPort<Integer>(this, -1);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {addr};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}
	
	@Override
	protected void estimate(Estimation est, boolean enabled) {
		if(enabled) {
			((Estimate) est).forModule(TIME_READ, ENERGY_READ);
		}
	}
	
	@Override
	protected long update() {
		// dummy
		return (long)(TIME_READ * 1000.0);
	}

}
