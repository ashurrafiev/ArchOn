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
	
	protected int config = 0;
	
	protected String name = "";
	
	protected abstract InPort<?>[] initInputs();
	protected abstract OutPort<?>[] initOutputs();
	protected abstract long update();
	
	public void setup(String key, String value) {
	}
	
	public void setup(String keyValues) {
		if(keyValues==null || keyValues.isEmpty())
			return;
		String[] pairs = keyValues.split("\\s*[\\;\\,]\\s*");
		for(String p : pairs) {
			if(p.isEmpty())
				continue;
			String[] s = p.split("\\s*[\\:\\=]\\s*", 2);
			setup(s[0], s.length<2 ? null : s[1]);
		}
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void initPorts() {
		inputs = initInputs();
		outputs = initOutputs();
		flags = initFlags();
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
	
	public void setConfig(int config) {
		this.config = config;
	}
	
	public long getTime() {
		return time;
	}
	
	public long syncTime(long t) {
		if(t>time) time = t;
		return time;
	}
	
	public void invalidate() {
		invalidated = true;
	}
	
	protected void estimate(Estimation est, boolean enabled) {
	}
	
	protected long update(Estimation est) {
		return update();
	}
	
	public void recompute(Estimation est) {
		if(inputs!=null) {
			for(InPort<?> in : inputs)
				if(in.isEnabled() && in.hasData())
					syncTime(in.getTime());
		}
		if(est!=null)
			estimate(est, invalidated);
		if(invalidated) {
			invalidated = false;
			
			// Important!
			// Saving to temporary variable is required
			// because update() may change the value of time.
			// Mutability is the root of evil...
			long delay = update(est);
			time += delay;
		}
	}
	
}
