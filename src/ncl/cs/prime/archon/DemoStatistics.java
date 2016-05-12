package ncl.cs.prime.archon;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import ncl.cs.prime.archon.arch.modules.arm.Mmu;
import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.FaultyCodeExecutor;

public class DemoStatistics {
	
	public static final int SPAN = 1;
	public static final int INDEX = 64;

	public static final int COUNTER_LIMIT = 1000000000 / 256 * SPAN;

	public int[] goldenMemory = null;
	
	public DemoStatistics(File goldenProgram, int[] matrix) {
		CodeExecutor exec = new CodeExecutor();
		try {
			exec.getIP().loadCode(goldenProgram);
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		Mmu.initData(DemoFaults.IMAGE_PATH, matrix, true);
		exec.setDebugOutput(null);
		exec.execute(new int[] {0, 256});
		goldenMemory = Arrays.copyOf(Mmu.sharedMemory, Mmu.sharedMemory.length);
	}

	public long run(int runs, byte[] code, int faultChance, int[] matrix, final JProgressBar progress) {
		long fitness = 0L;
		for(int r = 0; r<runs; r++) {
			FaultyCodeExecutor fexec = new FaultyCodeExecutor(faultChance, COUNTER_LIMIT);
			fexec.getIP().setCode(code);
			execute(fexec, INDEX, matrix);
			
			int ndiff = 0;
			int offs = 65536+65536+INDEX*256+256;
			for(int i=0; i<256*SPAN; i++) {
				if(goldenMemory[offs+i]!=Mmu.sharedMemory[offs+i]) ndiff++;
			}
			fitness += (long) ndiff;
			
			if(progress!=null) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						progress.setValue(progress.getValue()+1);
					}
				});
			}
		}
		return fitness;
	}
	
	public void execute(CodeExecutor exec, int index, int[] matrix) {
		Mmu.initData(DemoFaults.IMAGE_PATH, matrix, true);
		exec.setDebugOutput(null);
		exec.execute(new int[] {index*1024, SPAN});
	}

}
