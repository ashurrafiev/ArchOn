package ncl.cs.prime.archon.arch.modules.tasks;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.Module;

public abstract class FinishableModule extends Module {
	
	public static long simulate = 3600L*1000L;
	
	protected FlagOutPort finished = new FlagOutPort(this);

	@Override
	public void setup(String key, String value) {
		if("simulate".equals(key))
			simulate = Long.parseLong(value)*1000L;
	}
	
	@Override
	protected FlagOutPort[] initFlags() {
		return new FlagOutPort[] {finished};
	}

	@Override
	protected final long update() {
		return 0;
	}
	
	@Override
	protected final long update(Estimation est) {
		TaskEstimation e = (TaskEstimation) est;
		
		if(getTime()>=simulate) {
			finished.value = true;
			return 0L;
		}
		else {
			finished.value = false;
			return updateLive(e);
		}
	}
	
	protected abstract long updateLive(TaskEstimation e);

}
