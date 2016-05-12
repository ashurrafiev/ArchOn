package ncl.cs.prime.archon.arch;

public class FlagOutPort extends OutPort<Boolean> {

	public FlagOutPort(Module module) {
		super(module, false);
	}
}
