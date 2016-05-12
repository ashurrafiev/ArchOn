package ncl.cs.prime.archon.arch.modules.arm;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.Module;

public abstract class ArmModule extends Module {

	protected abstract String getResourceName();
	
	public abstract long getDuration();
	
	@Override
	protected void update() {
		addTime(getDuration());
	}
	
	@Override
	protected void estimate(Estimation est, boolean enabled) {
		if(enabled)
			((ArmEstimation) est).forModule(getResourceName());
	}
	
}
