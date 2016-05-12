package ncl.cs.prime.archon.genetic.smart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import ncl.cs.prime.archon.bytecode.InstructionPointer;
import ncl.cs.prime.archon.genetic.BasicGenome;
import ncl.cs.prime.archon.genetic.BasicGenomeFactory;

public class SmartGenome implements BasicGenome {

	public static final int CROSSOVER_RATE = 10;
	public static final int MUTATION_RATE = 200;
	
	private ArrayList<SmartGene> genes = new ArrayList<>();
	
	private byte[] header;
	private byte[] codeCache = null;
	private long fitness = -1;
	
	@Override
	public byte[] getCode() {
		if(codeCache==null) {
			ArrayList<Byte> bytecode = new ArrayList<>();
			SmartGene.encode(bytecode, genes);

			codeCache = new byte[bytecode.size()+header.length];
			for(int i=0; i<header.length; i++)
				codeCache[i] = header[i];
			for(int i=0; i<bytecode.size(); i++)
				codeCache[i+header.length] = bytecode.get(i);
		}
		return codeCache;
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
	
	public static BasicGenomeFactory<SmartGenome> getFactory() {
		if(factory==null)
			factory = new Factory();
		return factory; 
	}
	
	public static class Factory implements BasicGenomeFactory<SmartGenome> {

		@Override
		public SmartGenome fromCode(InstructionPointer ip) {
			SmartGenome gen = new SmartGenome();
			gen.header = ip.getHeader();
			HashMap<Integer, SmartGene> addressMap = new HashMap<>();
			while(!ip.outOfRange()) {
				int addr = ip.getAddress();
				SmartGene g = SmartGene.decode(ip);
				if(g!=null) {
					gen.genes.add(g);
					addressMap.put(addr, g);
				}
			}
			int addr = gen.header.length;
			int index = 0;
			for(SmartGene g : gen.genes) {
				g.address = addr;
				addr += g.length();
				g.updateAddressRefs(addr, addressMap);
				g.index = index;
				index++;
			}
			return gen;
		}

		@Override
		public SmartGenome crossBreed(SmartGenome g1, SmartGenome g2, Random random) {
			SmartGenome gen = new SmartGenome();
			gen.header = g1.header;
			
			boolean f = false;
			
			int index = 0;
			int srcIndex = index;
			for(;; index++, srcIndex++) {
				ArrayList<SmartGene> src = f ? g1.genes: g2.genes;
				if(srcIndex>=src.size())
					break;
				
				gen.genes.add(src.get(srcIndex).copy());
				
				if(random.nextInt(MUTATION_RATE)==0) {
					switch(random.nextInt(4)) {
						case 0:
						case 1:
						case 2:
							srcIndex--;
							break;
						case 3:
							srcIndex++;
							break;
						default:
							gen.genes.get(gen.genes.size()-1).mutate();
							break;
					}
				}
				
				if(random.nextInt(CROSSOVER_RATE)==0)
					f = !f;
			}
			
			int addr = gen.header.length;
			index = 0;
			for(SmartGene g : gen.genes) {
				g.updateAddressRefs(gen.genes);
				g.address = addr;
				g.index = index;
				addr += g.length();
				index++;
			}
			for(SmartGene g : gen.genes) {
				g.cleanupSources();
			}
			return gen;
		}
		
	}

}
