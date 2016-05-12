package ncl.cs.prime.archon.parse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import ncl.cs.prime.archon.arch.Architecture;
import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.bytecode.InstructionPointer;
import ncl.cs.prime.archon.bytecode.Instructions;

public class Decompiler implements Instructions {

	private InstructionPointer ip;
	private int addr;
	private String condition;

	public boolean showAddresses = true;
	
	private class VarInfo {
		public String name;
		public ClassInfo classInfo;
	}
	private HashMap<Integer, VarInfo> vars;
	
	private class ClassInfo {
		public String name;
		public String varPrefix;
		public Module.Declaration declaration = null;
	}
	private ArrayList<ClassInfo> classes;

	private HashSet<Integer> labels;
	
	StringBuilder out = null;

	public Decompiler(InstructionPointer ip) {
		this.ip = ip;
		ip.reset();
		addr = 0;
		condition = "";
		vars = new HashMap<>();
		classes = new ArrayList<>();
		labels = new HashSet<>();
	}
	
	private void add(Object... list) {
		if(out!=null) {
			if(labels.contains(addr))
				out.append("@L"+addr+"\n");
			if(showAddresses)
				out.append("/* "+addr+" */\t");
			if(!condition.isEmpty()) {
				out.append(condition);
				condition = "";
			}
			for(Object o : list) {
				out.append(o);
				out.append(" ");
			}
			out.append("\n");
		}
	}
	
	private void loadAliases() {
		for(int i=0; ; i++) {
			if((int) ip.next()!=i)
				return;
			String clsName = ip.nextString();
			if(out==null) {
				ClassInfo info = new ClassInfo();
				info.name = clsName;
				try {
					Class<?> cls = ClassLoader.getSystemClassLoader().loadClass(clsName);
					info.varPrefix = cls.getSimpleName()+"_";
					Method getDeclaration = cls.getMethod("getDeclaration");
					info.declaration = (Module.Declaration) getDeclaration.invoke(null);
				} catch (Exception e) {
					throw new RuntimeException("Cannot instantiate module class: "+clsName);
				}
				classes.add(info);
			}
		}
	}
	
	private String alias(int alias) {
		return "\""+((alias<0 || alias>=classes.size()) ? "(unknown)" : classes.get(alias).name)+"\"";
	}
	
	private String label(int rel) {
		if(out==null) {
			labels.add(ip.getAddress()+rel);
		}
		return "@L"+(ip.getAddress()+rel);
	}
	
	private String var(int id) {
		if(out==null)
			return null;
		int module = (id&Architecture.MODULE_MASK);
		VarInfo v = vars.get(module);
		if(v==null) return "?"+module;
		return v.name;
	}
	
	private String var(int id, int alias) {
		int module = (id&Architecture.MODULE_MASK);
		VarInfo v = vars.get(module);
		if(v==null) {
			v = new VarInfo();
			ClassInfo cls = (alias<0 || alias>=classes.size()) ? null : classes.get(alias);
			String s = (cls==null) ? "UNK" : cls.varPrefix;
			s = s + (module>>8);
			v.name = s;
			v.classInfo = cls;
			vars.put(module, v);
		}
		return v.name;
	}
	
	private String getDeclared(String[] decl, String fmt, int id) {
		id = id&Architecture.PORT_MASK;
		if(decl==null) {
			if(id!=0)
				return String.format(fmt, "?");
			return "";
		}
		if(id<0 || id>=decl.length)
			return String.format(fmt, "?");
		if(decl.length<2)
			return "";
		return String.format(fmt, decl[id]);
			
	}
	
	private String dest(int id) {
		if(out==null)
			return null;
		VarInfo v = vars.get(id&Architecture.MODULE_MASK);
		if(v==null || v.classInfo==null) return var(id)+".?";
		return v.name+getDeclared(v.classInfo.declaration.inputNames, ".%s", id);
	}

	private String source(int id) {
		if(out==null)
			return null;
		VarInfo v = vars.get(id&Architecture.MODULE_MASK);
		if(v==null || v.classInfo==null) return var(id)+".?";
		return v.name+getDeclared(v.classInfo.declaration.outputNames, ".%s", id);
	}
	
	private String flag(int id) {
		if(out==null)
			return null;
		VarInfo v = vars.get(id&Architecture.MODULE_MASK);
		if(v==null || v.classInfo==null) return var(id)+".?";
		return v.name+getDeclared(v.classInfo.declaration.flagNames, ".%s", id);
	}
	
	private String flag(int id, int flag) {
		if(out==null)
			return null;
		VarInfo v = vars.get(id&Architecture.MODULE_MASK);
		if(v==null || v.classInfo==null) return var(id)+".?";
		return v.name+getDeclared(v.classInfo.declaration.flagNames, ".%s", flag);
	}
	
	private String config(int id, int config) {
		if(out==null)
			return null;
		VarInfo v = vars.get(id&Architecture.MODULE_MASK);
		if(v==null || v.classInfo==null) return var(id)+"(?)";
		return v.name+getDeclared(v.classInfo.declaration.configNames, "(%s)", config);
	}

	private String str() {
		return "\""+ip.nextString()+"\"";
	}
	
	private void decodeNext() {
		addr = ip.getAddress();
		int cmd = ip.next();
		switch(cmd) {
		
			case ALIASES_START:
				loadAliases();
				break;
				
			case D_ESTIM:
				add("#estim", str());
				break;
				
			case D_EST_PRINT:
				add("#estprint");
				break;
				
			case D_PRINT:
				add("#print", dest(ip.nextInt()));
				break;
				
			case D_PRINT_STR:
				add("#print", str());
				break;
				
			case D_PRINT_LN:
				add("#print");
				break;
				
			case D_INIT_INT:
			{
				String dest = dest(ip.nextInt());
				add("#init", dest, ip.nextInt());
				break;
			}
			case D_PARAM_INT:
			{
				int arg = ip.nextInt();
				String dest = dest(ip.nextInt());
				int value = ip.nextInt();
				add("#param", arg, dest, value);
				break;
			}	
			
			case I_UNLINK:
				add(dest(ip.nextInt()), "=X");
				break;
				
			case I_UNLINK_ALL:
				add("!x");
				break;
				
			case I_LINK:
			{
				String dest = dest(ip.nextInt());
				String src = source(ip.nextInt());
				add(dest, "=", src);
				break;
			}
				
			case I_LINK_FLAG:
			{
				String dest = dest(ip.nextInt());
				int srcId = ip.nextInt();
				String src = source(srcId);
				add(dest, "=["+flag(srcId, ip.next())+"]", src);
				break;
			}
			
			case I_LINK_NFLAG:
			{
				String dest = dest(ip.nextInt());
				int srcId = ip.nextInt();
				String src = source(srcId);
				add(dest, "=[^"+flag(srcId, ip.next())+"]", src);
				break;
			}
			
			case I_CONFIG:
			{
				int destId = ip.nextInt();
				add(config(destId, ip.nextInt()));
				break;
			}
			
			case I_JUMP:
				add("#jump", label(ip.nextInt()));
				break;
				
			case I_NEXT:
				add("!");
				break;
				
			case I_NEXT_COUNT:
			{
				add("!"+ip.next());
				break;
			}
			
			case I_STOP:
				add("!stop");
				break;
				
			case C_FLAG:
				condition = "["+flag(ip.nextInt())+"] ";
				break;
	
			case C_NFLAG:
				condition = "[^"+flag(ip.nextInt())+"] ";
				break;
				
			case I_ASSIGN:
			{
				int alias = ip.next();
				String var = var(ip.nextInt(), alias);
				add("#assign", var, alias(alias));
				break;
			}
			
			case I_FREE:
			{
				String var = var(ip.nextInt());
				add("#free", var);
				break;
			}
			
			default:
				add("#nop", "//", "?"+cmd);
		}
	}
	
	public String decompile() {
		ip.reset();
		try {
			while(!ip.outOfRange())
				decodeNext();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		out = new StringBuilder();
		ip.reset();
		condition = "";
		try {
			while(!ip.outOfRange())
				decodeNext();
		}
		catch(Exception e) {
			e.printStackTrace();
			out.append("/* **DECOMPILER EXCEPTION** */");
		}
		return out.toString();
	}
	
	public static void main(String[] args) {
		try {
//			File f = new File("convo_arm.sim.bin");
			File f = new File("generations/best_30.sim.bin");
			InstructionPointer ip = new InstructionPointer();
			ip.loadCode(f);
			Decompiler dc = new Decompiler(ip);
			dc.showAddresses = false;
			String path = f.getAbsolutePath();
			File out = new File(path.substring(0, path.lastIndexOf('.', path.length()-5))+"_decomp.sim");
			FileOutputStream fout = new FileOutputStream(out);
			fout.write(dc.decompile().getBytes());
			fout.close();
			System.out.println("Done writing "+out.getAbsolutePath());
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
