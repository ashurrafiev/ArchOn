package ncl.cs.prime.archon.arch.modules.hicore;

import ncl.cs.prime.archon.arch.Architecture;
import ncl.cs.prime.archon.arch.Estimation;

public class HiEstimation implements Estimation {

	public Architecture arch = null;;
	
	public long time = 0L;
	public double totalEnergy = 0.0;
	public double leakage = 0.0;
	
	public long totalMemWait = 0L;
	public int memAccesses = 0;
	
	private boolean collectLeakage = true;
	
	@Override
	public void init(Architecture arch) {
		this.arch = arch;
		MasterNode.total = 0L;
		MasterNode.counter = 0;
		SlaveNode.time = 0L;
		SlaveNode.counter = 0;
	}
	
	public void finish() {
		time = arch.syncTime();
	}
	
	@Override
	public void beginCycle() {
	}
	
	public void collectLeakage(double leakage) {
		if(collectLeakage)
			this.leakage += leakage;
	}
	
	@Override
	public void endCycle() {
		collectLeakage = false;
	}
	
	public double averageMemWait() {
		return memAccesses==0 ? 0.0 : totalMemWait / (double) memAccesses;
	}
	
	public double totalPower() {
		return leakage + totalEnergy / (double)time; 
	}
	
	@Override
	public void dump() {
		finish();
		System.out.printf("time: %d\tpower: %.3f\tactive: %.3f\tidle: %.3f\tmemWait: %.1f\n", time, totalPower(), totalEnergy / (double)time, leakage, averageMemWait());
	}
}
