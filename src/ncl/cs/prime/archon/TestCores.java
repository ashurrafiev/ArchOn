package ncl.cs.prime.archon;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.CodeExecutor.ExecMode;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public class TestCores {

	public static int totalWork = 81920;
	public static boolean privateCache = true;
	public static int cacheMissPercent = 100;
	
	public static void createCode(File f, int n) {
		try {
			PrintWriter out = new PrintWriter(f);
			out.printf("// %d cores\n", n);
			out.println("#aliaspk \"ncl.cs.prime.archon.arch.modules.hicore\"");

			for(int i=1; i<=n; i++) {
				out.printf("#assign app%d \".App\"\n", i);
				out.printf("#assign core%d \".Core\"\n", i);
				if(privateCache)
					out.printf("#assign cache%d \".Cache\"\n", i);
				out.printf("#assign bus%d \".MasterNode\"\n", i);
			}

			out.println("#assign bus_mem \".SlaveNode\"");
			out.println("#assign mem \".Mem\"");

			for(int i=1; i<=n; i++) {
				out.printf("app%d.ack = core%d.done\n", i, i);
				out.printf("core%d.op = app%d.op\n", i, i);
				
				if(privateCache) {
					out.printf("core%d.ack = cache%d.done\n", i, i);
					out.printf("cache%d.req = core%d.mem_req\n", i, i);
					out.printf("cache%d(%d)\n", i, cacheMissPercent);
					out.printf("cache%d.mem_ack = bus%d.done\n", i, i);
					out.printf("bus%d.req = cache%d.mem_req\n", i, i);
					out.printf("bus%d(%d)\n", i, i);
				}
				else {
					out.printf("core%d.ack = bus%d.done\n", i, i);
					out.printf("bus%d.req = core%d.mem_req\n", i, i);
					out.printf("bus%d(%d)\n", i, i);
				}
				
				out.printf("bus%d.link = bus_mem.link\n", i);
				out.printf("#init app%d.c %d\n", i, totalWork/n);
				out.printf("#init core%d.done 1\n", i);
			}

			out.println("bus_mem.ack = mem.done");
			out.println("mem.req = bus_mem.mem_req");

			out.println("@loop");
			out.println("!");
			
			/*
			for(int i=1; i<=n; i++) {
				out.printf("#print \"State%d(\" app%d.c \") \" app%d.op \"; \" core%d.done \", \" core%d.mem_req \"; \" bus%d.done\n",
						i, i, i, i, i, i); 
			}			
			out.println("#print \"StateMem \" bus_mem.mem_req \"; \" mem.done");
			*/
			
			for(int i=1; i<=n; i++) {
				out.printf("[^app%d.done] #jump @loop\n", i);
			}

			out.println("!stop");
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void simulate(File f) {
		ProgramParserBytecode p = new ProgramParserBytecode();
		if(p.compile(f, false)!=null) {
//			System.out.println("Done");
			f = new File(f.getAbsolutePath()+".bin");
			f.deleteOnExit();
			
			CodeExecutor exec = new CodeExecutor();
			try {
				exec.getIP().loadCode(f);
				
//				System.out.println("Starting simulation");
				exec.execute(null, ExecMode.normal);
				System.out.println("Simulation finished, arch sync:\t"+exec.getArch().syncTime());
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
			System.out.println("Done with errors");

	}
	
	public static void main(String[] args) {
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-w")) {
				totalWork = Integer.parseInt(args[++i]);
			}
			else if(args[i].equals("-miss")) {
				double miss = Double.parseDouble(args[++i]);
				cacheMissPercent = (int) Math.round(miss * 100.0);
			}
		}
		
		System.out.printf("Requested workload: %d\nRequested cache miss rate: %d%%\n\n", totalWork, cacheMissPercent);
		
		File f = new File("test_cores.sim");
		f.deleteOnExit();
		for(int n=1; n<=256; n<<=1) {
			System.out.printf("Cores:\t%d\t", n);
			createCode(f, n);
			simulate(f);
		}
	}

}
