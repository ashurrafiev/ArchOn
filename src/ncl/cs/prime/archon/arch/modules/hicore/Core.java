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
	
	private InPort<Integer> ack = new InPort<>(this);
	private InPort<Integer> op = new InPort<>(this);
	private OutPort<Integer> done = new OutPort<Integer>(this, 0);
	private OutPort<Integer> memReq = new OutPort<Integer>(this, 0);

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
		if(op.getValue()==App.OP_WAIT) {
			done.value = ack.getValue();
			memReq.value = Mem.REQ_NONE;
			return 0L;
		}
		else if(op.getValue()==App.OP_CPU) {
			// TODO add core power
			done.value = 1;
			memReq.value = Mem.REQ_NONE;
			return 1L;
		}
		else {
			done.value = 0;
			memReq.value = op.getValue();
			return 0L;
		}
	}

}
