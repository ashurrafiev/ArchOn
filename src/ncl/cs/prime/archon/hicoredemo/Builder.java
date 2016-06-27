package ncl.cs.prime.archon.hicoredemo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public interface Builder {
	public void writeCode(File f) throws IOException;
	public PrintStream writeCode(PrintStream out);
}
