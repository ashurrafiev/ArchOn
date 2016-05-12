package ncl.cs.prime.archon.genetic;

import java.util.Arrays;
import java.util.Random;

import ncl.cs.prime.archon.bytecode.InstructionPointer;

public class Genome implements BasicGenome {

	public static final int CROSSOVER_RATE = 50;
	public static final int MUTATION_RATE = 800;
	
	public static int headerSize = 0;
	
	private byte[] code = null;
	private long fitness = -1;
	
	private Genome() {
	}
	
	@Override
	public byte[] getCode() {
		return code;
	}
	
	@Override
	public void setFitness(long fitness) {
		this.fitness = fitness;
	}
	
	@Override
	public long getFitness() {
		return fitness;
	}
	
	@Override
	public int compareFitness(BasicGenome g) {
		return Long.compare(fitness, g.getFitness());
	}
	
	private static Factory factory = null;
	
	public static BasicGenomeFactory<Genome> getFactory() {
		if(factory==null)
			factory = new Factory();
		return factory; 
	}
	
	public static class Factory implements BasicGenomeFactory<Genome> {
	
		@Override
		public Genome fromCode(InstructionPointer ip) {
			Genome gen = new Genome();
			gen.code = ip.copyCode();
			return gen;
		}
		
		@Override
		public Genome crossBreed(Genome g1, Genome g2, Random random) {
			byte[] code = new byte[Math.max(g1.code.length, g2.code.length)*2];
			int index = 0;
			for(; index<headerSize; index++)
				code[index] = g1.code[index];
			
			boolean f = false;
	
			int srcIndex = index;
			for(;; index++, srcIndex++) {
				byte[] src = f ? g1.code : g2.code;
				if(srcIndex>=src.length)
					break;
				
				code[index] = src[srcIndex];
				
				if(random.nextInt(MUTATION_RATE)==0) {
					switch(random.nextInt(3)) {
						case 0:
							srcIndex--;
							break;
						case 2:
							srcIndex++;
							break;
						default:
							code[index] = (byte)((int) code[index] ^ (1 << random.nextInt(7)));
							break;
					}
				}
				
				if(random.nextInt(CROSSOVER_RATE)==0)
					f = !f;
			}
			
			Genome gen = new Genome();
			gen.code = Arrays.copyOf(code, index);
			return gen;
		}
	
	}
	
}
