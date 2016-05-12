package ncl.cs.prime.archon.arch;

import java.util.ArrayList;
import java.util.HashMap;

public class Architecture {

	public static final int DYNAMIC_ID_MASK = 0x80000000;
	public static final int MODULE_MASK = 0x7fffff00;
	public static final int PORT_MASK = 0x000000ff;
	
	protected ArrayList<InPort<?>> inputs = new ArrayList<>();
	protected ArrayList<Module> modules = new ArrayList<>();
	
	private HashMap<Integer, Module> dynamicIdMap = new HashMap<>(); // for dynamic module assignment
	
	private int addModule(Module m) {
		int id = modules.size(); 
		modules.add(m);
		InPort<?>[] ins = m.getInputs();
		if(ins!=null) {
			for(InPort<?> p : ins)
				inputs.add(p);
		}
		return id<<8; // module index to id
	}

	private void registerAssignment(int var, int id) {
		dynamicIdMap.put((var & MODULE_MASK)>>8, modules.get((id & MODULE_MASK)>>8));
	}
	
	public int addModule(int var, Module m) {
		int id = addModule(m);
		registerAssignment(var, id);
		return id;
	}

	public int[] addModules(Class<? extends Module> cls, int num) throws ArchitectureException {
		int[] ids = new int[num];
		for(int i=0; i<num; i++) {
			Module m = null; 
			try {
				m = cls.newInstance();
			}
			catch(Exception e) {
				throw new ArchitectureException(ArchitectureException.MODULE_INSTANTIATION_ERROR);
			}
			m.initPorts();
			ids[i] = addModule(m);
		}
		return ids;
	}
	
	public long syncTime() {
		long t = 0;
		for(Module m : modules)
			if(m.getTime()>t) t = m.getTime();
		for(Module m : modules)
			m.syncTime(t);
		return t;
	}
	
	public int assign(int var, Class<? extends Module> cls) throws ArchitectureException {
		// TODO stub
		int id = addModules(cls, 1)[0];
		registerAssignment(var, id);
		return id;
	}
	
	public void free(int var) {
		// TODO stub
		dynamicIdMap.remove((var & MODULE_MASK)>>8);
	}
	
	protected Module getModuleById(int id) {
		if((id & DYNAMIC_ID_MASK) != 0) {
			return dynamicIdMap.get((id & MODULE_MASK)>>8);
		}
		else {
			return modules.get((id & MODULE_MASK)>>8);
		}
	}
	
	public static int portFromId(int id) {
		return id & PORT_MASK;
	}
	
}
