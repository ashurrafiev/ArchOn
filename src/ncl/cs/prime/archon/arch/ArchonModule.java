package ncl.cs.prime.archon.arch;

import java.io.File;

import ncl.cs.prime.archon.bytecode.CodeExecutor;
import ncl.cs.prime.archon.bytecode.CompileManager;
import ncl.cs.prime.archon.bytecode.ExternDeclaration;
import ncl.cs.prime.archon.bytecode.CodeExecutor.ExecMode;

public class ArchonModule extends Module {

	public static boolean verbose = true;
	
	private static void report(String s) {
		if(verbose)
			System.out.println(s);
	}
	
	private CodeExecutor exec;
	
	private ExternDeclaration decl;
	private InPort<?>[] inputs = null;
	private OutPort<?>[] outputs = null;
	private FlagOutPort[] flags = null;
	
	public String name = "...";
	public boolean step = false;
	
	@Override
	public void initPorts() {
		decl = ExternDeclaration.read(exec.getIP());
		exec.getIP().reset();
		if(decl!=null) {
			if(decl.inputVars!=null) {
				inputs = new InPort<?>[decl.inputVars.length];
				for(int i=0; i<decl.inputVars.length; i++) {
					inputs[i] = new InPort<>(this);
				}
			}
			if(decl.outputVars!=null) {
				outputs = new OutPort<?>[decl.outputVars.length];
				for(int i=0; i<decl.outputVars.length; i++) {
					outputs[i] = new OutPort<>(this, 0);
				}
			}
			if(decl.flagVars!=null) {
				flags = new FlagOutPort[decl.flagVars.length];
				for(int i=0; i<decl.flagVars.length; i++) {
					flags[i] = new FlagOutPort(this);
				}
			}
		}
		super.initPorts();
	}
	
	@Override
	protected InPort<?>[] initInputs() {
		return inputs;
	}

	@Override
	protected OutPort<?>[] initOutputs() {
		return outputs;
	}

	@Override
	protected FlagOutPort[] initFlags() {
		return flags;
	}
	
	@Override
	protected void update() {
		if(decl!=null && decl.inputVars!=null) {
			for(int i=0; i<decl.inputVars.length; i++) {
				exec.getRouter().debugSetValue(decl.inputVars[i], inputs[i].getValue());
			}
		}
		
		if(step) {
			report("Step "+name);
			exec.execute(null, ExecMode.step);
		}
		else {
			report("Executing "+name);
			exec.execute(null, ExecMode.normal);
			report("Stopped executing "+name);
		}
		
		if(decl!=null && decl.outputVars!=null) {
			for(int i=0; i<decl.outputVars.length; i++) {
				outputs[i].value = exec.getRouter().debugGetValue(decl.outputVars[i]);
			}
		}
		if(decl!=null && decl.flagVars!=null) {
			for(int i=0; i<decl.flagVars.length; i++) {
				flags[i].value = ((Integer)exec.getRouter().debugGetValue(decl.outputVars[i]))!=0;
			}
		}
	}
	
	public void finishInitialise() {
		report("Initialising "+name);
		exec.execute(null, ExecMode.init);
	}
	
	public void silense() {
		exec.setDebugOutput(null);
	}
	
	public void makeDependent() {
		exec.dependent = true;
	}
	
	@Override
	public void recompute(Estimation est) {
		invalidate();
		super.recompute(est);
	}
	
	public static ArchonModule get(int var, String fileName) {
		File f = CompileManager.instance.getFile(fileName);
		ArchonModule module = new ArchonModule();
		module.exec = new CodeExecutor();
		CompileManager.instance.load(module.exec.getIP(), f);
		module.initPorts();
		return module;
	}
	
	public static ArchonModule get(Architecture arch, int var) {
		return (ArchonModule) arch.getModuleById(var);
	}

}
