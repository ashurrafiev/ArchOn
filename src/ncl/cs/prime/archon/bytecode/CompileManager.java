package ncl.cs.prime.archon.bytecode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public class CompileManager {

	public static final CompileManager instance = new CompileManager();
	
	private HashMap<String, File> compiledFiles = new HashMap<>();	
	
	public File getFile(String fileName) {
		File f;
		if(fileName.endsWith("bin")) {
			f = new File(fileName);
		}
		else {
			f = compiledFiles.get(fileName);
			if(f==null) {
				f = new File(fileName);
				ProgramParserBytecode p = new ProgramParserBytecode();
				if(p.compile(f, false)!=null) {
					System.out.println("Done compiling "+fileName);
					f = new File(f.getAbsolutePath()+".bin");
					compiledFiles.put(fileName, f);
				}
				else {
					throw new RuntimeException("Failed compiling "+fileName);
				}
			}
		}
		return f;
	}
	
	public void load(InstructionPointer ip, File f) {
		try {
			ip.loadCode(f);
		} catch (IOException e) {
			throw new RuntimeException("Error loading "+f.getName()+": "+e.getMessage());
		}
	}
	
}
