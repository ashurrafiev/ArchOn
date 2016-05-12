package ncl.cs.prime.archon.arch.modules;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Reg<T> extends Module {

	private InPort<T> in;
	private OutPort<T> out;
	
	public Reg(T init) {
		in = new InPort<T>(this);
		out = new OutPort<T>(this, init);
	}
	
	@Override
	public InPort<?>[] initInputs() {
		return new InPort<?>[] {in};
	}
	
	@Override
	public OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}

	@Override
	protected void update() {
		out.value = in.getValue();
	}

}
