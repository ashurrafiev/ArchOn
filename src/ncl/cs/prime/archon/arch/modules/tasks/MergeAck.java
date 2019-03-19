package ncl.cs.prime.archon.arch.modules.tasks;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class MergeAck extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ack1", "ack2"};
		d.outputNames = new String[] {"ack"};
		return d;
	}	

	private InPort.Int[] ackIn = {new InPort.Int(this), new InPort.Int(this)};
	private OutPort.Int ackOut = new OutPort.Int(this, null);

	@Override
	protected InPort<?>[] initInputs() {
		return ackIn;
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {ackOut};
	}

	@Override
	protected long update() {
		return 0;
	}
	
	@Override
	protected long update(Estimation est) {
		if(ackIn[0].getValue()!=null)
			ackOut.value = ackIn[0].getValue();
		else if(ackIn[1].getValue()!=null)
			ackOut.value = ackIn[1].getValue();
		else
			ackOut.value = null;
		
		return 0L;
	}

}
