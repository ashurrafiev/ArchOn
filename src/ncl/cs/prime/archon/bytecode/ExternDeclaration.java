package ncl.cs.prime.archon.bytecode;

import java.io.File;

import ncl.cs.prime.archon.arch.Module;

public class ExternDeclaration extends Module.Declaration implements Instructions {

	public int[] inputVars = null;
	public int[] outputVars = null;
	public int[] flagVars = null;
	
	private static void readType(InstructionPointer ip, String[] names, int[] vars) {
		for(int i=0; i<names.length; i++) {
			names[i] = ip.nextString();
			vars[i] = ip.nextInt();
		}
	}
	
	public static ExternDeclaration read(InstructionPointer ip) {
		ip.reset();
		ExternDeclaration decl = new ExternDeclaration();
		byte type;
		int num;
		loop: for(;;) {
			type = ip.next();
			switch(type) {
				case EXTERN_IN:
					num = ip.next();
					decl.inputNames = new String[num];
					decl.inputVars = new int[num];
					readType(ip, decl.inputNames, decl.inputVars);
					break;
				case EXTERN_OUT:
					num = ip.next();
					decl.outputNames = new String[num];
					decl.outputVars = new int[num];
					readType(ip, decl.outputNames, decl.outputVars);
					break;
				case EXTERN_FLAG:
					num = ip.next();
					decl.flagNames = new String[num];
					decl.flagVars = new int[num];
					readType(ip, decl.flagNames, decl.flagVars);
					break;
				default:
					break loop;
			}
		}
		ip.back();
		return decl;
	}
	
	public static ExternDeclaration read(String fileName) {
		File f = CompileManager.instance.getFile(fileName);
		InstructionPointer ip = new InstructionPointer();
		CompileManager.instance.load(ip, f);
		return read(ip);
	}

	public static void skip(InstructionPointer ip) {
		ip.reset();
		byte type;
		int num;
		loop: for(;;) {
			type = ip.next();
			switch(type) {
				case EXTERN_IN:
				case EXTERN_OUT:
				case EXTERN_FLAG:
					num = ip.next();
					for(int i=0; i<num; i++) {
						ip.nextString();
						ip.nextInt();
					}
					break;
				default:
					break loop;
			}
		}
		ip.back();
	}

}
