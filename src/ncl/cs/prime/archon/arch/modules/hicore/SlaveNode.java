package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.LinkedList;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class SlaveNode extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ack"};
		d.outputNames = new String[] {"mem_req", "link"};
		return d;
	}	
	
	private InPort<Integer> ack = new InPort<>(this);
	private OutPort<Integer> memReq = new OutPort<Integer>(this, 0);
	private OutPort<Integer> link = new OutPort<Integer>(this, 0);

	private int waiting = -1;
	public int sender = -1;
	public int msg;
	
	private class Packet {
		public int sender;
		public int msg;
	}
	
	private LinkedList<Packet> queue = new LinkedList<>();
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {ack};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {memReq, link};
	}
	
	public void send(int sender, int msg) {
		Packet p = new Packet();
		p.sender = sender;
		p.msg = msg;
		queue.add(p);
	}
	
	public void accept(int sender) {
		if(sender==this.sender)
			this.sender = -1;
	}
	
	@Override
	protected long update() {
		if(waiting>=0 && sender<0) {
			msg = ack.getValue();
			if(msg!=Mem.REQ_NONE) {
				sender = waiting;
				waiting = -1;
			}
		}
		if(waiting<0 && !queue.isEmpty()) {
			Packet p = queue.removeFirst();
			memReq.value = p.msg;
			waiting = p.sender;
		}
		else {
			memReq.value = Mem.REQ_NONE;
		}
		return 0L;
	}

}
