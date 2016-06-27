package ncl.cs.prime.archon.hicoredemo;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class NocBuilder implements Builder {
	
	public enum NodeType {
		blocked,
		onlyRouter(
				Simulator.commSetup),
		memory("MEM", new Color(0xffffbb77),
				Simulator.memSetup),
		coreA7("A7", new Color(0xffbbddff), true, false,
				Simulator.a7setup, null),
		coreA15("A15", new Color(0xff99bbff), true, false,
				Simulator.a15setup, null),
		coreA7cache("A7", new Color(0xffddffbb), true, true,
				Simulator.a7L1CacheSetup, coreA7),
		coreA15cache("A15", new Color(0xffbbff99), true, true,
				Simulator.a15L1CacheSetup, coreA15);
		
		public String label = null;
		public Color color = null;
		public boolean isCore = false;
		public boolean privateCache = false;
		public String setup = null;
		public NodeType coreType = null;

		private NodeType() {
		}
		private NodeType(String setup) {
			this.setup = setup;
		}
		private NodeType(String label, Color color, String setup) {
			this(label, color, false, false, setup, null);
		}
		private NodeType(String label, Color color, boolean isCore, boolean privateCache, String setup, NodeType coreType) {
			this.label = label;
			this.color = color;
			this.isCore = isCore;
			this.privateCache = privateCache;
			this.setup = setup;
			this.coreType = coreType;
		}
		public String coreSetup() {
			return coreType==null ? setup : coreType.setup;
		}
	}

	public NodeType[][] mesh;
	public int width;
	public int height;
	
	public NocBuilder() {
		mesh = new NodeType[4][4];
		width = 4;
		height = 4;
		for(int x=0; x<width; x++)
			for(int y=0; y<height; y++) {
				mesh[x][y] = y==0 ? NodeType.memory : NodeType.coreA7cache;
			}
	}
	
	private NocBuilder(int width, int height) {
		mesh = new NodeType[width][height];
		this.width = width;
		this.height = height;
	}
	
	public void resizeMesh(int resx, int resy, int dx, int dy) {
		NodeType[][] newMesh = new NodeType[width+resx][height+resy];
		for(int x=0; x<width+resx; x++)
			for(int y=0; y<height+resy; y++) {
				newMesh[x][y] = NocNodeListPanel.activeType;
			}
		for(int x=0; x<width; x++)
			for(int y=0; y<height; y++) {
				newMesh[x+dx][y+dy] = mesh[x][y];
			}
		width += resx;
		height += resy;
		mesh = newMesh;
	}
	
	public void deleteRows(int y0, int y1) {
		NodeType[][] newMesh = new NodeType[width][height-(y1-y0+1)];
		int ny = 0;
		for(int y=0; y<height; y++) {
			if(y>=y0 && y<=y1)
				continue;
			for(int x=0; x<width; x++) {
				newMesh[x][ny] = mesh[x][y];
			}
			ny++;
		}
		height -= y1-y0+1;
		mesh = newMesh;
	}
	
	public void deleteCols(int x0, int x1) {
		NodeType[][] newMesh = new NodeType[width-(x1-x0+1)][height];
		int nx = 0;
		for(int x=0; x<width; x++) {
			if(x>=x0 && x<=x1)
				continue;
			for(int y=0; y<height; y++) {
				newMesh[nx][y] = mesh[x][y];
			}
			nx++;
		}
		width -= x1-x0+1;
		mesh = newMesh;
	}
	
	public int countNodes(NodeType t) {
		int count = 0;
		for(int x=0; x<width; x++)
			for(int y=0; y<height; y++) {
				if(mesh[x][y]==t)
					count++;
			}
		return count;
	}
	
	public int countCoreNodes() {
		int count = 0;
		for(NodeType t : NodeType.values())
			if(t.isCore)
				count += countNodes(t);
		return count;
	}
	
	@Override
	public void writeCode(File f) throws IOException {
		writeCode(new PrintStream(f)).close();
	}
	
	@Override
	public PrintStream writeCode(PrintStream out) {
		out.println("#aliaspk \"ncl.cs.prime.archon.arch.modules.hicore\"");
		out.println("#estim \".HiEstimation\"");

		int work = Simulator.totalWork;
		if(Simulator.splitWork)
			work /= countCoreNodes();
		int memNodes = countNodes(NodeType.memory);
		int[] memNodeMap = new int[memNodes];
		
		int mem = 0;
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				if(mesh[i][j]==NodeType.memory) {
					out.printf("#assign mem%d_%d \".Mem\"\n", j, i);
					memNodeMap[mem] = j*256+i;
					out.printf("#setup mem%d_%d \"%spageAddr=%d\"\n", j, i, mem==0 ? "reset;" : "" , memNodeMap[mem]);
					out.printf("#setup mem%d_%d \"%s\"\n", j, i, mesh[i][j].setup);
					mem++;
				}
				else if(mesh[i][j].isCore) {
					out.printf("#assign app%d_%d \".App\"\n", j, i);
					out.printf("#setup app%d_%d \"%s\"\n", j, i, Simulator.appSetup);
					out.printf("#assign core%d_%d \".Core\"\n", j, i);
					out.printf("#setup core%d_%d \"%s\"\n", j, i, mesh[i][j].coreSetup());
					if(mesh[i][j].privateCache) {
						out.printf("#assign cache%d_%d \".Cache\"\n", j, i);
						out.printf("#setup cache%d_%d \"missRate=%f\"\n", j, i, (double) Simulator.cacheMissPercent / 100.0);
						out.printf("#setup cache%d_%d \"%s\"\n", j, i, mesh[i][j].setup);
					}
				}
				
				if(mesh[i][j]!=NodeType.blocked) {
					out.printf("#assign rout%d_%d \".NocRouter\"\n\n", j, i);
					out.printf("#setup rout%d_%d \"%s\"\n", j, i, NodeType.onlyRouter.setup);
				}
			}
		}

		mem = 0;
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				if(mesh[i][j]==NodeType.memory) {
					out.printf("rout%d_%d.req = mem%d_%d.done\n", j, i, j, i);
					out.printf("mem%d_%d.req = rout%d_%d.done\n", j, i, j, i);
					out.printf("#init mem%d_%d.done 0\n", j, i);
					mem++;
				}
				else if(mesh[i][j].isCore) {
					out.printf("app%d_%d.ack = core%d_%d.done\n", j, i, j, i);
					out.printf("core%d_%d.op = app%d_%d.op\n", j, i, j, i);
					
					if(mesh[i][j].privateCache) {
						out.printf("core%d_%d.ack = cache%d_%d.done\n", j, i, j, i);
						out.printf("cache%d_%d.req = core%d_%d.mem_req\n", j, i, j, i);
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
				
				if(mesh[i][j]!=NodeType.blocked) {
					if(j>0 && mesh[i][j-1]!=NodeType.blocked)
						out.printf("rout%d_%d.n = rout%d_%d.link\n", j, i, j-1, i);
					if(j<height-1 && mesh[i][j+1]!=NodeType.blocked)
						out.printf("rout%d_%d.s = rout%d_%d.link\n", j, i, j+1, i);
					if(i>0 && mesh[i-1][j]!=NodeType.blocked)
						out.printf("rout%d_%d.w = rout%d_%d.link\n", j, i, j, i-1);
					if(i<width-1 && mesh[i+1][j]!=NodeType.blocked)
						out.printf("rout%d_%d.e = rout%d_%d.link\n", j, i, j, i+1);
					out.printf("rout%d_%d(%d)\n\n", j, i, j*256+i);
				}
			}
		}

		out.println("@loop");
		out.println("!");
		
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				if(mesh[i][j].isCore) {
					out.printf("[^app%d_%d.done] #jump @loop\n", j, i);
				}
			}
		}

		out.println("!stop");
		return out;
	}
	
	public static NocBuilder createSingleMemNoc(int width, int height, NodeType core) {
		NocBuilder builder = new NocBuilder(width, height+1);
		for(int x=0; x<width; x++)
			for(int y=0; y<height+1; y++) {
				if(y==0)
					builder.mesh[x][y] = x==0 ? NodeType.memory : NodeType.onlyRouter; 
				else
					builder.mesh[x][y] = core;
			}

		return builder;
	}
	
	public static NocBuilder createMemRowNoc(int width, int height, NodeType core) {
		NocBuilder builder = new NocBuilder(width, height+1);
		for(int x=0; x<width; x++)
			for(int y=0; y<height+1; y++) {
				if(y==0)
					builder.mesh[x][y] = NodeType.memory; 
				else
					builder.mesh[x][y] = core;
			}

		return builder;
	}
	
	public static NocBuilder create4MemNoc(int width, int height, NodeType core) {
		NocBuilder builder = new NocBuilder(width+2, height+2);
		for(int x=0; x<width+2; x++)
			for(int y=0; y<height+2; y++) {
				boolean bx = x==0 || x==(width+1);
				boolean by = y==0 || y==(height+1);
				if(bx || by)
					builder.mesh[x][y] = bx && by ? NodeType.memory : NodeType.onlyRouter; 
				else
					builder.mesh[x][y] = core;
			}

		return builder;
	}
	
	public static NocBuilder createMemBoxNoc(int width, int height, NodeType core) {
		NocBuilder builder = new NocBuilder(width+2, height+2);
		for(int x=0; x<width+2; x++)
			for(int y=0; y<height+2; y++) {
				boolean bx = x==0 || x==(width+1);
				boolean by = y==0 || y==(height+1);
				if(bx || by)
					builder.mesh[x][y] = bx && by ? NodeType.blocked : NodeType.memory; 
				else
					builder.mesh[x][y] = core;
			}

		return builder;
	}
}
