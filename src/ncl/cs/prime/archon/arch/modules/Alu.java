package ncl.cs.prime.archon.arch.modules;

import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public abstract class Alu<T> extends Module {

	protected InPort<T> x = new InPort<>(this);
	protected InPort<T> y = new InPort<>(this);
	protected OutPort<T> out;

	public Alu(T init) {
		out = new OutPort<>(this, init);
	}
	
	protected abstract T calc(int func, T x, T y);

	@Override
	public InPort<?>[] initInputs() {
		return new InPort<?>[] {x, y};
	}
	
	@Override
	public OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {out};
	}
	
	protected FlagOutPort[] initFlags(int n) {
		if(n==0) {
			return null;
		}
		else {
			FlagOutPort[] f = new FlagOutPort[n];
			for(int i=0; i<n; i++) {
				f[i] = new FlagOutPort(this);
			}
			return f;
		}
	}
	
	protected void calcFlags(T out) {
	}
	
	@Override
	protected void update() {
		out.value = calc(config, x.getValue(), y.getValue());
		calcFlags(out.value);
	}
}
