package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.LinkedList;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class NocRouter extends Module {

	public static final int QUEUE_SIZE = 1024;
	public static final int X_MASK = 0x00ff;
	public static final int Y_MASK = 0xff00;
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req", "n", "s", "w", "e"};
		d.outputNames = new String[] {"done", "link"};
		return d;
	}
	
	private static final long HOP_TIME = 1L; 
	
	private InPort<Integer> req = new InPort<>(this);
	private InPort<Integer> n = new InPort<>(this);
	private InPort<Integer> s = new InPort<>(this);
	private InPort<Integer> w = new InPort<>(this);
	private InPort<Integer> e = new InPort<>(this);
	private OutPort<Integer> done = new OutPort<Integer>(this, null);
	private OutPort<Integer> link = new OutPort<Integer>(this, null);

	private class Packet implements Comparable<Packet> {
		public long time;
		public int addr;
		public int msg;
		
		@Override
		public int compareTo(Packet p) {
			return Long.compare(this.time, p.time);
		}
	}
	
	private LinkedList<Packet> nQueue = new LinkedList<>();
	private LinkedList<Packet> sQueue = new LinkedList<>();
	private LinkedList<Packet> wQueue = new LinkedList<>();
	private LinkedList<Packet> eQueue = new LinkedList<>();
	private LinkedList<Packet> inQueue = new LinkedList<>();

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req, n, s, w, e};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done, link};
	}
	
	public boolean send(int sender, int req, long time) {
		Packet p = new Packet();
		p.time = time;
		p.addr = Mem.getAddr(req);
		p.msg = Mem.makeReq(Mem.getCmd(req), sender);
		return send(p);
	}
	
	private boolean send(Packet p) {
		LinkedList<Packet> queue = null;
		if(p.addr==config)
			queue = inQueue;
		else if((p.addr & X_MASK) < (config & X_MASK))
			queue = wQueue;
		else if((p.addr & X_MASK) > (config & X_MASK))
			queue = eQueue;
		if(queue!=null && queue.size()<QUEUE_SIZE) {
			queue.add(p);
			return true;
		}
		if(p.addr==config)
			return false;
		else if((p.addr & Y_MASK) < (config & Y_MASK))
			queue = nQueue;
		else if((p.addr & Y_MASK) > (config & Y_MASK))
			queue = sQueue;
		if(queue!=null && queue.size()<QUEUE_SIZE) {
			p.time += HOP_TIME;
			queue.add(p);
			return true;
		}
		return false;
	}
	
	private void sendForward(LinkedList<Packet> queue, InPort<Integer> link) {
		if(queue.isEmpty())
			return;
		Packet p = queue.getFirst();
		NocRouter next = (NocRouter) link.getLinkedModule();
		if(next==null) {
			throw new RuntimeException("Bad route at "+config+" for addr "+p.addr);
		}
		if(next.send(p))
			queue.remove(p);
	}
	
	@Override
	protected long update() {
		sendForward(nQueue, n);
		sendForward(sQueue, s);
		sendForward(wQueue, w);
		sendForward(eQueue, e);
		if(!inQueue.isEmpty()) {
			Packet p = inQueue.removeFirst();
			done.value = p.msg;
			syncTime(p.time);
		}
		else {
			done.value = null; // Mem.REQ_NONE;
		}
		if(req.getValue()!=null && Mem.getCmd(req.getValue())!=Mem.CMD_NONE) {
			send(config, req.getValue(), getTime());
		}
		return HOP_TIME;
	}
	
}
