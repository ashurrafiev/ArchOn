package ncl.cs.prime.archon.arch;

public interface Estimation {

	public void init(Architecture arch);
	
	public void beginCycle();
	public void endCycle();

	public void dump();
	
}
