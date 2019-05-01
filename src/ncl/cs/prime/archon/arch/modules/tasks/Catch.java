package ncl.cs.prime.archon.arch.modules.tasks;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Catch extends Module {

	public static final int EXCEPTIONS = 20; 

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ex"};
		d.outputNames = new String[EXCEPTIONS];
		for(int i=0; i<EXCEPTIONS; i++) {
			d.outputNames[i] = "catch"+(i+1);
		}
		return d;
	}	

	public double power = 1f;
	public long delay = 0L;

	private OutPort.Int[] handle;
	private InPort.Int ex = new InPort.Int(this);

	@Override
	public void setup(String key, String value) {
		if("delay".equals(key))
			delay = Long.parseLong(value);
		else if("power".equals(key))
			power = Double.parseDouble(value);
	}

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {ex};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		handle = new OutPort.Int[EXCEPTIONS];
		for(int i=0; i<EXCEPTIONS; i++)
			handle[i] = new OutPort.Int(this, null);
		return handle;

	}

	@Override
	protected long update() {
		return 0;
	}
	
	@Override
	protected long update(Estimation est) {
		TaskEstimation e = (TaskEstimation) est;

		for(int i=0; i<EXCEPTIONS; i++)
			handle[i].value = null;
		
		long t = 0L;
		if(ex.getValue()!=null) {
			int exId = ex.getValue()-1;
			//System.out.printf("@%d caught %d (battery=%.3f)\n", getTime(), exId, e.battery);
			handle[exId].value = 1;
			t = delay;
		}
		
		e.useEnergy(name, power * t / 1000.0);
		return t;
	}

}
