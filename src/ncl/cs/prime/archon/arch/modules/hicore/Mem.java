package ncl.cs.prime.archon.arch.modules.hicore;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Mem extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req"};
		d.outputNames = new String[] {"done"};
		return d;
	}	

	private static final long TIME[] = {0L, 0L, 1L, 100L};
	
	public static final int REQ_NONE = 0;
	public static final int REQ_READ = 2;
	public static final int REQ_WRITE = 3;

	private InPort<Integer> req = new InPort<>(this);
	private OutPort<Integer> done = new OutPort<Integer>(this, null);

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done};
	}
	
	@Override
	protected long update() {
		done.value = req.getValue();
		return done.value==null ? 0L : TIME[done.value];
	}

}
