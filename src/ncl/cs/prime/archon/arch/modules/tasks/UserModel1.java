package ncl.cs.prime.archon.arch.modules.tasks;

import java.util.Random;

import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.FlagOutPort;
import ncl.cs.prime.archon.arch.InPort;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.OutPort;

public class UserModel1 extends Module {

	public static Module.Declaration getDeclaration() {
		Declaration d = new Declaration();
		d.inputNames = new String[] {"ack"};
		d.outputNames = new String[] {"req"};
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
	public double probOut = 0;

	private OutPort.Int req = new OutPort.Int(this, null);
	private InPort.Int ack = new InPort.Int(this);
	protected FlagOutPort depleted = new FlagOutPort(this);

	@Override
	public void setup(String key, String value) {
		if("delay_mean".equals(key))
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
		else if("prob_out".equals(key))
			probOut = Double.parseDouble(value);
	}

	@Override
	protected InPort<?>[] initInputs() {
		return new InPort<?>[] {ack};
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return new OutPort<?>[] {req};
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
		if(ack.getValue()!=null && ack.getValue()!=0 || reqTime==0L) {
			if(getTime()>0)
				e.addResponse(getTime() - reqTime);
			else
				e.battery = battery;
			depleted.value = e.battery<=0;
			if(!depleted.value) {
				req.value = 1;
				t = getDelay();
				e.battery -= idlePower * t / 1000.0;
				if(RANDOM.nextDouble()<probOut || reqTime==0L) {
					long st = getOuterDelay();
					e.battery -= sleepPower * st / 1000.0;
					t += st;
				}
				reqTime = getTime()+t;
			}
		}
		else
			req.value = null;
		return t;
	}
	
}
