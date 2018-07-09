package ncl.cs.prime.archon;

import java.io.File;
import java.io.IOException;

import ncl.cs.prime.archon.arch.modules.arm.Mmu;
import ncl.cs.prime.archon.bytecode.FaultyCodeExecutor;
import ncl.cs.prime.archon.bytecode.FaultyCodeExecutor.InjectionInfo;

public class FaultyConvolution {

	public static final String IMAGE_PATH = "examples/sample.png";
	public static final int FAULT_CHANCE = 10000;
	public static final int COUNTER_LIMIT = 10000000;
	public static final int RUNS = 10;
	public static final int span = 1;
	public static final int index = 64;
	public static final int OUTPUT_PTR = 131072;
	
	public FaultyConvolution(File goldenProgram, File program) {
		System.out.println("GOLDEN");
		int[] golden = run(goldenProgram, 0);
		
		for(int r = 0; r<RUNS; r++) {
			System.out.println("\nRUN "+r+" fault chance = 1/"+FAULT_CHANCE);
			int[] res = run(program, FAULT_CHANCE);
			int diff = 0;
			int ndiff = 0;
			for(int i=0; i<1024*span; i++) {
				if(golden[i]!=res[i]) ndiff++;
				diff += Math.abs(golden[i] - res[i]);
			}
			System.out.println("Difference: "+diff+" ("+ndiff+"px)");
		}
	}

	public int[] getMemorySnapshot() {
		int[] m = new int[1024*span];
		for(int i=0; i<1024*span; i++)
			m[i] = Mmu.sharedMemory[OUTPUT_PTR+(span*index)*256+i];
		return m;
	}
	
	public int[] run(File program, int faultChance) {
		try {
			Mmu.initData(IMAGE_PATH, new int[] {0, -1, 0, -1, 5, -1, 0, -1, 0}, true);
			FaultyCodeExecutor exec = new FaultyCodeExecutor(faultChance, COUNTER_LIMIT);
			exec.getIP().loadCode(program);
			exec.execute(new int[] {(span*index)*1024, span});
			
			System.out.println("Injections (" + exec.injections.size() + "):");
			for(InjectionInfo i: exec.injections)
				System.out.println("\t"+i.counter+" @"+i.addr+" ("+i.cmd+")");
			System.out.println("Total operations: "+exec.counter);
			System.out.println("Exceptions: "+exec.errors.size());
			
			return getMemorySnapshot();
		}
		catch(IOException e) {
			System.err.println(e.getMessage());
			return null;
		}
	}
	
	public static void main(String[] args) {
/*		File f = new File("convo_arm.sim");
		ProgramParser p = new ProgramParser();
		if(!p.parse(f)) {
			System.err.println("Compiled with errors.\n\n");
			System.exit(1);
		}
		
		System.out.println("Compiled successfully.\n\n");
		f = new File(f.getAbsolutePath()+".bin");*/
		File fg = new File("convo_arm.sim.bin");
		File f = new File("generations/best_30.sim.bin");
		new FaultyConvolution(fg, f);
	}

}
