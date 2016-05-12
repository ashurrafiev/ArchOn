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
		System.out.println("\n\nSimulation finished");

	}
	
	public static void main(String[] args) {
		File f;
		if(args.length>0)
			f = new File(args[0]);
		else
			f = new File("sample.sim");
		
		ProgramParserBytecode p = new ProgramParserBytecode();
		if(p.compile(f, false)!=null) {
			System.out.println("Done\n\n");
			f = new File(f.getAbsolutePath()+".bin");
			
			CodeExecutor exec = new CodeExecutor();
			try {
				exec.getIP().loadCode(f);
				
				simulate(exec);
//				simulate(exec);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
			System.out.println("Done with errors");
	}

}
