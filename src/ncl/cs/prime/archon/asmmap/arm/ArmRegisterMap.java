package ncl.cs.prime.archon.asmmap.arm;

import ncl.cs.prime.archon.asmmap.RegisterMap;

public class ArmRegisterMap extends RegisterMap {

	private static final int NUM_REGS = 12;
	private static final String[] REGS = new String[NUM_REGS];
	{
		for(int i=0; i<NUM_REGS; i++)
			REGS[i] = "R"+i;
	}
	
	@Override
	public String[] getRegisterNames() {
		return REGS;
	}

}
