package ncl.cs.prime.archon.arch.modules.arm;

import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Alu extends ArmModule {

	private static final String[] CONFIG_NAMES = {
		"and", "eor", "sub", "rsb", "add", "adc", "sbc", "rsc", "tst", "teq", "cmp", "cmn", "orr", "mov", "bic", "mvn",
		"ands", "eors", "subs", "rsbs", "adds", "adcs", "sbcs", "rscs", "tsts", "teqs", "cmps", "cmns", "orrs", "movs", "bics", "mvns"
	};
	
	private static final long[] CONFIG_TIMES = {
		1, 1, 3, 3, 3, 3, 3, 3, 3, 2, 3, 3, 3, 1, 1, 1, 1
	};
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"n", "m"};
		d.outputNames = new String[] {"d"};
		d.flagNames = new String[] {"n", "z"};//, "c"};
		d.configNames = CONFIG_NAMES;
		return d;
	}
	
	protected InPort<Integer> n = new InPort<>(this);
	protected InPort<Integer> m = new InPort<>(this);
	protected OutPort<Integer> d = new OutPort<Integer>(this, -1);
	
	protected FlagOutPort fn = new FlagOutPort(this);
	protected FlagOutPort fz = new FlagOutPort(this);
	protected FlagOutPort fc = new FlagOutPort(this);
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {n, m};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {d};
	}

	@Override
	protected FlagOutPort[] initFlags() {
		return new FlagOutPort[] {fn, fz};
	}
	
	@Override
	protected String getResourceName() {
		return "alu-"+CONFIG_NAMES[config];
	}
	
	private long calc(int func, long x, long y) {
		switch(func) {
			case 0: // and
			case 8: // tst
				return x & y;
			case 1: // eor
			case 9: // teq
				return x ^ y;
			case 2: // sub
			case 10: // cmp
				return x - y;
			case 3: // rsb
				return y - x;
			case 4: // add
			case 11: // cmn
				return x + y;
			case 5: // adc
				return x + y + (fc.value ? 1 : 0);
			case 6: //sbc
				return x - y - 1 +(fc.value ? 1 : 0);
			case 7: // rsc
				return y - x - 1 + (fc.value ? 1 : 0);
			case 12: // orr
				return x | y;
			case 13: // mov
				return y;
			case 14:
				return x & ~y;
			case 15: // mvn
				return ~y;
			default:
				throw new UnsupportedOperationException(); // TODO proper operation exception
		}
	}
	
	@Override
	protected void initDelays() {
		delays = CONFIG_TIMES;
	}
	
	@Override
	protected void update() {
		long res = calc(config%16, (long) n.getValue(), (long) m.getValue());
		int intRes = (int) res;
		if(config/16>0 || (config>=8 && config<12)) {
			fz.value = (res==0);
			fn.value = (res<0);
			fc.value = res != (long) intRes;
		}
		if(config<8 || config>=12) {
			d.value = intRes;
		}
		super.update();
	}

}
