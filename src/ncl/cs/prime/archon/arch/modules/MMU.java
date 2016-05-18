package ncl.cs.prime.archon.arch.modules;

import java.util.HashMap;
import java.util.Map;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class MMU<T, U> extends Module {

	private Map<T, U> memory;
	
	private InPort<T> address;
	private InPort<U> data;
	private OutPort<U> out;
	
	public MMU(U defaultValue) {
		address = new InPort<T>(this);
		data = new InPort<U>(this);
		
		out = new OutPort<U>(this, defaultValue);
		
		memory = new HashMap<T, U>();
	}
	
	@Override
	public InPort<?>[] initInputs() {
		return new InPort<?>[] {address, data};
	}
	
	@Override
	public OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}

	@Override
	protected long update() {
		if (config==0) { // read mode
			T addr = address.getValue();
			out.value = memory.get(addr);
		} else { // write mode
			T addr = address.getValue();
			U val = data.getValue();
			memory.put(addr, val);
		}
		return 0L;
	}

}
