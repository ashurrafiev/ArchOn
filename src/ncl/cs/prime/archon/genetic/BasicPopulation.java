package ncl.cs.prime.archon.genetic;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import ncl.cs.prime.archon.bytecode.InstructionPointer;

public abstract class BasicPopulation<G extends BasicGenome> {

	private G[] creatures;
	private int numPreserve;
	
	public BasicPopulation(int numCreatures, int numPreserve) {
		creatures = createArray(numCreatures);
		this.numPreserve = numPreserve;
	}
	
	protected abstract G[] createArray(int size);
	protected abstract BasicGenomeFactory<G> getFactory(); 
	
	public void initialPopulate(InstructionPointer origin) {
		for(int i=0; i<creatures.length; i++)
			creatures[i] = getFactory().fromCode(origin);
	}
	
	public int size() {
		return creatures.length;
	}
	
	public G get(int i) {
		return creatures[i];
	}
	
	public G breed(Random random) {
		Arrays.sort(creatures, new Comparator<BasicGenome>() {
			@Override
			public int compare(BasicGenome o1, BasicGenome o2) {
				return o1.compareFitness(o2);
			}
		});
		G best = creatures[0];
		
		for(int i=numPreserve; i<creatures.length; i++)
			creatures[i] = getFactory().crossBreed(
					creatures[random.nextInt(numPreserve)],
					creatures[random.nextInt(numPreserve)],
					random
				);
		
		return best;
	}
	
}
