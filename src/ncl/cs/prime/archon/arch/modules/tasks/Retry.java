package ncl.cs.prime.archon.arch.modules.tasks;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Retry extends ncl.cs.prime.archon.arch.Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req", "nextAck", "retry"};
		d.outputNames = new String[] {"ack", "nextReq"};
		return d;
	}	

	protected InPort.Int retry = new InPort.Int(this);
	protected InPort.Int req = new InPort.Int(this);
	protected OutPort.Int ack = new OutPort.Int(this, null);
	protected OutPort.Int nextReq = new OutPort.Int(this, null);
	protected InPort.Int nextAck = new InPort.Int(this);

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req, nextAck, retry};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {ack, nextReq};
	}

	@Override
	protected long update() {
		return 0;
	}
	
	private int lastReq = 0;
	
	@Override
	protected long update(Estimation est) {
		if(req.getValue()==null && retry.getValue()==null) {
			ack.value = nextAck.getValue();
			nextReq.value = null;
		}
		else {
			ack.value = null;
			if(req.getValue()!=null)
				lastReq = req.getValue();
			nextReq.value = lastReq;
		}

		return 0L;
	}
	
}
