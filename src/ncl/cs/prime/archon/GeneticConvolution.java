package ncl.cs.prime.archon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import ncl.cs.prime.archon.arch.modules.arm.Mmu;
import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.FaultyCodeExecutor;
import ncl.cs.prime.archon.genetic.BasicGenome;
import ncl.cs.prime.archon.genetic.BasicPopulation;
import ncl.cs.prime.archon.genetic.Genome;
import ncl.cs.prime.archon.genetic.smart.SmartPopulation;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public class GeneticConvolution {

	public static final String IMAGE_PATH = "sample.png";
	public static final int FAULT_CHANCE = 25000;
	public static final int COUNTER_LIMIT = 2000000;
	public static final int RUNS = 10;
	
	public static final int SPAN = 1;
	public static final int MIN_INDEX = 32;
	public static final int MAX_INDEX = 192;
	public static final int OUTPUT_PTR = 131072;
	public static final int[][] MATRIX = {
		{0, -1, 0, -1, 4, -1, 0, -1, 0},
		{0, -1, 0, -1, 5, -1, 0, -1, 0},
		{0, -1, 0, -1, 1, 1, 0, 1, 0},
		{1, 1, 0, 1, 1, -1, 0, -1, -1},
	};

	public static final int POPULATION_SIZE = 10;
	public static final int NUM_PRESERVE = 4;

	public static final String CSV_OUT_PATH = "fitness.csv";
	public static final String CODE_OUT_PATH_FMT = "generations/best_%d.sim.bin";
	public static final int SAVE_EVERY = 10;
	public static final String SRC_OUT_PATH_FMT = "generations/best_%d.sim";

	public static final Random random = new Random();

	private byte[] origin;
	
	public GeneticConvolution(File program) {
		BasicPopulation<?> population =
				//new Population(POPULATION_SIZE, NUM_PRESERVE);
				new SmartPopulation(POPULATION_SIZE, NUM_PRESERVE);
		
		try {
			CodeExecutor exec = new CodeExecutor();
			exec.getIP().loadCode(program);
			origin = exec.getIP().copyCode();
			Genome.headerSize = exec.getIP().getHeaderSize();
			System.out.println("Header size: "+Genome.headerSize);
			population.initialPopulate(exec.getIP());
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
			return;
		}
		population.breed(random);
	
		long tStart = System.currentTimeMillis();
		PrintWriter out = null;
		if(CSV_OUT_PATH!=null) {
			try {
			    out = new PrintWriter(new FileWriter(CSV_OUT_PATH));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for(int generation = 0; ; generation++) {
			long tGen = System.currentTimeMillis();
			System.out.println("\nGENERATION "+generation);
			long fitness = 0L;
			for(int i=0; i<population.size(); i++) {
				if(i==NUM_PRESERVE-1)
					System.out.print("___");
				System.out.print("\t..."+i);
				BasicGenome gen = population.get(i);
				long f = run(gen.getCode()) / RUNS;
				fitness += f;
				gen.setFitness(f);
				System.out.println("\t"+f);
			}
			fitness = fitness / population.size();
			BasicGenome best = population.breed(random);
			System.out.println("Fitness: " + best.getFitness() + " (best), " + fitness + " (ave)");
			long t = System.currentTimeMillis();
			System.out.println(formatTime(t-tGen)+" generation time | "+formatTime(t-tStart)+" elapsed total");
			
			if(out!=null) {
				out.println(generation+"\t"+best.getFitness()+"\t"+fitness);
				out.flush();
			}
			if(generation%SAVE_EVERY==0 && CODE_OUT_PATH_FMT!=null) {
				try {
					FileOutputStream f = new FileOutputStream(new File(String.format(CODE_OUT_PATH_FMT, generation)));
					f.write(best.getCode());
					f.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public String formatTime(long t) {
		String s = "--";
		t = t/1000L; if(t==0L) return s;
		s = (t % 60)+"s";
		t = t/60L; if(t==0L) return s;
		s = (t % 60)+"min "+s;
		t = t/60L; if(t==0L) return s;
		s = t+"hr ";
		return s;
	}
		
	public long run(byte[] code) {
		long fitness = 0L;
		for(int r = 0; r<RUNS; r++) {
			int index = random.nextInt(MAX_INDEX-MIN_INDEX+1)+MIN_INDEX;
			int[] matrix = MATRIX[random.nextInt(MATRIX.length)];

			CodeExecutor exec = new CodeExecutor();
			exec.getIP().setCode(origin);
			int[] golden = execute(exec, index, matrix);
			
			FaultyCodeExecutor fexec = new FaultyCodeExecutor(FAULT_CHANCE, COUNTER_LIMIT);
			fexec.getIP().setCode(code);
			int[] res = execute(fexec, index, matrix);
			
//			long diff = 0;
			int ndiff = 0;
			for(int i=0; i<1024*SPAN; i++) {
				if(golden[i]!=res[i]) ndiff++;
//				diff += (long) Math.abs(golden[i] - res[i]);
			}
			
			// Compute fitness (lesser=better)
			fitness += fexec.errors.size() * 300L; // for exceptions
//			fitness += (long) diff; // for difference
			fitness += (long) ndiff * 100L; // for difference in pixels
//			fitness += (long) fexec.counter / 1000L; // for execution time
			fitness += (long) code.length / 10L; // for code size
		}
		return fitness;
	}
	
	public int[] getMemorySnapshot(int index) {
		int[] m = new int[1024*SPAN];
		for(int i=0; i<1024*SPAN; i++)
			m[i] = Mmu.sharedMemory[OUTPUT_PTR+(SPAN*index)*256+i];
		return m;
	}
	

	public int[] execute(CodeExecutor exec, int index, int[] matrix) {
		Mmu.initData(IMAGE_PATH, matrix, true);
		exec.setDebugOutput(null);
		exec.execute(new int[] {(SPAN*index)*1024, SPAN});
		return getMemorySnapshot(index);
	}
	
	public static void main(String[] args) {
		new GeneticConvolution(ProgramParserBytecode.compileFile("convo_arm.sim", true));
	}

}
