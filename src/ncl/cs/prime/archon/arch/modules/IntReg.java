package ncl.cs.prime.archon.arch.modules;

import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class IntReg extends Reg<Integer> {
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"in"};
		d.outputNames = new String[] {"out"};
		return d;
	}
	
	public IntReg() {
		super(0);
	}
	
	@Override
	protected OutPort<Integer> createOut(Integer init) {
		return new OutPort.Int(this, init);
	}
}