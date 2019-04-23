package ncl.cs.prime.archon.arch.modules.tasks;

import java.util.HashMap;
import java.util.Map.Entry;

import ncl.cs.prime.archon.arch.Architecture;
import ncl.cs.prime.archon.arch.Estimation;

public class TaskEstimation implements Estimation {

	public Architecture arch = null;

	public long time;
	public long numUserCommands;
	public long responseTime;

	private double energy;
	private HashMap<String, Double> energyPerModule = new HashMap<>();

	@Override
	public void init(Architecture arch) {
		this.arch = arch;
		this.numUserCommands = 0;
		this.responseTime = 0;
		
		this.energy = 0.0;
		this.energyPerModule.clear();
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
	}
	
	public void useEnergy(String name, double e) {
		energy += e;
		Double e0 = energyPerModule.get(name);
		if(e0!=null)
			e += e0;
		energyPerModule.put(name, e);
	}
	
	@Override
	public void dump() {
		time = arch.syncTime();
		String tstr = String.format("%dh %dmin %ds", time/1000L/3600L, (time/1000L/60L)%60L, (time/1000L)%60L);
		System.out.printf("Total time: %d (%s)\nUser commands: %d\nMean response time: %d ms\nTotal energy: %.3f Ws\n\n", time, tstr, numUserCommands,
				numUserCommands==0 ? 0 : responseTime/numUserCommands, energy);
		
		System.out.println("Energy per component:\n");
		for(Entry<String, Double> e : energyPerModule.entrySet()) {
			System.out.printf("%s: %.3f\n", e.getKey(), e.getValue());
		}
	}

}
