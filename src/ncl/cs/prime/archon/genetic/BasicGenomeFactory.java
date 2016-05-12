package ncl.cs.prime.archon.genetic;

import java.util.Random;

import ncl.cs.prime.archon.bytecode.InstructionPointer;

public interface BasicGenomeFactory<G extends BasicGenome> {

	public G fromCode(InstructionPointer ip);
	public G crossBreed(G g1, G g2, Random random);

}
