package ncl.cs.prime.archon;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import ncl.cs.prime.archon.arch.modules.arm.Mmu;
import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.FaultyCodeExecutor;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public class FaultStatistics {

	public static final String IMAGE_PATH = "sample.png";
	public static final double FAULT_RATE_MIN = 25.0;
	public static final double FAULT_RATE_MAX = 250.0;
	public static final double FAULT_RATE_STEP = 25.0;
	public static final int COUNTER_LIMIT = 2000000;
	public static final int RUNS = 25;
	
	public static final int SPAN = 1;
	public static final int MIN_INDEX = 64;//32;
	public static final int MAX_INDEX = 64;//192;
	public static final int OUTPUT_PTR = 65536+65536+256;
	public static final int[][] MATRIX = {
//		{0, -1, 0, -1, 4, -1, 0, -1, 0},
		{0, -1, 0, -1, 5, -1, 0, -1, 0},
//		{0, -1, 0, -1, 1, 1, 0, 1, 0},
//		{1, 1, 0, 1, 1, -1, 0, -1, -1},
	};

	public static final String CSV_OUT_PATH = "fault_stats.csv";

	public static final Random random = new Random();

	private File goldenProgram;
	
	public FaultStatistics(File goldenProgram, File... programs) {
		this.goldenProgram = goldenProgram;
		
		PrintWriter out = null;
		if(CSV_OUT_PATH!=null) {
			try {
			    out = new PrintWriter(new FileWriter(CSV_OUT_PATH));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for(double faultRate = FAULT_RATE_MIN; faultRate <= FAULT_RATE_MAX; faultRate += FAULT_RATE_STEP) {
			out.print(faultRate);
			int fault = (int) (1000000.0 / faultRate);
			out.print("\t"+fault);
			System.out.println("Fault rate: "+faultRate+" ["+fault+"]");
			for(int i=0; i<programs.length; i++) {
				System.out.print("\t..."+i);
				try {
					long fitness = run(programs[i], fault);
					System.out.println("\t"+fitness);
					out.print("\t"+fitness);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			out.println();
		}
		out.close();
		System.out.println("Done.");
	}
		
	public long run(File program, int faultChance) throws IOException {
		long fitness = 0L;
		for(int r = 0; r<RUNS; r++) {
			int index = random.nextInt(MAX_INDEX-MIN_INDEX+1)+MIN_INDEX;
			int[] matrix = MATRIX[random.nextInt(MATRIX.length)];

			CodeExecutor exec = new CodeExecutor();
			exec.getIP().loadCode(goldenProgram);
			int[] golden = execute(exec, index, matrix);
			
			FaultyCodeExecutor fexec = new FaultyCodeExecutor(faultChance, COUNTER_LIMIT);
			fexec.getIP().loadCode(program);
			int[] res = execute(fexec, index, matrix);
			
//			long diff = 0;
			int ndiff = 0;
			for(int i=0; i<256*SPAN; i++) {
				if(golden[i]!=res[i]) ndiff++;
//				diff += (long) Math.abs(golden[i] - res[i]);
			}
			
			fitness += (long) ndiff;
		}
		return fitness;
	}
	
	public int[] getMemorySnapshot(int index) {
		int[] m = new int[256*SPAN];
		for(int i=0; i<256*SPAN; i++)
			m[i] = Mmu.sharedMemory[OUTPUT_PTR+index*256+i];
		return m;
	}
	

	public int[] execute(CodeExecutor exec, int index, int[] matrix) {
		Mmu.initData(IMAGE_PATH, matrix, true);
		exec.setDebugOutput(null);
		exec.execute(new int[] {(SPAN*index)*1024, SPAN});
		return getMemorySnapshot(index);
	}
	
	public static void main(String[] args) {
		new FaultStatistics(
			ProgramParserBytecode.compileFile("convo_arm.sim", true),
			ProgramParserBytecode.compileFile("convo_arm.sim", true),
			ProgramParserBytecode.compileFile("convo_arm_safe.sim", true),
			ProgramParserBytecode.compileFile("convo_arm_safest.sim", true),
			ProgramParserBytecode.compileFile("convo_arm_safestx.sim", true)
		);
	}
}
