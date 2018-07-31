package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.Random;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Cache extends HiModule {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req", "mem_ack"};
		d.outputNames = new String[] {"done", "mem_req"};
		return d;
	}	

	private static final double MISS_RATE = 0.5;
	private static final long DELAY_HIT = 1L;
	private static final double ENERGY_HIT = 1.0;
	private static final double ENERGY_MISS = 0.0;
	private static final double LEAKAGE = 1.0;
	
	private double missRate = MISS_RATE;
	private long delayHit = DELAY_HIT;
	private double energyHit = ENERGY_HIT;
	private double energyMiss = ENERGY_MISS;
	private double leakage = LEAKAGE;

	private InPort.Int req = new InPort.Int(this);
	private InPort.Int memAck = new InPort.Int(this);
	private OutPort.Int done = new OutPort.Int(this, null);
	private OutPort.Int memReq = new OutPort.Int(this, null);

	@Override
	public void setup(String key, String value) {
		if("missRate".equals(key))
			missRate = Double.parseDouble(value);
		else if("delayHit".equals(key))
			delayHit = Long.parseLong(value);
		else if("energyHit".equals(key))
			energyHit = Double.parseDouble(value);
		else if("energyMiss".equals(key))
			energyMiss = Double.parseDouble(value);
		else if("leakage".equals(key))
			leakage = Double.parseDouble(value);
	}
	
	@Override
	public void setConfig(int config) {
		missRate = (double)config / 100.0;
	}
	
	@Override
	protected double getLeakage() {
		return leakage;
	}
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req, memAck};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done, memReq};
	}
	
	private static final Random RANDOM = new Random();
	
	private boolean blocked = false;
	private boolean firstMiss = false;
	
	@Override
	protected long update(HiEstimation est) {
		boolean miss = firstMiss || RANDOM.nextDouble() < missRate;
		if(blocked) {
			if(memAck.getValue()!=null && Mem.getCmd(memAck.getValue())!=Mem.CMD_NONE) {
				done.value = memAck.getValue();
				blocked = false;
			}
			else {
				done.value = null;
			}
			memReq.value = null;
			return 0L;
		}
		else if(miss) {
			firstMiss = false;
			if(req.getValue()!=null && Mem.getCmd(req.getValue())!=Mem.CMD_NONE) {
				memReq.value = req.getValue();
				blocked = true;
				est.totalEnergy += energyMiss;
			}
			else {
				memReq.value = null;
			}
			done.value = null;
			return 0L;
		}
		else {
			done.value = req.getValue();
			memReq.value = null;
			if(done.value!=null)
				est.totalEnergy += energyHit;
			return done.value==null ? 0L : delayHit;
		}
	}

}
