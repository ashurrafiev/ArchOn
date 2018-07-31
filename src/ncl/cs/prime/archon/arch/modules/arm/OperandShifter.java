package ncl.cs.prime.archon.arch.modules.arm;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class OperandShifter extends ArmModule {

	public static final long TIME = 2;
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"m", "shift"};
		d.outputNames = new String[] {"d"};
		d.configNames = new String[] {"lsl", "lsr", "asr", "ror"};
		return d;
	}
	
	protected InPort.Int m = new InPort.Int(this);
	protected InPort.Int shift = new InPort.Int(this);
	protected OutPort.Int d = new OutPort.Int(this, -1);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {m, shift};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {d};
	}
	
	@Override
	protected String getResourceName() {
		return "sh";
	}

	private Integer calc(int func, Integer x, Integer shift) {
		switch(func) {
			case 0: // lsl
				return x << shift;
			default:
				throw new UnsupportedOperationException(); // TODO proper operation exception
		}
	}

	@Override
	protected long update() {
		d.value = calc(config, m.getValue(), shift.getValue());
		return TIME;
	}

}
