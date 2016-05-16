package ncl.cs.prime.archon.arch.modules.arm;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Reg extends ArmModule {

	public static final long TIME = 1;
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"n"};
		d.outputNames = new String[] {"d"};
		return d;
	}
	
	protected InPort<Integer> n = new InPort<>(this);
	protected OutPort<Integer> d = new OutPort<Integer>(this, -1);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {n};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {d};
	}
	
	@Override
	protected String getResourceName() {
		return "reg";
	}

	@Override
	protected void initDelays() {
		defaultDelay = TIME;
	}
	
	@Override
	protected void update() {
		d.value = n.getValue();
		super.update();
	}

}
