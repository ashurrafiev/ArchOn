package ncl.cs.prime.archon.genetic;

public class Population extends BasicPopulation<Genome> {

	public Population(int numCreatures, int numPreserve) {
		super(numCreatures, numPreserve);
	}
	
	@Override
	protected Genome[] createArray(int size) {
		return new Genome[size];
	}

	@Override
	protected BasicGenomeFactory<Genome> getFactory() {
		return Genome.getFactory();
	}

}
