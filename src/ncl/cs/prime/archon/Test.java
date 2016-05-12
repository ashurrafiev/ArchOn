package ncl.cs.prime.archon;

import ncl.cs.prime.archon.arch.Architecture;
import ncl.cs.prime.archon.arch.RouteController;
import ncl.cs.prime.archon.arch.modules.IntAlu;
import ncl.cs.prime.archon.arch.modules.IntReg;

public class Test {

	public static void main(String[] args) {
		Architecture arch = new Architecture();
		RouteController router = new RouteController(arch);
		
		try {
			
			int[] alu = arch.addModules(IntAlu.class, 2);
			int r0 = arch.addModules(IntReg.class, 1)[0];
			
			router.debugSetValue(alu[0], 1);
			router.debugSetValue(alu[1], 5);
			router.debugSetValue(r0, 1);
			
			router.configure(alu[0], 1); // add
			
			router.connect(alu[0], alu[0]);
			router.connect(alu[0]+1, r0);
			router.connect(r0, alu[0]);
			
			router.configure(alu[1], 4); // dec
			router.connect(alu[1], alu[1]);
			
//			for(int i=0; i<10; i++) {
			while(!router.checkCondition(alu[1]+IntAlu.ZERO)) {
				System.out.println(router.<Integer>debugGetValue(alu[0]));
				System.out.println("\t\tc = " + router.<Integer>debugGetValue(alu[1]));
				router.next();
			}
			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}
