package ncl.cs.prime.archon.hicoredemo.python;

import org.python.core.PyInteger;
import org.python.util.PythonInterpreter;

import ncl.cs.prime.archon.hicoredemo.NocBuilder;

public class PyNocBuilder extends NocBuilder {

	public static final String scriptPath = "examples/nocbuild.py";
	
	public PyNocBuilder(int width, int height) {
		super(width, height);
	}
	
	public void set(int x, int y, NodeType node) {
		mesh[x][y] = node;
	}
	
	public static NocBuilder createFromFile(String path, int size) {
		try {
			/*FileInputStream f = new FileInputStream(new File(path));
			byte[] buf = new byte[f.available()];
			f.read(buf);
			f.close();
			String script = new String(buf);*/
			
			PythonInterpreter python = new PythonInterpreter();
			python.exec("from ncl.cs.prime.archon.hicoredemo.python import PyNocBuilder");
			python.exec("from ncl.cs.prime.archon.hicoredemo.NocBuilder import NodeType");
			python.set("size", new PyInteger(size));
			
			python.execfile(path);
			
			PyNocBuilder builder = python.get("builder", PyNocBuilder.class);
			python.close();
			return builder;
		}
		catch (Exception e) {
			return null;
		}
	}

}
