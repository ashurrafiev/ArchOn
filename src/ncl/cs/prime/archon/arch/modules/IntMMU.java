package ncl.cs.prime.archon.arch.modules;

import ncl.cs.prime.archon.arch.Module;


public class IntMMU extends MMU<Integer, Integer> {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"addr", "data"};
		d.outputNames = new String[] {"data"};
		d.configNames = new String[] {"read", "write"};
		return d;
	}	
	
	public IntMMU() {
		super(-1);
	}

}
