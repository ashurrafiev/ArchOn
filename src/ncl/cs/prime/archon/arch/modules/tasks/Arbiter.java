package ncl.cs.prime.archon.arch.modules.tasks;

import java.util.LinkedList;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Arbiter extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req1", "req2", "nextAck"};
		d.outputNames = new String[] {"ack1", "ack2", "nextReq"};
		return d;
	}	

	private InPort.Int[] req = {new InPort.Int(this), new InPort.Int(this)};
	private OutPort.Int[] ack = {new OutPort.Int(this, null), new OutPort.Int(this, null)};
	private OutPort.Int nextReq = new OutPort.Int(this, null);
	private InPort.Int nextAck = new InPort.Int(this);

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req[0], req[1], nextAck};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {ack[0], ack[1], nextReq};
	}

	@Override
	protected long update() {
		return 0;
	}
	
	private class ReqItem {
		public final int req;
		public final int value;
		public ReqItem(int req, int value) {
			this.req = req;
			this.value = value;
			// System.out.printf("Time %d: #%d queued\n", getTime(), req);
		}
	}
	
	private LinkedList<ReqItem> queue = new LinkedList<>();
	
	protected long sendTime = 0L;
	protected int resolve = -1;
	
	@Override
	protected long update(Estimation est) {
		if(req[0].getValue()!=null)
			queue.add(new ReqItem(0, req[0].getValue()));
		if(req[1].getValue()!=null)
			queue.add(new ReqItem(1, req[1].getValue()));
		
		ack[0].value = null;
		ack[1].value = null;
		
		if(resolve>=0) {
			Integer v = nextAck.getValue();
			ack[resolve].value = v;
			nextReq.value = null;
			if(v!=null) {
				// System.out.printf("Time %d: #%d released\n", getTime(), resolve);
				resolve = -1;
			}
		}
		else if(!queue.isEmpty()) {
			ReqItem ri = queue.removeFirst();
			resolve = ri.req;
			nextReq.value = ri.value;
			sendTime = getTime();
			// System.out.printf("Time %d: #%d granted\n", getTime(), resolve);
		}
		
		return 0L;
	}

}
