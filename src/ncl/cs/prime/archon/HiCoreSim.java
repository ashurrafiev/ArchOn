package ncl.cs.prime.archon;

import java.io.File;
import java.io.IOException;

import ncl.cs.prime.archon.hicoredemo.Builder;
import ncl.cs.prime.archon.hicoredemo.BusBuilder;
import ncl.cs.prime.archon.hicoredemo.NocBuilder;
import ncl.cs.prime.archon.hicoredemo.Simulator;

public class HiCoreSim {
	
	private static final int BUS = 0;
	private static final int NOC_A = 1;
	private static final int NOC_B = 2;
	private static final int NOC_C = 3;
	private static final int NOC_D = 4;

	public static Builder createBuilder(int type, int i) {
		switch (type) {
			case NOC_A:
				return NocBuilder.createSingleMemNoc(i, i, NocBuilder.NodeType.coreA7cache);
			case NOC_B:
				return NocBuilder.create4MemNoc(i, i, NocBuilder.NodeType.coreA7cache);
			case NOC_C:
				return NocBuilder.createMemRowNoc(i, i, NocBuilder.NodeType.coreA7cache);
			case NOC_D:
				return NocBuilder.createMemBoxNoc(i, i, NocBuilder.NodeType.coreA7cache);
			default:
				return BusBuilder.createSimpleA7Cache(i*i);
		}
	}
	
	public static void main(String[] args) {
		int type = BUS;
		int imin = 1;
		int imax = 10;
		for(int i=0; i<args.length; i++) {
			if("-m".equals(args[i])) {
				Simulator.cacheMissPercent = Integer.parseInt(args[++i]);
			}
			else if("-min".equals(args[i])) {
				imin = Integer.parseInt(args[++i]);
			}
			else if("-max".equals(args[i])) {
				imax = Integer.parseInt(args[++i]);
			}
			else if("-w".equals(args[i])) {
				Simulator.totalWork = Integer.parseInt(args[++i]);
			}
			else if("-nosplit".equals(args[i])) {
				Simulator.splitWork = false;
			}
			else if("-bus".equals(args[i]))
				type = BUS;
			else if("-noca".equals(args[i]))
				type = NOC_A;
			else if("-nocb".equals(args[i]))
				type = NOC_B;
			else if("-nocc".equals(args[i]))
				type = NOC_C;
			else if("-nocd".equals(args[i]))
				type = NOC_D;
		}
		
		for(int i=imin; i<=imax; i++) {
			System.out.printf("cores: %d\t", i*i);
			final Builder b = createBuilder(type, i);
			new Simulator(false, new Simulator.SimpleListener()) {
				@Override
				protected void writeCode(File f) throws IOException {
					b.writeCode(f);
				}
			}.simulateOnce().getEst().dump();
		}
	}

}
