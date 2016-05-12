package ncl.cs.prime.archon.genetic.smart;

import ncl.cs.prime.archon.genetic.BasicGenomeFactory;
import ncl.cs.prime.archon.genetic.BasicPopulation;

public class SmartPopulation extends BasicPopulation<SmartGenome> {

	public SmartPopulation(int numCreatures, int numPreserve) {
		super(numCreatures, numPreserve);
	}
	
	@Override
	protected SmartGenome[] createArray(int size) {
		return new SmartGenome[size];
	}

	@Override
	protected BasicGenomeFactory<SmartGenome> getFactory() {
		return SmartGenome.getFactory();
	}
	
}
