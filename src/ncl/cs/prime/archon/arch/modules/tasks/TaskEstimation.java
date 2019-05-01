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

	private int exceptions;
	private HashMap<String, Integer> exceptionsPerModule = new HashMap<>();

	private HashMap<String, Integer> callsPerType = new HashMap<>();

	@Override
	public void init(Architecture arch) {
		this.arch = arch;
		this.numUserCommands = 0;
		this.responseTime = 0;
		
		this.energy = 0.0;
		this.energyPerModule.clear();
		this.exceptions = 0;
		this.exceptionsPerModule.clear();
		this.callsPerType.clear();
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
	
	public void countException(String name, int ex) {
		exceptions++;
		String key = String.format("%s (ex%d)", name, ex);
		Integer c0 = exceptionsPerModule.get(key);
		int c = (c0!=null) ? c0+1 : 1;
		exceptionsPerModule.put(key, c);
	}
	
	public void countTaskType(String type) {
		if(type==null || type.isEmpty())
			return;
		Integer c0 = callsPerType.get(type);
		int c = (c0!=null) ? c0+1 : 1;
		callsPerType.put(type, c);
	}
	
	@Override
	public void dump() {
		time = arch.syncTime();
		String tstr = String.format("%dh %dmin %ds", time/1000L/3600L, (time/1000L/60L)%60L, (time/1000L)%60L);
		System.out.printf("{\n\"Total time\": %d,\n"
				+ "\"Total time (str)\": \"%s\",\n"
				+ "\"User commands\": %d,\n"
				+ "\"Mean response time\": %d,\n"
				+ "\"Total energy\": %.3f,\n"
				+ "\"Total exceptions\": %d,\n",
				time, tstr, numUserCommands,
				numUserCommands==0 ? 0 : responseTime/numUserCommands,
				energy, exceptions
			);
		
		System.out.println("\n\"Energy per component\": {");
		boolean first = true;
		for(Entry<String, Double> e : energyPerModule.entrySet()) {
			if(!first)
				System.out.println(",");
			first = false;
			System.out.printf("\t\"%s\": %.3f", e.getKey(), e.getValue());
		}
		System.out.println("\n},");
		
		System.out.println("\n\"Calls per task type\": {");
		first = true;
		for(Entry<String, Integer> e : callsPerType.entrySet()) {
			if(!first)
				System.out.println(",");
			first = false;
			System.out.printf("\t\"%s\": %d", e.getKey(), e.getValue());
		}
		System.out.println("\n},");
		
		System.out.println("\n\"Exceptions per component\": {");
		first = true;
		for(Entry<String, Integer> e : exceptionsPerModule.entrySet()) {
			if(!first)
				System.out.println(",");
			first = false;
			System.out.printf("\t\"%s\": %d", e.getKey(), e.getValue());
		}
		System.out.println("\n}\n}");
	}

}
