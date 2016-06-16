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
		p.time = time;// + HOP_TIME;
		queue.add(p);
		Collections.sort(queue);
	}
	
	public void accept(int sender) {
		if(sender==this.sender)
			this.sender = -1;
	}

	public static long time;
	public static int counter = 0;
	
	@Override
	protected long update() {
		long delay = 0L;
		if(waiting>=0 && sender<0) {
			msg = ack.getValue();
			if(msg!=null && Mem.getCmd(msg)!=Mem.CMD_NONE) {
				sender = waiting;
				waiting = -1;
//				delay = HOP_TIME;
			}
		}
		if(waiting<0 && !queue.isEmpty()) {
			Packet p = queue.removeFirst();
			memReq.value = p.msg;
			waiting = p.sender;
			syncTime(p.time);
//			long dt = getTime() + HOP_TIME - time;
			time = getTime() + HOP_TIME;
			counter++;
//			System.out.printf("[%d] %d / %d = %d\n", dt, time, counter, time/counter);
			delay = HOP_TIME;
		}
		else {
			memReq.value = null; // Mem.REQ_NONE;
		}
		return delay;
	}

}
