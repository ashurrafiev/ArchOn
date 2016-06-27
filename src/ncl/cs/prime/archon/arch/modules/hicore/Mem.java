package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.ArrayList;
import java.util.Random;

import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class Mem extends HiModule {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"req"};
		d.outputNames = new String[] {"done"};
		return d;
	}	

	private static final long DELAY_READ = 6L;
	private static final long DELAY_WRITE = 6L;
	private static final double ENERGY_READ = 1.0;
	private static final double ENERGY_WRITE = 1.0;
	private static final double LEAKAGE = 1.0;

	public static final int CMD_NONE = 0;
	public static final int CMD_READ = 2;
	public static final int CMD_WRITE = 3;

	private long[] delays = {0L, 0L, DELAY_READ, DELAY_WRITE};
	private double[] energies = {0.0, 0.0, ENERGY_READ, ENERGY_WRITE};
	private double leakage = LEAKAGE;
	
	private static ArrayList<Integer> pageList = new ArrayList<>();
	private static final Random random = new Random();
	
	@Override
	public void setup(String key, String value) {
		if("pageAddr".equals(key))
			pageList.add(Integer.parseInt(value));
		else if("reset".equals(key))
			pageList.clear();
		else if("delayRead".equals(key))
			delays[CMD_READ] = Long.parseLong(value);
		else if("delayWrite".equals(key))
			delays[CMD_WRITE] = Long.parseLong(value);
		else if("energyRead".equals(key))
			energies[CMD_READ] = Double.parseDouble(value);
		else if("energyWrite".equals(key))
			energies[CMD_WRITE] = Double.parseDouble(value);
		else if("leakage".equals(key))
			leakage = Double.parseDouble(value);
	}
	
	@Override
	protected double getLeakage() {
		return leakage;
	}
	
	public static int getRandomPage() {
		return pageList.isEmpty() ? 0 : pageList.get(random.nextInt(pageList.size()));
	}
	
	public static int makeReq(int cmd, int addr) {
		return (cmd & 0x03) | ((addr & 0xffff) << 2);
	}
	
	public static int getAddr(int req) {
		return (req >> 2) & 0xffff;
	}

	public static int getCmd(int req) {
		return req & 0x03;
	}

	private InPort<Integer> req = new InPort<>(this);
	private OutPort<Integer> done = new OutPort<Integer>(this, null);

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {req};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {done};
	}
	
	@Override
	protected long update(HiEstimation est) {
		done.value = req.getValue();
		if(done.value!=null)
			est.totalEnergy += energies[getCmd(done.value)];
		return done.value==null ? 0L : delays[getCmd(done.value)];
	}

}
