package ncl.cs.prime.archon;

import java.io.File;
import java.io.IOException;

import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.CodeExecutor.ExecMode;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public class TestSim {

	private static void simulate(CodeExecutor exec) {
		System.out.println("Starting simulation\n\n");
		exec.execute(null, ExecMode.normal);
		System.out.println("\n\nSimulation finished, arch sync @"+exec.getArch().syncTime());

	}
	
	public static void main(String[] args) {
		File f = new File("sample.sim");
		boolean randomOrder = false;
		boolean timeOrder = false;
		for(int i=0; i<args.length; i++) {
			if("-ro".equals(args[i]))
				randomOrder = true;
			else if("-to".equals(args[i]))
				timeOrder = true;
			else
				f = new File(args[i]);
		}
		
		System.out.println(f.getAbsolutePath());
		ProgramParserBytecode p = new ProgramParserBytecode();
		if(p.compile(f, false)!=null) {
			System.out.println("Done\n\n");
			f = new File(f.getAbsolutePath()+".bin");
			
			CodeExecutor exec = new CodeExecutor();
			exec.getRouter().randomOrder = randomOrder;
			exec.getRouter().timeOrder = timeOrder;
			try {
				exec.getIP().loadCode(f);
				long t = System.currentTimeMillis();
				simulate(exec);
				System.out.printf("Done in %d ms", System.currentTimeMillis() - t);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
			System.out.println("Done with errors");
	}

}
