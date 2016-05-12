package ncl.cs.prime.archon.arch;

public class OutPort<T> {

	private Module module;
	public T value;
	
	public OutPort(Module module, T init) {
		this.module = module;
		value = init;
	}
	
	public long getTime() {
		return module.getTime();
	}
	
	public long syncTime(long t) {
		return module.syncTime(t);
	}
		
}
