package ncl.cs.prime.archon.bytecode;

import java.io.PrintStream;

import ncl.cs.prime.archon.arch.Architecture;
import ncl.cs.prime.archon.arch.ArchonModule;
import ncl.cs.prime.archon.arch.Estimation;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.arch.RouteController;

public class CodeExecutor implements Instructions {

	public enum ExecMode {
		normal, step, init
	}
	
	private Architecture arch = new Architecture();
	private RouteController router = new RouteController(arch);
	private Estimation est = null;
	
	private ExternDeclaration extern = null;
	public boolean dependent = false;
	
	private int[] params = null;
	private InstructionPointer ip = new InstructionPointer();
	private int resumePoint = 0;
	
	private PrintStream debug = System.out;
	
	@SuppressWarnings("unchecked")
	private Class<? extends Module>[] aliases = new Class[128];
	
	public Architecture getArch() {
		return arch;
	}
	
	public RouteController getRouter() {
		return router;
	}
	
	public Estimation getEst() {
		return est;
	}
	
	public InstructionPointer getIP() {
		return ip;
	}
	
	public void setDebugOutput(PrintStream debug) {
		this.debug = debug;
	}
	
	private void error(String msg) {
		throw new RuntimeException("Error@"+ip.getAddress()+": "+msg);
	}
	
	@SuppressWarnings("unchecked")
	private void loadAliases() {
		for(int i=0; ; i++) {
			if((int) ip.next()!=i)
				return;
			String clsName = ip.nextString();
			try {
				Class<? extends Module> cls = (Class<Module>) ClassLoader.getSystemClassLoader().loadClass(clsName);
				aliases[i] = cls;
			} catch (ClassNotFoundException e) {
				error("Cannot instantiate module class: "+clsName);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createEstimation(String clsName) {
		if(est!=null) {
			error("Estimation already initialised");
			return;
		}		
		try {
			Class<? extends Estimation> cls = (Class<Estimation>) ClassLoader.getSystemClassLoader().loadClass(clsName);
			est = cls.newInstance();
			router.setEstimation(est);
		} catch (Exception e) {
			error("Cannot start estimation from class: "+clsName);
		}
	}

	public void setEst(Estimation est) {
		if(this.est!=null) {
			error("Estimation already initialised");
			return;
		}		
		this.est = est;
		router.setEstimation(est);
	}
	
	public void skipNext() {
		byte cmd = ip.next();
		
		switch(cmd) {
		
			case ALIASES_START:
			case D_PRINT:
			case D_PRINT_STR:
			case D_PRINT_LN:
			case D_ESTIM:
			case D_EST_PRINT:
				error("Cannot condition init/debug operations");
				return;

			case I_NEXT:
			case I_STOP:
			case I_UNLINK_ALL:
				return;
			case I_NEXT_COUNT:
				ip.jump(1);
				return;
			case I_UNLINK:
			case I_JUMP:
			case C_FLAG:
			case C_NFLAG:
			case I_FREE:
				ip.jump(4);
				return;
			case I_ASSIGN:
				ip.jump(5);
				return;
			case I_LINK:
			case I_CONFIG:
				ip.jump(8);
				return;
			case I_LINK_FLAG:
			case I_LINK_NFLAG:
				ip.jump(9);
				return;
		}
	}
	
	private boolean isExtern(int srcId) {
		if(extern==null || !dependent)
			return false;
		if(extern.inputVars==null)
			return false;
		for(int i=0; i<extern.inputVars.length; i++) {
			if(extern.inputVars[i]==srcId)
				return true;
		}
		return false;
	}
	
	public boolean executeNext(ExecMode execMode) {
		byte cmd = ip.next();
		
		switch(cmd) {
		
			case ALIASES_START:
				loadAliases();
				break;
				
			case EXTERN_IN:
			case EXTERN_OUT:
			case EXTERN_FLAG:
				extern = ExternDeclaration.read(ip);
				break;
				
			case D_ESTIM:
				createEstimation(ip.nextString());
				break;
				
			case D_EST_PRINT:
				if(est!=null)
					est.dump();
				break;
				
			case D_PRINT:
				if(debug!=null)
					debug.print(router.debugGetValue(ip.nextInt()));
				else
					ip.nextInt();
				break;
				
			case D_PRINT_STR:
				if(debug!=null)
					debug.print(ip.nextString());
				else
					ip.nextString();
				break;
				
			case D_PRINT_LN:
				if(debug!=null)
					debug.println();
				break;
				
			case D_INIT_INT:
			{
				int srcId = ip.nextInt();
				int value = ip.nextInt();
				if(!isExtern(srcId))
					router.debugSetValue(srcId, value);
				break;
			}
			case D_PARAM_INT:
			{
				int arg = ip.nextInt();
				int srcId = ip.nextInt();
				int value = ip.nextInt();
				if(params!=null && arg>=0 && arg<params.length)
					value = params[arg];
				if(!isExtern(srcId))
					router.debugSetValue(srcId, value);
				break;
			}	
			
			case I_UNLINK:
				router.disconnect(ip.nextInt());
				break;
				
			case I_UNLINK_ALL:
				router.disconnectAll();
				break;
				
			case I_LINK:
			{
				int destId = ip.nextInt();
				int srcId = ip.nextInt();
				router.connect(destId, srcId);
				break;
			}
				
			case I_LINK_FLAG:
			{
				int destId = ip.nextInt();
				int srcId = ip.nextInt();
				router.connect(destId, srcId, ip.next(), false);
				break;
			}
			
			case I_LINK_NFLAG:
			{
				int destId = ip.nextInt();
				int srcId = ip.nextInt();
				router.connect(destId, srcId, ip.next(), true);
				break;
			}
			
			case I_CONFIG:
			{
				int destId = ip.nextInt();
				router.configure(destId, ip.nextInt());
				break;
			}
			
			case I_JUMP:
				ip.jump(ip.nextInt());
				break;
				
			case I_NEXT:
				if(execMode==ExecMode.step || execMode==ExecMode.init) {
					ip.back();
					return false;
				}
				return router.next();
				
			case I_NEXT_COUNT:
			{
				if(execMode==ExecMode.step || execMode==ExecMode.init) {
					ip.back();
					return false;
				}
				int n = ip.next();
				for(; n>0; n--)
					if(!router.next())
						return false;
				return true;
			}
			
			case I_STOP:
				ip.jumpAbs(resumePoint);
				return execMode==ExecMode.step;
			case I_ACK:
				return execMode==ExecMode.step;
			
			case I_RESUME:
				resumePoint = ip.getAddress();
				return execMode!=ExecMode.init;
				
			case C_FLAG:
				if(router.checkCondition(ip.nextInt()))
					return executeNext(execMode);
				else
					skipNext();
				break;

			case C_NFLAG:
				if(!router.checkCondition(ip.nextInt()))
					return executeNext(execMode);
				else
					skipNext();
				break;
				
			case I_ASSIGN:
			{
				byte c = ip.next();
				int var = ip.nextInt();
				arch.assign(var, aliases[c]);
				break;
			}
			
			case I_FREE:
			{
				int var = ip.nextInt();
				arch.free(var);
				break;
			}
			
			case I_SIM:
			{
				String fileName = ip.nextString();
				int var = ip.nextInt();
				arch.addModule(var, ArchonModule.get(var, fileName));
				break;
			}
			case I_SIM_NAME:
				ArchonModule.get(arch, ip.nextInt()).name = ip.nextString(); 
				break;
			case I_SIM_SILENT:
				ArchonModule.get(arch, ip.nextInt()).silense();
				break;
			case I_SIM_STEP:
				ArchonModule.get(arch, ip.nextInt()).step = true;
				break;
			case I_SIM_DEP:
				ArchonModule.get(arch, ip.nextInt()).makeDependent();
				break;
			case I_SIM_END_INIT:
				ArchonModule.get(arch, ip.nextInt()).finishInitialise();
				break;
				
		}
		
		return true;
	}
	
	public void execute(int[] params, ExecMode execMode) {
		this.params = params;
		ExecMode m = ExecMode.normal;
		while(executeNext(m)) {
			m = execMode;
		}
	}
	
	public void execute(int[] params) {
		execute(params, ExecMode.normal);
	}
	
	public void executeFirst(int[] params) {
		this.params = params;
	}
	
}
