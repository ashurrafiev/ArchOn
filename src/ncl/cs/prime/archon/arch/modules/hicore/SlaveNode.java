package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.Collections;
import java.util.LinkedList;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class SlaveNode extends HiModule {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ack"};
		d.outputNames = new String[] {"mem_req", "link"};
		return d;
	}	

	private static final long DELAY_HOP = 4L;
	private static final double ENERGY_HOP = 1.0;
	private static final double LEAKAGE = 1.0;
	
	private long delayHop = DELAY_HOP;
	private double energyHop = ENERGY_HOP;
	private double leakage = LEAKAGE;
	
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
	public void setup(String key, String value) {
		if("delayHop".equals(key))
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
		p.time = time;
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
	protected long update(HiEstimation est) {
		long delay = 0L;
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
			time = getTime() + delayHop;
			counter++;
			est.totalEnergy += energyHop;
			delay = delayHop;
		}
		else {
			memReq.value = null;
		}
		return delay;
	}

}
