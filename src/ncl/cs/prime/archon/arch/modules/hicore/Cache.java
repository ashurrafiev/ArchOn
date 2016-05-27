package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.Random;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Cache extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req", "mem_ack"};
		d.outputNames = new String[] {"done", "mem_req"};
		return d;
	}	

	private InPort<Integer> req = new InPort<>(this);
	private InPort<Integer> memAck = new InPort<>(this);
	private OutPort<Integer> done = new OutPort<Integer>(this, null);
	private OutPort<Integer> memReq = new OutPort<Integer>(this, null);

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
	protected long update() {
		boolean miss = firstMiss || RANDOM.nextInt(100) < config;
		if(blocked) {
			if(memAck.getValue()!=null && Mem.getCmd(memAck.getValue())!=Mem.CMD_NONE) {
				done.value = memAck.getValue();
				blocked = false;
			}
			else {
				done.value = null;
			}
			memReq.value = null;
			return done.value==null ? 0L : 1L;
		}
		else if(miss) {
			firstMiss = false;
			if(req.getValue()!=null && Mem.getCmd(req.getValue())!=Mem.CMD_NONE) {
				memReq.value = req.getValue();
				blocked = true;
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
			return done.value==null ? 0L : 1L;
		}
	}

}
