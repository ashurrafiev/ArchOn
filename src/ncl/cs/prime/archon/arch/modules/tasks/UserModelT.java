package ncl.cs.prime.archon.arch.modules.tasks;

import java.util.Random;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class UserModelT extends Module {

	public static final int TASKS = 5; 
	
	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[TASKS];
		d.outputNames = new String[TASKS];
		for(int i=0; i<TASKS; i++) {
			d.inputNames[i] = "ack"+(i+1);
			d.outputNames[i] = "req"+(i+1);
		}
		d.flagNames = new String[] {"depleted"};
		return d;
	}

	private static final Random RANDOM = new Random();
	
	public double battery = 10.0;
	public double idlePower = 0;
	public double sleepPower = 0;
	public long outerDelayMean = 0L;
	public long delayMean = 5000L;
	public long delaySDev = 1000L;
	public double pTask[] = new double[TASKS];

	private OutPort.Int[] req;
	private InPort.Int[] ack;
	protected FlagOutPort depleted = new FlagOutPort(this);

	@Override
	public void setup(String key, String value) {
		if(key.startsWith("p_task")) {
			int index = Integer.parseInt(key.substring(6, key.length()))-1;
			pTask[index] = Double.parseDouble(value);
		}
		else if("delay_mean".equals(key))
			delayMean = Long.parseLong(value);
		else if("delay_sdev".equals(key))
			delaySDev = Long.parseLong(value);
		else if("idle_power".equals(key))
			idlePower = Double.parseDouble(value);
		else if("battery".equals(key))
			battery = Double.parseDouble(value);
		else if("outer_delay_mean".equals(key))
			outerDelayMean = Long.parseLong(value);
		else if("sleep_power".equals(key))
			sleepPower = Double.parseDouble(value);
	}

	@Override
	protected InPort<?>[] initInputs() {
		ack = new InPort.Int[TASKS];
		for(int i=0; i<TASKS; i++)
			ack[i] = new InPort.Int(this);
		return ack;
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		req = new OutPort.Int[TASKS];
		for(int i=0; i<TASKS; i++)
			req[i] = new OutPort.Int(this, null);
		return req;
	}

	@Override
	protected FlagOutPort[] initFlags() {
		return new FlagOutPort[] {depleted};
	}

	@Override
	protected long update() {
		return 0;
	}
	
	protected long reqTime = 0L;
	
	protected long getDelay() {
		return (long)(RANDOM.nextGaussian()*delaySDev + delayMean);
	}
	
	protected long getOuterDelay() {
		return outerDelayMean>0 ? (long)Math.log(1-RANDOM.nextDouble())/(-outerDelayMean) : 0L;
	}
	
	@Override
	protected long update(Estimation est) {
		TaskEstimation e = (TaskEstimation) est;
		
		long t = 0L;
		
		boolean ackAny = false;
		for(int i=0; i<TASKS; i++) {
			ackAny |= (ack[i].getValue()!=null && ack[i].getValue()!=0);
		}
		
		if(ackAny || reqTime==0L) {
			if(getTime()>0)
				e.addResponse(getTime() - reqTime);
			else
				e.battery = battery;
			depleted.value = e.battery<=0;
			if(!depleted.value) {
				int task = reqTime>0L ? weightedRandom(RANDOM, pTask) : 0;
				req[task].value = 1;
				t = getDelay();
				e.battery -= idlePower * t / 1000.0;
				if(task==0) {
					long st = getOuterDelay();
					e.battery -= sleepPower * st / 1000.0;
					t += st;
				}
				reqTime = getTime()+t;
			}
		}
		else {
			for(int i=0; i<TASKS; i++)
				req[i].value = null;
		}
		return t;
	}
	
	public static int weightedRandom(Random random, double[] w) {
		double max = 0;
		for(int i = 0; i < w.length; i++)
			max += w[i];
		if(max == 0)
			return 0;
		double x = random.nextDouble()*max;
		for(int i = 0;; i++) {
			if(x < w[i])
				return i;
			x -= w[i];
		}
	}
	
	public static void main(String[] args) {
		int dummies = 0;
		if(args.length>0)
			dummies = Integer.parseInt(args[0]);
		if(dummies>TASKS-1)
			dummies = TASKS-1;
		
		System.out.println("#aliaspk \"ncl.cs.prime.archon.arch.modules.tasks\"");
		System.out.println();
		System.out.println("#estim \".TaskEstimation\"");
		System.out.println();
		System.out.println("#assign User \".UserModelT\" // UserModelT.TASKS = "+TASKS);
		System.out.printf("// Template for %d tasks and %d dummies\n", TASKS-dummies, dummies);
		
		StringBuilder setup = new StringBuilder("delay_mean:5000; delay_sdev:1000; idle_power:0; battery:10");
		for(int i=1; i<=TASKS; i++) {
			setup.append(String.format("; p_task%d:%.3f", i,
					(i > TASKS-dummies) ? 0.0 : 1.0/(double)(TASKS-dummies)));
		}
		System.out.printf("#setup User \"%s\"\n", setup);
		System.out.println();

		for(int i=1; i<=TASKS; i++) {
			if(i > TASKS-dummies) { // dummy
				System.out.printf("// ------- DUMMY -------\n", i);
				System.out.println();
				
				System.out.printf("User.ack%d = User.req%d\n", i, i);
				System.out.println();
			}
			else { // task
				System.out.printf("// ------- TASK #%d -------\n", i);
				System.out.println();
				
				System.out.printf("#assign Task%d \".Task\"\n", i);
				System.out.printf("#setup Task%d \"pre_delay:0; post_delay:10; power:1\"\n", i);
				System.out.println();
				
				System.out.printf("Task%d.req = User.req%d\n", i, i);
				System.out.printf("User.ack%d = Task%d.ack\n", i, i);
				System.out.println();
				
				System.out.printf("Task%d.next_ack = Task%d.next_req\n", i, i);
				System.out.println();
			}
		}

		System.out.println();
		System.out.println("// Simulation cycles until battery depleted");
		System.out.println("@loop");
		System.out.println("!");
		System.out.println("[^User.depleted] #jump @loop");
		System.out.println();
		System.out.println("#estprint");
		System.out.println("!stop");
	}
}
