package ncl.cs.prime.archon.arch.modules.arm;

import java.util.HashMap;
import java.util.Map.Entry;

import ncl.cs.prime.archon.arch.Architecture;
import ncl.cs.prime.archon.arch.Estimation;

public class ArmEstimation implements Estimation {

	public static String estimDump = "";
	
	private Architecture arch;
	private HashMap<String, Integer> counter = new HashMap<>(); 
	
	@Override
	public void init(Architecture arch) {
		this.arch = arch;
		reset();
	}

	private void reset() {
		counter.clear();
	}
	
	public void forModule(String key) {
		int c = 1;
		if(counter.containsKey(key))
			c += counter.get(key);
		counter.put(key, c);
	}
	
	@Override
	public void beginCycle() {
	}

	@Override
	public void endCycle() {
	}

	@Override
	public void dump() {
		System.out.println(" -- Resource access counter --");
		for(Entry<String, Integer> e : counter.entrySet()) {
			System.out.printf("  |  %s:%d", e.getKey(), e.getValue());
		}
		System.out.println("\n -- Platform time: "+arch.syncTime()+" -- \n\n");

		estimDump += "&nbsp;<br><u>Resource access (per core):</u><br><table>";
		for(Entry<String, Integer> e : counter.entrySet()) {
			estimDump += "<tr><td align=\"right\">"+e.getKey()+"</td><td><b>"+e.getValue()+"</b></td></tr>";
		}
		estimDump += "</table>";
		
		reset();
	}

	
	
}
