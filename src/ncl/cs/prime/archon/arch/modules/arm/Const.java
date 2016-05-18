package ncl.cs.prime.archon.arch.modules.arm;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Const extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.outputNames = new String[] {"d"};
		return d;
	}
	
	protected OutPort<Integer> d = new OutPort<Integer>(this, 0);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {d};
	}
	
	@Override
	protected long update() {
		return 0L;
	}
	
}
