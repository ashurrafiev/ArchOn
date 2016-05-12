package ncl.cs.prime.archon.bytecode;

import java.util.LinkedList;
import java.util.Random;

public class FaultyCodeExecutor extends CodeExecutor {

	public static final int MAX_ERRORS = 100000;
	
	private int faultChance;
	private long counterLimit = -1; 
	
	private Random random = new Random();
	public LinkedList<ErrorInfo> errors = new LinkedList<>();
	public LinkedList<InjectionInfo> injections = new LinkedList<>();
	
	public long counter = 0;
	
	public FaultyCodeExecutor(int faultChance) {
		this.faultChance = faultChance;
	}
	
	public FaultyCodeExecutor(int faultChance, long counterLimit) {
		this.faultChance = faultChance;
		this.counterLimit = counterLimit;
	}
	
	public class InjectionInfo {
		public int addr;
		public long counter;
		public byte cmd;
		public InjectionInfo(byte cmd, int addr, long counter) {
			this.cmd = cmd;
			this.addr = addr;
			this.counter = counter;
		}
	}

	public class ErrorInfo {
		public int addr;
		public long counter;
		public String msg;
		public ErrorInfo(String msg, int addr, long counter) {
			this.msg = msg;
			this.addr = addr;
			this.counter = counter;
		}
	}

	@Override
	public boolean executeNext(ExecMode execMode) {
		int addr = getIP().getAddress();
		counter++;
		try {
			if(getIP().outOfRange()) {
				errors.add(new ErrorInfo("Code memory range error.", addr, counter));
				return false;
			}
			if(counterLimit>0 && counter>=counterLimit) {
				errors.add(new ErrorInfo("Simulaton limit reached, infinite loop?", addr, counter));
				return false;
			}
			if(faultChance<=0 || random.nextInt(faultChance)>0)
				return super.executeNext(execMode);
			else {
				injections.add(new InjectionInfo(getIP().codeAt(addr), addr, counter));
//				faultChance = 0;
				skipNext();
				return true;
			}
		}
		catch(Exception e) {
//			e.printStackTrace();
			errors.add(new ErrorInfo(e.getMessage(), addr, counter));
			if(errors.size()>=MAX_ERRORS)
				return false;
			return true;
		}
	}
	
}
