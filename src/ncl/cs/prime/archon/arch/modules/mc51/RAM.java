package ncl.cs.prime.archon.arch.modules.mc51;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class RAM extends Module {

	private static final double TIME_WRITE = 3.16;
	private static final double TIME_READ = 3.16;
	private static final double ENERGY_WRITE = 14.34;
	private static final double ENERGY_READ = 13.06;
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"data", "addr"};
		d.outputNames = new String[] {"data"};
		d.configNames = new String[] {"read", "write"};
		return d;
	}
	
	protected InPort<Integer> in = new InPort<>(this);
	protected InPort<Integer> addr = new InPort<>(this);
	protected OutPort<Integer> out = new OutPort<Integer>(this, -1);
	
	protected int[] memory = new int[256];
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {in, addr};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}
	
	@Override
	protected void estimate(Estimation est, boolean enabled) {
		if(enabled && config>0) {
			((Estimate) est).forModule(TIME_WRITE, ENERGY_WRITE);
		}
		else if(enabled && config==0) {
			((Estimate) est).forModule(TIME_READ, ENERGY_READ);
		}
	}
	
	@Override
	protected long update() {
		if (config==0) { // read mode
			out.value = memory[addr.getValue()];
			return (long)(TIME_READ * 1000.0);
		}
		else { // write mode
			memory[addr.getValue()] = in.getValue();
			out.value = in.getValue();
			return (long)(TIME_WRITE * 1000.0);
		}
	}

}
