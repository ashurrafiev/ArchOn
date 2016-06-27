package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.LinkedList;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class NocRouter extends HiModule {

	public static final int X_MASK = 0x00ff;
	public static final int Y_MASK = 0xff00;
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req", "n", "s", "w", "e"};
		d.outputNames = new String[] {"done", "link"};
		return d;
	}
	
	private static final int QUEUE_SIZE = 64;
	private static final int TTL = 1024;
	private static final long DELAY_HOP = 2L;
	private static final double ENERGY_HOP = 1.0;
	private static final double LEAKAGE = 1.0;
	
	private int queueSize = QUEUE_SIZE;
	private int packetTTL = TTL;
	private long delayHop = DELAY_HOP;
	private double energyHop = ENERGY_HOP;
	private double leakage = LEAKAGE;
	
	private InPort<Integer> req = new InPort<>(this);
	private InPort<Integer> n = new InPort<>(this);
	private InPort<Integer> s = new InPort<>(this);
	private InPort<Integer> w = new InPort<>(this);
	private InPort<Integer> e = new InPort<>(this);
	private OutPort<Integer> done = new OutPort<Integer>(this, null);
	private OutPort<Integer> link = new OutPort<Integer>(this, null);

	private class Packet implements Comparable<Packet> {
		public long time;
		public int ttl;
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
	public void setup(String key, String value) {
		if("queueSize".equals(key))
			queueSize = Integer.parseInt(value);
		else if("packetTTL".equals(key))
			packetTTL = Integer.parseInt(value);
		else if("delayHop".equals(key))
			delayHop = Long.parseLong(value);
		else if("energyHop".equals(key))
			energyHop = Double.parseDouble(value);
		else if("leakage".equals(key))
			leakage = Double.parseDouble(value);
	}
	
	@Override
	protected double getLeakage() {
		return leakage;
	}
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req, n, s, w, e};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done, link};
	}
	
	public boolean send(int sender, int req, long time, int ttl) {
		Packet p = new Packet();
		p.time = time;
		p.ttl = ttl;
		p.addr = Mem.getAddr(req);
		p.msg = Mem.makeReq(Mem.getCmd(req), sender);
		return send(p);
	}
	
	private boolean send(Packet p) {
		LinkedList<Packet> queue = null;
		long delay = delayHop;
		if(p.addr==config) {
			queue = inQueue;
			delay = 0L;
		}
		else if((p.addr & X_MASK) < (config & X_MASK))
			queue = wQueue;
		else if((p.addr & X_MASK) > (config & X_MASK))
			queue = eQueue;
		if(queue!=null && queue.size()<queueSize) {
			p.time += delay;
			queue.add(p);
			return true;
		}
		if(p.addr==config)
			return false;
		else if((p.addr & Y_MASK) < (config & Y_MASK))
			queue = nQueue;
		else if((p.addr & Y_MASK) > (config & Y_MASK))
			queue = sQueue;
		if(queue!=null && queue.size()<queueSize) {
			p.time += delay;
			queue.add(p);
			return true;
		}
		return false;
	}
	
	private void sendForward(LinkedList<Packet> queue, InPort<Integer> link, HiEstimation est) {
		if(queue.isEmpty())
			return;
		for(Packet p : queue) {
			p.ttl--;
			if(p.ttl<0)
				throw new RuntimeException("NoC congested to stall (hop)");
		}
		
		Packet p = queue.getFirst();
		NocRouter next = (NocRouter) link.getLinkedModule();
		if(next==null) {
			throw new RuntimeException("Bad route at "+addrString(config)+" for addr "+addrString(p.addr));
		}
		
		if(next.send(p)) {
			queue.remove(p);
			est.totalEnergy += energyHop;
		}
	}
	
	public static String addrString(int addr) {
		return String.format("%d[%d,%d]", addr, addr>>8, addr&0xff);
	}
	
	@Override
	protected long update(HiEstimation est) {
		sendForward(nQueue, n, est);
		sendForward(sQueue, s, est);
		sendForward(wQueue, w, est);
		sendForward(eQueue, e, est);
		if(!inQueue.isEmpty()) {
			Packet p = inQueue.removeFirst();
			done.value = p.msg;
			syncTime(p.time);
		}
		else {
			done.value = null;
		}
		if(req.getValue()!=null && Mem.getCmd(req.getValue())!=Mem.CMD_NONE) {
			if(!send(config, req.getValue(), getTime(), packetTTL))
				throw new RuntimeException("NoC congested to stall (send)");
		}
		return 0L;
	}
	
}
