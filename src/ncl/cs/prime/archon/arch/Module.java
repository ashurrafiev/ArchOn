package ncl.cs.prime.archon.arch;

public abstract class Module {
	
	public static class Declaration {
		public String[] inputNames = null;
		public String[] outputNames = null;
		public String[] flagNames = null;
		public String[] configNames = null;
	}
	
	private boolean invalidated = false;
	private long time = 0;

	private InPort<?>[] inputs;
	private OutPort<?>[] outputs;
	protected FlagOutPort[] flags;
	
	protected long delays[] = null;
	protected long defaultDelay = 0L;

	public int config;
	
	protected abstract InPort<?>[] initInputs();
	protected abstract OutPort<?>[] initOutputs();
	protected abstract void update();
	
	public void initPorts() {
		inputs = initInputs();
		outputs = initOutputs();
		flags = initFlags();
		initDelays();
	}
	
	protected void initDelays() {
	}
	
	protected FlagOutPort[] initFlags() {
		return null;
	}
	
	public InPort<?>[] getInputs() {
		return inputs;
	}
	
	public OutPort<?>[] getOutputs() {
		return outputs;
	}
	
	public FlagOutPort[] getFlags() {
		return flags;
	}
	
	public long getTime() {
		return time;
	}
	
	public long syncTime(long t) {
		if(t>time) time = t;
		return time;
	}
	
	protected long getDelay() {
		return (delays==null || config<0 || config>=delays.length) ? defaultDelay : delays[config];
	}
	
	protected void addDelay() {
		time += getDelay();
	}
	
	public void invalidate() {
		invalidated = true;
	}
	
	protected void estimate(Estimation est, boolean enabled) {
	}
	
	public void recompute(Estimation est) {
		if(inputs!=null) {
			for(InPort<?> in : inputs)
				if(in.isEnabled())
					syncTime(in.getTime());
		}
		if(est!=null)
			estimate(est, invalidated);
		if(invalidated) {
			invalidated = false;
			addDelay();
			update();
		}
	}
	
}
