package ncl.cs.prime.archon.arch;

public class InPort<T> {

	private OutPort<T> connection = null;
	private FlagOutPort condition = null;
	private boolean negCondition;
	
	private Module module;
	private T value;

	public InPort(Module module) {
		this.module = module;
	}
	
	@SuppressWarnings("unchecked")
	public void connect(OutPort<?> p) {
		connection = (OutPort<T>) p;
	}
	
	@SuppressWarnings("unchecked")
	public void connect(OutPort<?> p, FlagOutPort cond, boolean neg) {
		connection = (OutPort<T>) p;
		condition = cond;
		negCondition = neg;
	}
	
	public void disconnect() {
		connection = null;
	}
	
	public boolean isEnabled() {
		if(connection==null)
			return false;
		else if(condition!=null && !(condition.value^negCondition))
			return false;
		else
			return true;
	}
	
	public long getTime() {
		return connection.getTime();
	}
	
	public boolean pullData() {
		if(isEnabled()) {
			value = connection.value;
			connection.syncTime(module.getTime());
			module.invalidate();
			return true;
		}
		else {
			return false;
		}
	}
	
	public T getValue() {
		return value;
	}
	
}
