package ncl.cs.prime.archon.arch.modules.hicore;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Core extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ack", "op"};
		d.outputNames = new String[] {"done", "mem_req"};
		return d;
	}	
	
	private static final long TIME = 1L;
	
	private InPort<Integer> ack = new InPort<>(this);
	private InPort<Integer> op = new InPort<>(this);
	private OutPort<Integer> done = new OutPort<Integer>(this, null);
	private OutPort<Integer> memReq = new OutPort<Integer>(this, null);

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {ack, op};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done, memReq};
	}
	
	@Override
	protected long update() {
		if(op.getValue()==null || op.getValue()==App.OP_WAIT) {
			done.value = ack.getValue()==null ? null : Mem.getCmd(ack.getValue());
			memReq.value = null; // Mem.REQ_NONE;
			return 0L;
		}
		else if(op.getValue()==App.OP_CPU) {
			// TODO add core power
			done.value = 1;
			memReq.value = null; // Mem.REQ_NONE;
			return TIME;
		}
		else {
			done.value = null;
			memReq.value = Mem.makeReq(op.getValue(), config);
			return 0L;
		}
	}

}
