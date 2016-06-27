package ncl.cs.prime.archon.hicoredemo;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.CodeExecutor.ExecMode;
import ncl.cs.prime.archon.parse.ProgramParserBytecode;

public abstract class Simulator implements Runnable {

	public static String memSetup = "delayRead=100000;delayWrite=100000;energyRead=39750;energyWrite=99000;leakage=60";
	public static String commSetup = "delayHop=1333;energyHop=20000;leakage=4";
	
	public static String a7setup = "delayCpu=1270;energyCpu=88889;leakage=19";
	public static String a15setup = "delayCpu=667;energyCpu=542667;leakage=150";
	
	public static String a7L1CacheSetup = "delayHit=4000;energyHit=35000;leakage=2";
	public static String a7L2CacheSetup = "delayHit=15000;energyHit=35000;leakage=10";
	public static String a15L1CacheSetup = "delayHit=2667;energyHit=35000;leakage=2";
	public static String a15L2CacheSetup = "delayHit=10000;energyHit=35000;leakage=10";
	
	public static String[] APP_SETUP = {
			"",
			"countCpu=1000;countMemRead=0;countMemWrite=0",
			"countCpu=0;countMemRead=1000;countMemWrite=0",
	};
	
	public static String appSetup = APP_SETUP[0];
	
	public static int cacheMissPercent = 50;
	public static boolean splitWork = true;
	public static int totalWork = 640000;
	
	public static interface Listener {
		public void simulationStarted();
		public void simulationStopped();
		public void updateInfo(int simId, CodeExecutor exec);
		public void error(int simId, Exception e);
	}
	
	public static class SimpleListener implements Listener {
		@Override
		public void simulationStarted() {
		}
		@Override
		public void simulationStopped() {
		}
		@Override
		public void updateInfo(int simId, CodeExecutor exec) {
			if(exec.getEst()!=null)
				exec.getEst().dump();
		}
		@Override
		public void error(int simId, Exception e) {
			e.printStackTrace();
		}
	}
	
	private Thread simThread = null;

	private File srcFile = null;
	private Listener listener = null;
	
	private boolean ui;

	public Simulator(boolean ui) {
		this.ui = ui;
	}
	
	public Simulator(boolean ui, Listener listener) {
		this.ui = ui;
		this.listener = listener;
	}
	
	public void setListener(Listener listener) {
		this.listener = listener;
	}
	
	public void interrupt() {
		if(simThread!=null) {
			simThread.interrupt();
			stopSimulation();
		}
	}
	
	@Override
	public void run() {
		try {
			for(int i=0; simThread!=null; i++) {
				CodeExecutor exec = new CodeExecutor();
				try {
					exec.getIP().loadCode(srcFile);
					try {
						exec.execute(null, ExecMode.normal);
						if(exec.interrupted) {
							stopSimulation();
							return;
						}
					}
					catch(Exception e) {
						listener.error(i, e);
						stopSimulation();
						return;
					}
					listener.updateInfo(i, exec);
				} catch (IOException e) {
					stopSimulation();
					return;
				}
				Thread.sleep(1);
			}
		}
		catch(InterruptedException e) {
		}
		stopSimulation();
	}
	
	protected abstract void writeCode(File f) throws IOException;
	
	private File createSource() {
		File f = new File("noc_demo.sim");
		f.deleteOnExit();	
		try {
			writeCode(f);
		}
		catch(IOException e) {
			error("Cannot write temporary files.");
			return null;
		}

		ProgramParserBytecode p = new ProgramParserBytecode();
		if(p.compile(f, false)==null) {
			error("Unable to start simulation due to compile error.");
			return null;
		}
		
		f = new File(f.getAbsolutePath()+".bin");
		f.deleteOnExit();
		
		this.srcFile = f;
		return f;
	}
	
	private void error(String s) {
		if(ui)
			JOptionPane.showMessageDialog(null, s, "Error", JOptionPane.ERROR_MESSAGE);
		else
			System.err.println("ERROR: "+s);
	}
	
	public void startSimulation() {
		if(simThread!=null)
			return;
		createSource();
		simThread = new Thread(this);
		simThread.start();
		listener.simulationStarted();
	}
	
	public void stopSimulation() {
		simThread = null;
		listener.simulationStopped();
	}
	
	public CodeExecutor simulateOnce() {
		createSource();
		CodeExecutor exec = new CodeExecutor();
		try {
			exec.getIP().loadCode(srcFile);
		} catch (IOException e) {
			error("Cannot load source.");
			return null;
		}
		exec.execute(null, ExecMode.normal);
		return exec;
	}
}
