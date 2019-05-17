package ncl.cs.prime.archon.arch.modules.tasks;

import java.util.Random;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class FaultyTask extends Task {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req", "nextAck"};
		d.outputNames = new String[] {"ack", "nextReq", "ex"};
		return d;
	}
	
	public double pEx[] = new double[Catch.EXCEPTIONS];
	
	protected OutPort.Int ex = new OutPort.Int(this, null);
	
	@Override
	public void setup(String key, String value) {
		if(key.startsWith("pEx")) {
			int index = Integer.parseInt(key.substring(3, key.length()))-1;
			pEx[index] = Double.parseDouble(value);
		}
		else
			super.setup(key, value);
	}
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req, nextAck};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {ack, nextReq, ex};
	}
	
	private static final Random RANDOM = new Random();
	
	private int injectException() {
		double x = RANDOM.nextDouble();
		for(int i=0; i<Catch.EXCEPTIONS; i++) {
			if(x<pEx[i])
				return i;
			else
				x -= pEx[i];
		}
		return -1;
	}
	
	@Override
	protected long update(Estimation est) {
		TaskEstimation e = (TaskEstimation) est;

		if(req.getValue()!=null) {
			ack.value = null;
			int exId = injectException();
			if(exId>=0) {
				nextReq.value = null;
				ex.value = exId+1;
				e.countException(name, ex.value);
				
				long t = preDelay;
				e.useEnergy(name, true, power * t / 1000.0);
				return t;
			}
		}
		
		ex.value = null;
		return super.update(est);
	}
}
