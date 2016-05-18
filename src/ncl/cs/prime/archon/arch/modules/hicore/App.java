package ncl.cs.prime.archon.arch.modules.hicore;

import java.util.Random;

import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class App extends Module {

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
	private static final int PROBABILITIES[] = {0, 4718597, 3342340, 65536};
	
	private InPort<Integer> ack = new InPort<>(this);
	private OutPort<Integer> op = new OutPort<Integer>(this, null);
	private OutPort<Integer> counter = new OutPort<Integer>(this, -1);
	protected FlagOutPort done = new FlagOutPort(this);
	
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
	protected long update() {
		if(ack.getValue()!=null && ack.getValue()!=0) {
			if(counter.value>0)
				counter.value--;
			done.value = (counter.value==0);
			if(counter.value!=0) {
				op.value = weightedRandom(RANDOM, PROBABILITIES);
			}
		}
		else
			op.value = null; // OP_WAIT;
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
