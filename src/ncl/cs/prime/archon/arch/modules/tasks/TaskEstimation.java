package ncl.cs.prime.archon.arch.modules.tasks;

import ncl.cs.prime.archon.arch.Architecture;
import ncl.cs.prime.archon.arch.Estimation;

public class TaskEstimation implements Estimation {

	public Architecture arch = null;

	public long time;
	public double battery;
	public long numUserCommands;
	public long responseTime;
	
	@Override
	public void init(Architecture arch) {
		this.arch = arch;
		this.battery = 0.0;
		this.numUserCommands = 0;
		this.responseTime = 0;
	}

	@Override
	public void beginCycle() {
	}

	@Override
	public void endCycle() {
	}

	public void addResponse(long time) {
		numUserCommands++;
		responseTime += time;
		//System.out.println(time);
	}
	
	@Override
	public void dump() {
		time = arch.syncTime();
		String tstr = String.format("%dh %dmin %ds", time/1000L/3600L, (time/1000L/60L)%60L, (time/1000L)%60L);
		System.out.printf("Total time: %d (%s)\nUser commands: %d\nMean response time: %d ms\nBattery:%.3f Ws\n", time, tstr, numUserCommands,
				numUserCommands==0 ? 0 : responseTime/numUserCommands, battery);
	}

}
