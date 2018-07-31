package ncl.cs.prime.archon.arch.modules.hicore;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Core extends HiModule {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ack", "op"};
		d.outputNames = new String[] {"done", "mem_req"};
		return d;
	}	
	
	private static final long DELAY_CPU = 1L;
	private static final double ENERGY_CPU = 1.0;
	private static final double LEAKAGE = 1.0;
	
	private long delayCpu = DELAY_CPU;
	private double energyCpu = ENERGY_CPU;
	private double leakage = LEAKAGE;
	
	private InPort.Int ack = new InPort.Int(this);
	private InPort.Int op = new InPort.Int(this);
	private OutPort.Int done = new OutPort.Int(this, null);
	private OutPort.Int memReq = new OutPort.Int(this, null);

	public Core() {
		config = -1;
	}
	
	@Override
	public void setup(String key, String value) {
		if("delayCpu".equals(key))
			delayCpu = Long.parseLong(value);
		else if("energyCpu".equals(key))
			energyCpu = Double.parseDouble(value);
		else if("leakage".equals(key))
			leakage = Double.parseDouble(value);
	}
	
	@Override
	protected double getLeakage() {
		return leakage;
	}
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {ack, op};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done, memReq};
	}
	
	private long sendTime;

	@Override
	protected long update(HiEstimation est) {
		if(config<0) {
			config = Mem.getRandomPage();
		}
		
		if(op.getValue()==null || op.getValue()==App.OP_WAIT) {
			done.value = ack.getValue()==null ? null : Mem.getCmd(ack.getValue());
			memReq.value = null;
			if(done.value!=null) {
				est.totalMemWait += getTime()-sendTime;
				est.memAccesses++;
			}
			return 0L;
		}
		else if(op.getValue()==App.OP_CPU) {
			done.value = 1;
			memReq.value = null;
			est.totalEnergy += energyCpu;
			return delayCpu;
		}
		else {
			done.value = null;
			memReq.value = Mem.makeReq(op.getValue(), config);
			sendTime = getTime();
			return 0L;
		}
	}

}
