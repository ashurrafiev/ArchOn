package ncl.cs.prime.archon.arch.modules.tasks;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Invoker extends FinishableModule {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ack"};
		d.outputNames = new String[] {"req"};
		d.flagNames = new String[] {"finished"};
		return d;
	}

	public long period = 1000L;

	private OutPort.Int req = new OutPort.Int(this, null);
	private InPort.Int ack = new InPort.Int(this);

	@Override
	public void setup(String key, String value) {
		if("period".equals(key))
			period = Long.parseLong(value);
		else
			super.setup(key, value);
	}

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {ack};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {req};
	}

	protected long reqTime = 0L;
	
	@Override
	protected long updateLive(TaskEstimation e) {
		long t = 0L;
		if(ack.getValue()!=null && ack.getValue()!=0 || reqTime==0L) {
			req.value = 1;
			t = period;
			reqTime = getTime()+t;
		}
		else
			req.value = null;
		return t;
	}

}
