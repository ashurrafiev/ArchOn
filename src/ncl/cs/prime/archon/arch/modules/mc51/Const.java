package ncl.cs.prime.archon.arch.modules.mc51;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Const extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.outputNames = new String[] {"out"};
		return d;
	}
	
	protected OutPort.Int out = new OutPort.Int(this, 0);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}
	
	@Override
	protected long update() {
		return 0L;
	}

}
