package ncl.cs.prime.archon.genetic;

public interface BasicGenome {

	public byte[] getCode();
	
	public void setFitness(long fitness);
	public long getFitness();
	public int compareFitness(BasicGenome g);

}
