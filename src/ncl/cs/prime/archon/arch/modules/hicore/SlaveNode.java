package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.Collections;
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

	private static final long HOP_TIME = 1L; 
	
	private InPort<Integer> ack = new InPort<>(this);
	private OutPort<Integer> memReq = new OutPort<Integer>(this, null);
	private OutPort<Integer> link = new OutPort<Integer>(this, null);

	private int waiting = -1;
	public int sender = -1;
	public Integer msg;
	
	private class Packet implements Comparable<Packet> {
		public long time;
		public int sender;
		public int msg;
		
		@Override
		public int compareTo(Packet p) {
			return Long.compare(this.time, p.time);
		}
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
	
	public void send(int sender, int msg, long time) {
		Packet p = new Packet();
		p.sender = sender;
		p.msg = msg;
		p.time = time + HOP_TIME;
		queue.add(p);
		Collections.sort(queue);
	}
	
	public void accept(int sender) {
		if(sender==this.sender)
			this.sender = -1;
	}

	@Override
	protected long update() {
		if(waiting>=0 && sender<0) {
			msg = ack.getValue();
			if(msg!=null && Mem.getCmd(msg)!=Mem.CMD_NONE) {
				sender = waiting;
				waiting = -1;
			}
		}
		if(waiting<0 && !queue.isEmpty()) {
			Packet p = queue.removeFirst();
			memReq.value = p.msg;
			waiting = p.sender;
			syncTime(p.time);
		}
		else {
			memReq.value = null; // Mem.REQ_NONE;
		}
		return HOP_TIME;
	}

}
