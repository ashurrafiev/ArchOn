package ncl.cs.prime.archon.arch.modules.mc51;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Reg extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"in"};
		d.outputNames = new String[] {"out"};
		return d;
	}
	
	protected InPort<Integer> in = new InPort<>(this);
	protected OutPort<Integer> out = new OutPort<Integer>(this, -1);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {in};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}
	
	@Override
	protected void estimate(Estimation est, boolean enabled) {
		// TODO reg params?
	}
	
	@Override
	protected long update() {
		out.value = in.getValue();
		return 0L;
	}

}
