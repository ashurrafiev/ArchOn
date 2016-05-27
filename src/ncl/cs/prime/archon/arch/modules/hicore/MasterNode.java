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
	
	private InPort<Integer> req = new InPort<>(this);
	private InPort<Integer> link = new InPort<>(this);
	private OutPort<Integer> done = new OutPort<Integer>(this, null);

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req, link};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done};
	}
	
	@Override
	protected long update() {
		SlaveNode bus = (SlaveNode) link.getLinkedModule();
		if(bus.sender==config) {
			done.value = bus.msg;
			bus.accept(config);
		}
		else {
			done.value = null; // Mem.REQ_NONE;
		}
		if(req.getValue()!=null && Mem.getCmd(req.getValue())!=Mem.CMD_NONE) {
			bus.send(config, req.getValue());
		}
		return 1L;
	}

}
