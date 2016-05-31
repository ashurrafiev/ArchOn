package ncl.cs.prime.archon;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Random;

import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.CodeExecutor.ExecMode;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public class TestCores {

	public static boolean noc = false;
	public static int totalWork = 81920;
	public static boolean privateCache = true;
	public static int cacheMissPercent = 50;
	public static int memNodes = 1;
	
	public static void createCodeBuf(File f, int n) {
		try {
			PrintWriter out = new PrintWriter(f);
			out.println("#aliaspk \"ncl.cs.prime.archon.arch.modules.hicore\"");

			for(int i=1; i<=n; i++) {
				out.printf("#assign app%d \".App\"\n", i);
				out.printf("#assign core%d \".Core\"\n", i);
				if(privateCache)
					out.printf("#assign cache%d \".Cache\"\n", i);
				out.printf("#assign bus%d \".MasterNode\"\n\n", i);
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
				out.printf("#init core%d.done 1\n\n", i);
			}

			out.println("bus_mem.ack = mem.done");
			out.println("mem.req = bus_mem.mem_req");

			out.println("@loop");
			out.println("!");
			
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

	public static void createCodeNoc(File f, int n, int m, int memNodes) {
		try {
			PrintStream out = new PrintStream(f);
			out.println("#aliaspk \"ncl.cs.prime.archon.arch.modules.hicore\"");

			int work = totalWork / (n*m - memNodes);
			int[] memNodeMap = new int[memNodes];
			
			int mem = 0;
			for(int i=0; i<n; i++) {
				for(int j=0; j<m; j++) {
					if(mem < memNodes) {
						out.printf("#assign mem%d_%d \".Mem\"\n", j, i);
						memNodeMap[mem] = j*256+i;
						mem++;
					}
					else {
						out.printf("#assign app%d_%d \".App\"\n", j, i);
						out.printf("#assign core%d_%d \".Core\"\n", j, i);
						if(privateCache)
							out.printf("#assign cache%d_%d \".Cache\"\n", j, i);
					}
					out.printf("#assign rout%d_%d \".NocRouter\"\n\n", j, i);
				}
			}

			Random random = new Random();
			
			mem = 0;
			for(int i=0; i<n; i++) {
				for(int j=0; j<m; j++) {
					if(mem < memNodes) {
						out.printf("rout%d_%d.req = mem%d_%d.done\n", j, i, j, i);
						out.printf("mem%d_%d.req = rout%d_%d.done\n", j, i, j, i);
						out.printf("#init mem%d_%d.done 0\n", j, i);
						mem++;
					}
					else {
						out.printf("app%d_%d.ack = core%d_%d.done\n", j, i, j, i);
						out.printf("core%d_%d.op = app%d_%d.op\n", j, i, j, i);
						out.printf("cache%d_%d(%d)\n", j, i, memNodeMap[random.nextInt(memNodeMap.length)]);
						
						if(privateCache) {
							out.printf("core%d_%d.ack = cache%d_%d.done\n", j, i, j, i);
							out.printf("cache%d_%d.req = core%d_%d.mem_req\n", j, i, j, i);
							out.printf("cache%d_%d(%d)\n", j, i, cacheMissPercent);
							out.printf("cache%d_%d.mem_ack = rout%d_%d.done\n", j, i, j, i);
							out.printf("rout%d_%d.req = cache%d_%d.mem_req\n", j, i, j, i);
						}
						else {
							out.printf("core%d_%d.ack = rout%d_%d.done\n", j, i, j, i);
							out.printf("rout%d_%d.req = core%d_%d.mem_req\n", j, i, j, i);
						}

						out.printf("#init app%d_%d.c %d\n", j, i, work);
						out.printf("#init core%d_%d.done 1\n", j, i);
					}
					
					if(j>0)
						out.printf("rout%d_%d.n = rout%d_%d.link\n", j, i, j-1, i);
					if(j<m-1)
						out.printf("rout%d_%d.s = rout%d_%d.link\n", j, i, j+1, i);
					if(i>0)
						out.printf("rout%d_%d.w = rout%d_%d.link\n", j, i, j, i-1);
					if(i<n-1)
						out.printf("rout%d_%d.e = rout%d_%d.link\n", j, i, j, i+1);
					out.printf("rout%d_%d(%d)\n\n", j, i, j*256+i);
				}
			}

			out.println("@loop");
			out.println("!");
			
			mem = 0;
			for(int i=0; i<n; i++) {
				for(int j=0; j<m; j++) {
					if(mem < memNodes) {
						mem++;
					}
					else {
						out.printf("[^app%d_%d.done] #jump @loop\n", j, i);
					}
				}
			}

			out.println("!stop");
			out.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void simulate(File f) {
		ProgramParserBytecode p = new ProgramParserBytecode();
		if(p.compile(f, false)!=null) {
			f = new File(f.getAbsolutePath()+".bin");
			f.deleteOnExit();
			
			CodeExecutor exec = new CodeExecutor();
			try {
				exec.getIP().loadCode(f);
				exec.execute(null, ExecMode.normal);
				System.out.println(exec.getArch().syncTime());
				
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
			else if(args[i].equals("-m")) {
				memNodes = Integer.parseInt(args[++i]);
			}
			else if(args[i].equals("-miss")) {
				double miss = Double.parseDouble(args[++i]);
				cacheMissPercent = (int) Math.round(miss * 100.0);
			}
			else if(args[i].equals("-bus")) {
				noc = false;
			}
			else if(args[i].equals("-noc")) {
				noc = true;
			}
		}
		if(!noc) {
			memNodes = 1;
		}
		
		System.out.printf("Requested workload: %d\nRequested cache miss rate: %d%%\nRequested memory access points: %d\n",
				totalWork, cacheMissPercent, memNodes);
		File f = new File("test_cores.sim");
		f.deleteOnExit();
		
		if(noc) {
			System.out.println("NoC interconnect\n");
			System.out.printf("Mode:\tMem:\tCores:\tTime:\n");
			if(memNodes<0) {
				for(int n=2; n<=16; n++) {
					memNodes = n;
					if(n*(n+1)/2 < memNodes)
						continue;
					System.out.printf("NoC\t%d\t%d\t", memNodes, n*(n+1)-memNodes);
					createCodeNoc(f, n, n+1, memNodes);
					simulate(f);
				}
			}
			else {
				for(int n=2; n<=16; n++) {
					if(n*n/2 < memNodes)
						continue;
					System.out.printf("NoC\t%d\t%d\t", memNodes, n*n-memNodes);
					createCodeNoc(f, n, n, memNodes);
					simulate(f);
				}
			}
		}
		else {
			System.out.println("Bus interconnect\n");
			System.out.printf("Mode:\tMem:\tCores:\tTime:\n");
			for(int n=1; n<=256; n<<=1) {
				System.out.printf("Buf\t1\t%d\t", n);
				createCodeBuf(f, n);
				simulate(f);
			}
		}
	}

}
