package ncl.cs.prime.archon.arch.modules.mc51;

import ncl.cs.prime.archon.arch.Architecture;
import ncl.cs.prime.archon.arch.Estimation;

public class Estimate implements Estimation {

	public double cycleTime;
	public double cycleEnergy;
	
	public double spanTime;
	public double spanEnergy;
	
	public double totalTime;
	public double totalEnergy;
	
	@Override
	public void init(Architecture arch) {
		totalEnergy = 0.0;
		totalTime = 0.0;
	}

	@Override
	public void beginCycle() {
		cycleEnergy = 0.0;
		cycleTime = 0.0;
	}

	@Override
	public void endCycle() {
		totalEnergy += cycleEnergy;
		totalTime += cycleTime;
		spanEnergy += cycleEnergy;
		spanTime += cycleTime;
	}
	
	public void forModule(double time, double power) {
		if(time>cycleTime)
			cycleTime = time;
		cycleEnergy += power;
	}

	private void resetSpan() {
		spanTime = 0.0;
		spanEnergy = 0.0;
	}
	
	private void printParam(String name, double span, double total) {
		System.out.printf(" \t %10s \t| from last dump \t %4.4f \t| total \t %4.4f\n", name+":", span, total);
	}
	
	@Override
	public void dump() {
		System.out.println(" -- Physical parameters estimation --");
		printParam("Time", spanTime, totalTime);
		printParam("Energy", spanEnergy, totalEnergy);
		resetSpan();
	}

}
