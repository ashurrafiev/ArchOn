package ncl.cs.prime.archon.arch;

public abstract class OutPort<T> {

	private Module module;
	public T value;
	
	public OutPort(Module module, T init) {
		this.module = module;
		value = init;
	}
	
	public Module getModule() {
		return module;
	}
	
	public long getTime() {
		return module.getTime();
	}
	
	public long syncTime(long t) {
		return module.syncTime(t);
	}
	
	public abstract void debugSetIntValue(int v);
	public abstract int debugGetIntValue();

	public static class Int extends OutPort<Integer> {
		public Int(Module module, Integer init) {
			super(module, init);
		}
		@Override
		public void debugSetIntValue(int v) {
			value = v;
		}
		@Override
		public int debugGetIntValue() {
			return value;
		}
	}
	
	public static class Bool extends OutPort<Boolean> {
		public Bool(Module module, Boolean init) {
			super(module, init);
		}
		@Override
		public void debugSetIntValue(int v) {
			value = v!=0;
		}
		@Override
		public int debugGetIntValue() {
			return value ? 1 : 0;
		}
	}
}
