package ncl.cs.prime.archon.arch.modules.hicore;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.Module;

public abstract class HiModule extends Module {

	@Override
	protected long update() {
		return 0L;
	}
	
	protected abstract double getLeakage();
	
	@Override
	protected void estimate(Estimation est, boolean enabled) {
		((HiEstimation) est).collectLeakage(getLeakage());
	}
	
	protected abstract long update(HiEstimation est);
	
	@Override
	protected long update(Estimation est) {
		return update((HiEstimation) est);
	}
}
