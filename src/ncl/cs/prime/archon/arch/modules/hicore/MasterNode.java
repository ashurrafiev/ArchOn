package ncl.cs.prime.archon.arch.modules.hicore;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class MasterNode extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req", "link"};
		d.outputNames = new String[] {"done"};
		return d;
	}	
	
	private InPort.Int req = new InPort.Int(this);
	private InPort.Int link = new InPort.Int(this);
	private OutPort.Int done = new OutPort.Int(this, null);

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req, link};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done};
	}
	
	private long sendTime;
	public static long total = 0L;
	public static int counter = 0;
	
	@Override
	protected long update() {
		SlaveNode bus = (SlaveNode) link.getLinkedModule();
		if(bus.sender==config) {
			done.value = bus.msg;
			bus.accept(config);
			syncTime(bus.getTime());
			total += getTime()-sendTime;
			counter++;
		}
		else {
			done.value = null;
		}
		if(req.getValue()!=null && Mem.getCmd(req.getValue())!=Mem.CMD_NONE) {
			sendTime = getTime();
			bus.send(config, req.getValue(), sendTime);
		}
		return 0L; // bus delay is managed in the SlaveNode
	}

}
