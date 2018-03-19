package ncl.cs.prime.archon;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

public class PrimeModel {

	private static final String PATH = "examples/prime_fpga.csv";
	private static final String SCALING_PATH = "examples/scaling.csv";
	public static final int MAX_CORES = 4;
	
	public static final PrimeModel model = new PrimeModel();

	private double[] v;
	private double[] f;
	private double[] p;
	private double[] lim;
	private boolean hasLim = false;
	
	private int scalingType = -1;
	private double[][] scaling = new double[3][64];
	
	private int active;
	private int ncores;
	
	public PrimeModel() {
		try {
			Scanner in = new Scanner(new FileInputStream(new File(PATH)));
			
			int n = in.nextInt();
			v = new double[n];
			f = new double[n];
			p = new double[n];
			lim = new double[n];
			hasLim = false;
			ncores = 1;
			active = -1;
			
			for(int i=0; i<n; i++)
				v[i] = in.nextDouble();
			for(int i=0; i<n; i++)
				f[i] = in.nextDouble();
			for(int i=0; i<n; i++)
				p[i] = in.nextDouble();
			
			in.close();
		}
		catch(Exception e) {
			System.err.println("Failed to load model.");
			e.printStackTrace();
			System.exit(1);
		}
		
		try {
			Scanner in = new Scanner(new FileInputStream(new File(SCALING_PATH)));
			
			for(int j=0; j<64; j++)
				for(int i=0; i<3; i++)
					scaling[i][j] = in.nextDouble();
			
			in.close();
		}
		catch(Exception e) {
			System.err.println("Failed to load scaling profile.");
			e.printStackTrace();
		}

	}
	
	public boolean setPowerLimit(double plim) {
		this.scalingType = -1;
		ncores = -1;
		active = -1;
		double max = 0;
		for(int i=v.length-1; i>=0; i--) {
			double n = plim / p[i];
			lim[i] = f[i] * n;
			n = Math.floor(n);
			if(n>MAX_CORES) n = MAX_CORES;
			if(f[i] * n > max) {
				max = f[i] * n;
				ncores = (int) n;
				active = i;
			}
		}
		hasLim = true;
		if(ncores<=0) {
			ncores = 1;
			active = 0;
			return false;
		}
		return true;
	}
	
	public boolean setPowerLimitScaled(double plim, int scalingType) {
		this.scalingType = scalingType;
		ncores = -1;
		active = -1;
		double max = 0;
		for(int i=v.length-1; i>=0; i--) {
			int maxn = (int) Math.floor(plim / p[i]);
			if(maxn>MAX_CORES) maxn = MAX_CORES;
			int n;
			double fn = 0.0;
			double prevfn = 0.0;
			for(n = maxn; n>0; n--) {
				fn = f[i] * scaling[scalingType][n-1];
				if(n<maxn && fn<prevfn) {
					fn = prevfn;
					break;
				}
				prevfn = fn;
			}
			n++;
			lim[i] = fn;
			if(fn > max) {
				max = fn;
				ncores = n;
				active = i;
			}
		}
		hasLim = true;
		if(ncores<=0) {
			ncores = 1;
			active = 0;
			return false;
		}
		return true;
	}

	
	public boolean setFreqLimit(double flim) {
		this.scalingType = -1;
		ncores = -1;
		active = -1;
		double min = Double.MAX_VALUE;
		for(int i=0; i<v.length; i++) {
			double n = flim / f[i];
			lim[i] = flim;
			n = Math.ceil(n);
			if(n>MAX_CORES) n = MAX_CORES;
			if(n * f[i] >=flim && p[i] * n < min) {
				min = p[i] * n;
				ncores = (int) n;
				active = i;
			}
		}
		hasLim = true;
		if(ncores<=0) {
			ncores = 1;
			active = v.length - 1;
			return false;
		}
		return true;
	}

	public boolean setFreqLimitScaled(double flim, int scalingType) {
		this.scalingType = scalingType;
		ncores = -1;
		active = -1;
		double min = Double.MAX_VALUE;
		for(int i=0; i<v.length; i++) {
			int n;
			double fn = 0.0;
			for(n=1; n<=MAX_CORES; n++) {
				fn = f[i] * scaling[scalingType][n-1];
				if(fn>=flim)
					break;
			}
			lim[i] = flim;
			if(fn >=flim && p[i] * n < min) {
				min = p[i] * n;
				ncores = n;
				active = i;
			}
		}
		hasLim = true;
		if(ncores<=0) {
			ncores = 1;
			active = v.length - 1;
			return false;
		}
		return true;
	}

	public int range() {
		return v.length;
	}

	public double maxF() {
		double max = 0.0;
		int n = (ncores>0) ? ncores : 1;
		for(int i=0; i<v.length; i++) {
			if(f[i]*n>max)
				max = f[i]*n;
			if(hasLim && lim[i]>max)
				max = lim[i];
		}
		if(active>=0 && f[active]*n < max*0.5) {
			max = f[active]*n*2.0;
		}
		return max;
	}
	
	public double minV() {
		double min = 1000.0;
		for(int i=0; i<v.length; i++) {
			if(v[i]<min)
				min = v[i];
		}
		return min;
	}
	
	public double maxV() {
		double max = 0.0;
		for(int i=0; i<v.length; i++) {
			if(v[i]>max)
				max = v[i];
		}
		return max;
	}
	
	public int getNCores() {
		return ncores;
	}
	
	public void setNCores(int ncores) {
		this.ncores = ncores;
	}
	
	public double setVoltage(double voltage) {
		for(int i=v.length-1; i>=0; i--) {
			if(voltage>=v[i] || i==0) {
				active = i;
				return v[i];
			}
		}
		return v[0];
	}
	
	public int getActive() {
		return active;
	}
	
	public double getV(int i) {
		return v[i];
	}

	public double getF(int i) {
		return f[i];
	}
	
	public double getFn(int i, int n) {
		if(scalingType<0)
			return n * f[i];
		else
			return f[i] * scaling[scalingType][n-1];
	}
	
	public double getP(int i) {
		return p[i];
	}
	
	public boolean hasLim() {
		return hasLim;
	}
	
	public double getLim(int i) {
		return lim[i];
	}

}
