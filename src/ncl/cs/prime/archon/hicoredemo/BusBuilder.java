package ncl.cs.prime.archon.hicoredemo;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class BusBuilder {

	public enum NodeType {
		empty,
		bus("BUS", new Color(0xffffeeaa), Simulator.commSetup),
		memory("MEM", new Color(0xffffbb77), Simulator.memSetup),
		coreA7("A7", new Color(0xffbbddff), Simulator.a7setup),
		coreA15("A15", new Color(0xff99bbff), Simulator.a15setup),
		coreA7idle("A7", new Color(0xffdddddd), Simulator.a7setup),
		coreA15idle("A15", new Color(0xffbbbbbb), Simulator.a15setup),
		cacheL1_A7("L1", new Color(0xffddffbb), Simulator.a7L1CacheSetup),
		cacheL1_A15("L1", new Color(0xffbbff99), Simulator.a15L1CacheSetup),
		cacheL2_A7("L2", new Color(0xffddffbb), Simulator.a7L2CacheSetup),
		cacheL2_A15("L2", new Color(0xffbbff99), Simulator.a15L2CacheSetup);
		
		public String label = null;
		public Color color = null;
		public String setup = null;

		private NodeType() {
		}
		private NodeType(String setup) {
			this.setup = setup;
		}
		private NodeType(String label, Color color, String setup) {
			this.label = label;
			this.color = color;
			this.setup = setup;
		}
		
		public boolean isCore() {
			return this==coreA7 || this==coreA15 || this==coreA7idle || this==coreA15idle;
		}
		public boolean isCache() {
			return this==cacheL1_A7 || this==cacheL2_A7 || this==cacheL1_A15 || this==cacheL2_A15;
		}
		public Node createNode(BusBuilder builder) {
			switch (this) {
				case empty:
					return builder.new EmptyNode();
				case bus:
					return builder.new BusNode();
				case coreA7:
				case coreA15:
					return builder.new CoreNode(this);
				case coreA7idle:
				case coreA15idle:
					return builder.new IdleCoreNode(this);
				case cacheL1_A7:
				case cacheL2_A7:
				case cacheL1_A15:
				case cacheL2_A15:
					return builder.new CacheNode(this);
				default:
					return null;
			}
		}
	}
	
	public class UndoReplace {
		public Node parent;
		public int index; 
		public Node oldSub;
		public boolean removed = false;
	}
	
	private LinkedList<UndoReplace> undoList = new LinkedList<>();
	
	public abstract class Node {
		public int id;
		public NodeType type;
		public ArrayList<Node> subs = new ArrayList<>();
		public Node parent = null;
		private Node(NodeType type) {
			this.type = type;
		}
		public Node add(Node... subs) {
			for(Node s : subs) {
				this.subs.add(s);
				s.parent = this;
			}
			return this;
		}
		public int getWidth() {
			int w = 0;
			for(Node s : subs)
				w += s.getWidth();
			return w>0 ? w : 1;
		}
		public int getHeight() {
			int h = 0;
			for(Node s: subs) {
				int sh = s.getHeight();
				if(sh>h) h = sh;
			}
			return h+1;
		}
		public int updateIds(int baseId) {
			int i = 1;
			id = baseId;
			for(Node s : subs)
				i += s.updateIds(baseId+i);
			return i;
		}
		public Node replaceSub(Node sub, NodeType newType) {
			int index = subs.indexOf(sub);
			
			UndoReplace u = new UndoReplace();
			u.parent = this;
			u.index = index;
			u.oldSub = sub;
			undoList.add(u);
			
			Node n = newType.createNode(BusBuilder.this);
			if(newType==NodeType.empty && index<subs.size()-1) {
				subs.remove(index);
				n.parent = this;
				u.removed = true;
				return subs.get(index);
			}
			else {
				subs.set(index, n);
				n.parent = this;
				return n;
			}
		}
		public Node replace(NodeType newType) {
			return parent.replaceSub(this, newType);
		}
		public abstract String getReqName(Node s);
		public abstract String getAckName(Node s);
		public abstract void writeInit(PrintStream out);
		public abstract void writeConnect(PrintStream out);
		public abstract boolean acceptChild(NodeType type);
	}
	
	public class EmptyNode extends Node {
		public EmptyNode() {
			super(NodeType.empty);
		}
		public EmptyNode(Node parent) {
			super(NodeType.empty);
			this.parent = parent;
		}
		@Override
		public String getReqName(Node s) {
			return null;
		}
		@Override
		public String getAckName(Node s) {
			return null;
		}
		@Override
		public void writeInit(PrintStream out) {
		}
		@Override
		public void writeConnect(PrintStream out) {
		}
		@Override
		public boolean acceptChild(NodeType type) {
			return false;
		}
	}
	
	public class IdleCoreNode extends Node {
		public IdleCoreNode(NodeType type) {
			super(type);
		}
		@Override
		public String getReqName(Node s) {
			return null;
		}
		@Override
		public String getAckName(Node s) {
			return null;
		}
		@Override
		public void writeInit(PrintStream out) {
			out.printf("#assign core%d \".Core\"\n", id);
			if(type.setup!=null)
				out.printf("#setup core%d \"%s\"\n", id, type.setup);
		}
		@Override
		public void writeConnect(PrintStream out) {
			out.printf("#init core%d.done 1\n", id);
			out.printf("core%d.ack = %s\n", id, parent.getAckName(this));
			out.printf("%s = core%d.mem_req\n", parent.getReqName(this), id);
		}
		@Override
		public boolean acceptChild(NodeType type) {
			return false;
		}
	}
	
	public class CoreNode extends IdleCoreNode {
		public CoreNode(NodeType type) {
			super(type);
		}
		@Override
		public int updateIds(int baseId) {
			appIds.add(baseId);
			return super.updateIds(baseId);
		}
		@Override
		public void writeInit(PrintStream out) {
			out.printf("#assign app%d \".App\"\n", id);
			out.printf("#setup app%d \"%s\"\n", id, Simulator.appSetup);
			super.writeInit(out);
		}
		@Override
		public void writeConnect(PrintStream out) {
			out.printf("app%d.ack = core%d.done\n", id, id);
			out.printf("core%d.op = app%d.op\n", id, id);
			out.printf("#init app%d.c %d\n", id, work);
			super.writeConnect(out);
		}
	}
	
	public class CacheNode extends Node {
		public CacheNode(NodeType type) {
			super(type);
			subs.add(new EmptyNode(this));
		}
		@Override
		public Node add(Node... subs) {
			this.subs.clear();
			return super.add(subs[0]);
		}
		@Override
		public String getReqName(Node s) {
			return String.format("cache%d.req", id);
		}
		@Override
		public String getAckName(Node s) {
			return String.format("cache%d.done", id);
		}
		@Override
		public void writeInit(PrintStream out) {
			subs.get(0).writeInit(out);
			out.printf("#assign cache%d \".Cache\"\n", id);
			out.printf("#setup cache%d \"missRate=%f\"\n", id, (double) Simulator.cacheMissPercent / 100.0);
			if(type.setup!=null)
				out.printf("#setup cache%d \"%s\"\n", id, type.setup);
		}
		@Override
		public void writeConnect(PrintStream out) {
			subs.get(0).writeConnect(out);
			out.printf("cache%d.mem_ack = %s\n", id, parent.getAckName(this));
			out.printf("%s = cache%d.mem_req\n", parent.getReqName(this), id);
		}
		@Override
		public boolean acceptChild(NodeType type) {
			return type.isCore() || type==NodeType.bus || type==NodeType.empty;
		}
	}
	
	public class BusNode extends Node {
		public BusNode() {
			super(NodeType.bus);
			subs.add(new EmptyNode(this));
		}
		@Override
		public Node add(Node... subs) {
			this.subs.remove(this.subs.size()-1);
			super.add(subs);
			this.subs.add(new EmptyNode(this));
			return this;
		}
		@Override
		public Node replaceSub(Node sub, NodeType newType) {
			Node n = super.replaceSub(sub, newType);
			if(sub.type==NodeType.empty)
				this.subs.add(new EmptyNode(this));
			return n;
		}
		@Override
		public String getReqName(Node s) {
			return String.format("bus%d_%d.req", id, s.id);
		}
		@Override
		public String getAckName(Node s) {
			return String.format("bus%d_%d.done", id, s.id);
		}
		@Override
		public void writeInit(PrintStream out) {
			for(Node s : subs) {
				s.writeInit(out);
				out.printf("#assign bus%d_%d \".MasterNode\"\n\n", id, s.id);
			}
			out.printf("#assign bus%d \".SlaveNode\"\n", id);
			out.printf("#setup bus%d \"%s\"\n", id, Simulator.commSetup);
		}
		@Override
		public void writeConnect(PrintStream out) {
			for(Node s : subs) {
				s.writeConnect(out);
				out.printf("bus%d_%d(%d)\n", id, s.id, s.id);
				out.printf("bus%d_%d.link = bus%d.link\n\n", id, s.id, id);
			}
			out.printf("bus%d.ack = %s\n", id, parent.getAckName(this));
			out.printf("%s = bus%d.mem_req\n", parent.getReqName(this), id);
		}
		@Override
		public boolean acceptChild(NodeType type) {
			return type.isCore() || type.isCache() || type==NodeType.empty;
		}
	}
	
	public class MemoryNode extends Node {
		public MemoryNode() {
			super(NodeType.memory);
			subs.add(new EmptyNode(this));
		}
		@Override
		public Node add(Node... subs) {
			this.subs.clear();
			return super.add(subs[0]);
		}
		@Override
		public String getReqName(Node s) {
			return "mem.req";
		}
		@Override
		public String getAckName(Node s) {
			return "mem.done";
		}
		@Override
		public void writeInit(PrintStream out) {
			subs.get(0).writeInit(out);
			out.printf("#assign mem \".Mem\"\n");
			out.printf("#setup mem \"%s\"\n\n", Simulator.memSetup);
		}
		@Override
		public void writeConnect(PrintStream out) {
			subs.get(0).writeConnect(out);
			out.println();
		}
		public boolean acceptChild(NodeType type) {
			return type!=NodeType.empty && type!=NodeType.memory;
		}
	}

	public MemoryNode memory;
	private List<Integer> appIds;
	
	private int work;
	
	public BusBuilder() {
		memory = (MemoryNode)new MemoryNode().add(new BusNode().add(
					new CacheNode(NodeType.cacheL2_A7).add(new BusNode().add(
							new CacheNode(NodeType.cacheL1_A7).add(new CoreNode(NodeType.coreA7)),
							new CacheNode(NodeType.cacheL1_A7).add(new CoreNode(NodeType.coreA7)),
							new CacheNode(NodeType.cacheL1_A7).add(new CoreNode(NodeType.coreA7)),
							new CacheNode(NodeType.cacheL1_A7).add(new CoreNode(NodeType.coreA7))
						)),
					new CacheNode(NodeType.cacheL2_A15).add(new BusNode().add(
							new CacheNode(NodeType.cacheL1_A15).add(new CoreNode(NodeType.coreA15)),
							new CacheNode(NodeType.cacheL1_A15).add(new CoreNode(NodeType.coreA15)),
							new CacheNode(NodeType.cacheL1_A15).add(new CoreNode(NodeType.coreA15)),
							new CacheNode(NodeType.cacheL1_A15).add(new CoreNode(NodeType.coreA15))
						))
				));
	}
	
	public Node undo() {
		if(undoList.isEmpty())
			return null;
		UndoReplace u = undoList.removeLast();
		if(u.removed)
			u.parent.subs.add(u.index, u.oldSub);
		else
			u.parent.subs.set(u.index, u.oldSub);
		return u.oldSub;
	}
	
	public void writeCode(File f) throws IOException {
		writeCode(new PrintStream(f)).close();
	}
	
	public PrintStream writeCode(PrintStream out) {
		out.println("#aliaspk \"ncl.cs.prime.archon.arch.modules.hicore\"");
		out.println("#estim \".HiEstimation\"");

		appIds = new ArrayList<>();
		memory.updateIds(0);

		work = Simulator.totalWork;
		if(Simulator.splitWork)
			work /= appIds.size();
		
		memory.writeInit(out);
		memory.writeConnect(out);
		
		out.println("@loop");
		out.println("!");
		for(int i : appIds) {
			out.printf("[^app%d.done] #jump @loop\n", i);
		}
		out.println("!stop");
		return out;
	}
	
	public static BusBuilder createSimpleA7Cache(int ncores) {
		BusBuilder builder = new BusBuilder();
		builder.memory = builder.new MemoryNode();
		BusNode bus = builder.new BusNode();
		builder.memory.add(bus);
		for(int i=0; i<ncores; i++)
			bus.add(builder.new CacheNode(NodeType.cacheL1_A7).add(builder.new CoreNode(NodeType.coreA7)));
		return builder;
	}
	
	public static void main(String[] args) {
		Simulator.cacheMissPercent = 0;
		Simulator.splitWork = true;
				
		for(int i=1; i<=10; i++) {
			System.out.printf("cores: %d\tmem: %d\t", i*i, 4);
			final NocBuilder b = NocBuilder.createMemBoxNoc(i, i, NocBuilder.NodeType.coreA7cache);
			// final BusBuilder b = BusBuilder.createSimpleA7Cache(i*i);
			// b.writeCode(System.out);
			new Simulator(false, new Simulator.SimpleListener()) {
				@Override
				protected void writeCode(File f) throws IOException {
					b.writeCode(f);
				}
			}.simulateOnce().getEst().dump();
		}
	}
}
