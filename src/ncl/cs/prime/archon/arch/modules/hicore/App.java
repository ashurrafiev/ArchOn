package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.Random;

import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class App extends HiModule {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ack"};
		d.outputNames = new String[] {"op", "c"};
		d.flagNames = new String[] {"done"};
		return d;
	}	
	
	public static final int OP_WAIT = 0;
	public static final int OP_CPU = 1;
	public static final int OP_MEM_READ = 2;
	public static final int OP_MEM_WRITE = 3;
	
	private static final Random RANDOM = new Random(); // change to global random with seed?
	private static final int PROBABILITIES[] = {0, 4718597, 65536*18 /*3342340*/, 65536};
	
	private int probs[] = new int[] {0, PROBABILITIES[OP_CPU], PROBABILITIES[OP_MEM_READ], PROBABILITIES[OP_MEM_WRITE]};
	
	private InPort.Int ack = new InPort.Int(this);
	private OutPort.Int op = new OutPort.Int(this, null);
	private OutPort.Int counter = new OutPort.Int(this, -1);
	protected FlagOutPort done = new FlagOutPort(this);
	
	@Override
	public void setup(String key, String value) {
		if("countCpu".equals(key))
			probs[OP_CPU] = Integer.parseInt(value);
		else if("countMemRead".equals(key))
			probs[OP_MEM_READ] = Integer.parseInt(value);
		else if("countMemWrite".equals(key))
			probs[OP_MEM_WRITE] = Integer.parseInt(value);
	}
	
	@Override
	protected double getLeakage() {
		return 0.0;
	}
	
	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {ack};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {op, counter};
	}

	@Override
	protected FlagOutPort[] initFlags() {
		return new FlagOutPort[] {done};
	}
	
	@Override
	protected long update(HiEstimation est) {
		if(ack.getValue()!=null && ack.getValue()!=0) {
			if(counter.value>=0) {
				counter.value--;
			}
			done.value = (counter.value<0);
			if(counter.value>=0) {
				op.value = weightedRandom(RANDOM, probs);
			}
		}
		else
			op.value = null;
		return 0L;
	}
	
	public static int weightedRandom(Random random, int[] w) {
		int max = 0;
		for(int i = 0; i < w.length; i++)
			max += w[i];
		if(max == 0)
			return 0;
		int x = random.nextInt(max);
		for(int i = 0;; i++) {
			if(x < w[i])
				return i;
			x -= w[i];
		}
	}

}
