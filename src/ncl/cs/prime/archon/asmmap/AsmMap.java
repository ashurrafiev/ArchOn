package ncl.cs.prime.archon.asmmap;

import java.io.File;
import java.io.PrintStream;

import ncl.cs.prime.archon.asmmap.arm.AluTemplate1;
import ncl.cs.prime.archon.asmmap.arm.AluTemplate2;
import ncl.cs.prime.archon.asmmap.arm.ArmRegisterMap;
import ncl.cs.prime.archon.asmmap.arm.BranchTemplate;
import ncl.cs.prime.archon.asmmap.arm.SingleMmuTemplate;

public class AsmMap {

	public static void main(String[] args) {
		new InstructionParser(new InstructionTemplate[] {
				new AluTemplate1(),
				new AluTemplate2(),
				new SingleMmuTemplate(),
				new BranchTemplate()
			}) {
			
			@Override
			protected boolean createRegisterMap(File f, PrintStream out) {
				registerMap = new ArmRegisterMap();
				return registerMap.scanVars(f, out);
			}
		}.parse(new File("asmmap_test.s"), System.out);
//		new ArmRegisterMap().scanVars(new File("asmmap_test.s"), System.out);
	}

}
