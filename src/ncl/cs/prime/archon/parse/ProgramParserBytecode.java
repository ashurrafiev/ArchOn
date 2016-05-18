package ncl.cs.prime.archon.parse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ncl.cs.prime.archon.arch.Module;
import ncl.cs.prime.archon.bytecode.BytecodeWriter;
import ncl.cs.prime.archon.bytecode.ExternDeclaration;
import ncl.cs.prime.archon.bytecode.Instructions;

import java.util.TreeMap;

public class ProgramParserBytecode extends BytecodeWriter implements Instructions {

	private AsmTokeniser tokeniser = new AsmTokeniser();
	private AsmToken token;
	private boolean result;

	private int bytecodeOffset = 0;
	
	private HashMap<String, Integer> labels;
	private HashMap<String, List<Integer>> missedLabels;
	
	private TreeMap<Integer, Integer> lineIndexMapping;
	
	private class ExternInfo {
		public String name;
		public int srcId;
	}
	
	private ArrayList<ExternInfo> externInputs;
	private ArrayList<ExternInfo> externOutputs;
	private ArrayList<ExternInfo> externFlags;
	
	private class VarInfo {
		public int id;
		public ClassInfo classInfo = null;
		public String importFile = null;
	}
	
	private HashMap<String, VarInfo> vars;
	private int nextVarId;
	
	private class ClassInfo {
		public String name;
		public Module.Declaration declaration = null;
	}
	
	private HashMap<String, Byte> aliasInfo;
	private String aliasPackage;
	private ArrayList<ClassInfo> classes;
	
	public void reset() {
		super.reset();
		result = true;
		labels = new HashMap<>();
		missedLabels = new HashMap<>();
		vars = new HashMap<>();
		nextVarId = 0;
		aliasInfo = new HashMap<>();
		classes = new ArrayList<>();
		externInputs = new ArrayList<>();
		externOutputs = new ArrayList<>();
		externFlags = new ArrayList<>();
		aliasPackage = null;
		lineIndexMapping = new TreeMap<>();
	}
	
	private void error(String msg, boolean skipToNewline) {
		System.err.println("Parser error on line "+tokeniser.getLineIndex()+": "+msg);
		result = false;
		
		if(skipToNewline)
			while(token!=null && token.type!=AsmToken.NEWLINE) {
				try {
					token = tokeniser.getNextToken();
				}
				catch(AsmTokeniser.UnknownTokenException e) {
				}
			}
	}
	
	private void error(String msg) {
		error(msg, true);
	}
	
	private void next() {
		try {
			token = tokeniser.getNextToken();
		}
		catch(AsmTokeniser.UnknownTokenException e) {
			error("unknown symbol "+tokeniser.getIndex());
			next();
		}
	}
	
	private void regLineIndex() {
		lineIndexMapping.put(address(), tokeniser.getLineIndex());
	}
	
	private BytecodeWriter makeAliasTable() {
		BytecodeWriter b = new BytecodeWriter();
		b.write(ALIASES_START);
		for(int i=0; i<classes.size(); i++) {
			b.write((byte) i);
			b.writeString(classes.get(i).name);
		}
		b.write(ALIASES_END);
		bytecodeOffset = b.address() - address();
		return b;
	}
	
	private void makeExternTable(BytecodeWriter b, byte type, List<ExternInfo> info) {
		if(info.size()>0) {
			b.write(type);
			b.write((byte) info.size());
			for(ExternInfo i : info) {
				b.writeString(i.name);
				b.writeInt(i.srcId);
			}
		}
	}
	
	private BytecodeWriter makeExternTable() {
		BytecodeWriter b = new BytecodeWriter();
		makeExternTable(b, EXTERN_IN, externInputs);
		makeExternTable(b, EXTERN_OUT, externOutputs);
		makeExternTable(b, EXTERN_FLAG, externFlags);
		return b;
	}
	
	private VarInfo getVar(String name) {
		VarInfo var = vars.get(name);
		if(var==null) {
			error("unknown var", false);
			return null;
		}
		return var;
	}
	
	private String fullName(String c) {
		if(c.startsWith(".")) {
			if(aliasPackage==null) {
				error("alias package is not specified, use #aliaspk", false);
			}
			else
			c = aliasPackage+c;
		}
		return c;
	}
	
	private byte addClass(String c) {
		int id = -1;
		for(int i=0; i<classes.size(); i++) {
			if(classes.get(i).name.equals(c)) {
				id = i;
				break;
			}
		}
		if(id<0) {
			id = classes.size();
			ClassInfo info = new ClassInfo();
			info.name = c;
			try {
				Class<?> cls = ClassLoader.getSystemClassLoader().loadClass(c);
				Method getDeclaration = cls.getMethod("getDeclaration");
				info.declaration = (Module.Declaration) getDeclaration.invoke(null);
			} catch (Exception e) {
				error("module class cannot be accessed", false);
			}
			classes.add(info);
		}
		return (byte) id;
	}
	
	private int findPort(String[] names, String p, String type) {
		if(names!=null) {
			for(int i=0; i<names.length; i++)
				if(names[i].equals(p))
					return i;
		}
		error("unknown "+type+" name "+p, false);
		return 0;
	}
	
	// some super ugly functions below
	private int getSrcId(String id) {
		String[] s = id.split("\\.");
		if(s.length>2 || s[0].isEmpty()) {
			error("unexpected . (dot)", false);
			return 0;
		}
		VarInfo var = getVar(s[0]);
		if(var==null) return 0;
		int n = var.id;
		if(s.length>1 && !s[1].isEmpty()) {
			n += findPort(var.classInfo.declaration.outputNames, s[1], "output");
		}
		return n;
	}
	
	private int getDestId(String id) {
		String[] s = id.split("\\.");
		if(s.length>2 || s[0].isEmpty()) {
			error("unexpected . (dot)", false);
			return 0;
		}
		VarInfo var = getVar(s[0]);
		if(var==null) return 0;
		int n = var.id;
		if(s.length>1 && !s[1].isEmpty()) {
			n += findPort(var.classInfo.declaration.inputNames, s[1], "input");
		}
		return n;
	}
	
	private int getFlagId(String src, String flag) {
		String[] s = src.split("\\.");
		if(s.length>2 || s[0].isEmpty()) {
			error("unexpected . (dot)", false);
			return 0;
		}
		VarInfo var = getVar(s[0]);
		if(var==null) return 0;
		int n = var.id;
		n += findPort(var.classInfo.declaration.flagNames, flag, "flag");
		return n;
	}
	
	private int getFlagId(String id) {
		String[] s = id.split("\\.");
		if(s.length>2 || s[0].isEmpty()) {
			error("unexpected . (dot)", false);
			return 0;
		}
		VarInfo var = getVar(s[0]);
		if(var==null) return 0;
		int n = var.id;
		if(s.length>1 && !s[1].isEmpty()) {
			n += findPort(var.classInfo.declaration.flagNames, s[1], "flag");
		}
		return n;
	}
	
	private int getConfigId(String dst, String cfg) {
		String[] s = dst.split("\\.");
		if(s.length>2 || s[0].isEmpty()) {
			error("unexpected . (dot)", false);
			return 0;
		}
		VarInfo var = getVar(s[0]);
		if(var==null) return 0;
		int n = var.id;
		n = findPort(var.classInfo.declaration.configNames, cfg, "config");
		return n;
	}

	private int getLabel(String s) {
		Integer n = labels.get(s);
		if(n==null) {
			List<Integer> misses = missedLabels.get(s);
			if(misses==null) {
				misses = new LinkedList<>();
				missedLabels.put(s, misses);
			}
			misses.add(address());
			return 0;
		}
		else
			return n-address()-4;
	}
	
	private void setLabel(String s) {
		if(labels.get(s)!=null) {
			error("duplicate label @"+s, false);
			return;
		}
		int addr = address();
		labels.put(s, addr);
		List<Integer> misses = missedLabels.get(s);
		if(misses!=null) {
			for(int n : misses) {
				writeIntAt(n, addr-n-4);
			}
			missedLabels.remove(s);
		}
	}
	
	private VarInfo newVar(String name, int id, String fileName) {
		ClassInfo info = (id>=0) ? classes.get(id) : null;
		VarInfo var = vars.get(name);
		if(var==null) {
			var = new VarInfo();
			var.id = nextVarId | 0x80000000;
			if(info==null) {
				info = new ClassInfo();
				info.declaration = ExternDeclaration.read(fileName);
			}
			var.classInfo = info;
			var.importFile = fileName;
			nextVarId += 0x100;
			vars.put(name, var);
		}
		else if(var.classInfo!=null) {
			if(!var.classInfo.name.equals(info.name)) {
				error("var re-assignment must retain module class");
				return null;
			}
		}
		else if(var.importFile!=null) {
			if(!var.importFile.equals(fileName)) {
				error("var re-import must retain source file link");
				return null;
			}
		}
		return var;
	}
	
	private void addExtern(List<ExternInfo> info) {
		next();
		if(token.type==AsmToken.IDENTIFIER) {
			String name = (String) token.value;
			next();
			if(token.type==AsmToken.IDENTIFIER) {
				ExternInfo ex = new ExternInfo();
				ex.name = name;
				ex.srcId = getSrcId((String) token.value);
				info.add(ex);
				next();
			}
			else {
				error("src name expected");
				return;
			}		
		}
		else {
			error("extern name expected");
			return;
		}		
	}
	
	private void directive(String s) {
		if(s.equals("print")) {
			for(;;) {
				next();
				if(token.type==AsmToken.NEWLINE)
					break;
				else if(token.type==AsmToken.STRING) {
					regLineIndex();
					write(D_PRINT_STR);
					writeString((String) token.value);
				}
				else if(token.type==AsmToken.IDENTIFIER) {
					regLineIndex();
					write(D_PRINT);
					writeInt(getSrcId((String) token.value));
				}
			}
			regLineIndex();
			write(D_PRINT_LN);
		}
		
		else if(s.equals("init")) {
			next();
			if(token.type==AsmToken.IDENTIFIER) {
				int srcId = getSrcId((String) token.value);
				next();
				if(token.type==AsmToken.INTEGER) {
					regLineIndex();
					write(D_INIT_INT);
					writeInt(srcId);
					writeInt((Integer) token.value);
					next();
				}
				else {
					error("number constant expected");
					return;
				}
			}
			else {
				error("port identifier expected");
				return;
			}
		}
		
		else if(s.equals("param")) {
			next();
			if(token.type==AsmToken.INTEGER) {
				int arg = (Integer) token.value;
				next();
				if(token.type==AsmToken.IDENTIFIER) {
					int srcId = getSrcId((String) token.value);
					next();
					if(token.type==AsmToken.INTEGER) {
						regLineIndex();
						write(D_PARAM_INT);
						writeInt(arg);
						writeInt(srcId);
						writeInt((Integer) token.value);
						next();
					}
					else {
						error("number constant expected");
						return;
					}
				}
				else {
					error("port identifier expected");
					return;
				}
			}
			else {
				error("argument index (number) expected");
				return;
			}
		}
		
		else if(s.equals("jump")) {
			next();
			if(token.type==AsmToken.LABEL) {
				regLineIndex();
				write(I_JUMP);
				writeInt(getLabel((String) token.value));
				next();
			}
			else {
				error("expected label (start with @)");
			}
		}
		
		else if(s.equals("estim")) {
			next();
			if(token.type==AsmToken.STRING) {
				String cls = fullName((String) token.value);
				regLineIndex();
				write(D_ESTIM);
				writeString(cls);
				next();
			}
			else {
				error("estimation class expected");
				return;
			}
		}
		
		else if(s.equals("estprint")) {
			regLineIndex();
			write(D_EST_PRINT);
			next();	
		}

		else if(s.equals("syncall")) {
			regLineIndex();
			write(D_SYNC_ALL);
			next();	
		}

		else if(s.equals("nop")) {
			regLineIndex();
			write(I_NOP);
			next();	
		}
		
		else if(s.equals("assign")) {
			next();
			if(token.type==AsmToken.IDENTIFIER) {
				String name = (String) token.value;
				if(name.indexOf('.')>=0) {
					error("bad var name "+name);
					return;
				}
				next();
				Byte id;
				if(token.type==AsmToken.IDENTIFIER) {
					id = aliasInfo.get((String) token.value);
					if(id==null) {
						error("unknown alias");
						return;
					}
				}
				else if(token.type==AsmToken.STRING) {
					id = addClass(fullName((String) token.value));
				}
				else {
					error("alias name or module class expected");
					return;
				}
				
				VarInfo var = newVar(name, id, null);
				regLineIndex();
				write(I_ASSIGN);
				write(id);
				writeInt(var.id);
				next();
			}
			else {
				error("var name expected");
				return;
			}
		}
		
		else if(s.equals("free")) {
			next();
			if(token.type==AsmToken.IDENTIFIER) {
				regLineIndex();
				write(I_FREE);
				writeInt(getVar((String) token.value).id);
				next();
			}
			else {
				error("var name expected");
				return;
			}
		}
		
		else if(s.equals("archon")) {
			next();
			if(token.type==AsmToken.IDENTIFIER) {
				String name = (String) token.value;
				if(name.indexOf('.')>=0) {
					error("bad var name "+name);
					return;
				}
				next();
				String fileName;
				if(token.type==AsmToken.STRING) {
					fileName = (String) token.value;
				}
				else {
					error("file name expected");
					return;
				}
				next();
				boolean silent = false;
				boolean step = false;
				boolean dep = false;
				while(token.type==AsmToken.DIRECTIVE) {
					String opt = (String) token.value; 
					if(opt.equals("silent")) {
						silent = true;
					}
					else if(opt.equals("step")) {
						step = true;
					}
					else if(opt.equals("dep")) {
						dep = true;
					}
					else {
						error("unknown option");
						return;
					}
					next();
				}
				
				VarInfo var = newVar(name, -1, fileName);
				regLineIndex();
				write(I_SIM);
				writeString(fileName);
				writeInt(var.id);
				write(I_SIM_NAME);
				writeInt(var.id);
				writeString(name);
				if(silent) {
					write(I_SIM_SILENT);
					writeInt(var.id);
				}
				if(step) {
					write(I_SIM_STEP);
					writeInt(var.id);
				}
				if(dep) {
					write(I_SIM_DEP);
					writeInt(var.id);
				}
				write(I_SIM_END_INIT);
				writeInt(var.id);
			}
			else {
				error("var name expected");
				return;
			}
		}
		
		else if(s.equals("aliaspk")) {
			next();
			if(token.type==AsmToken.STRING) {
				aliasPackage = (String) token.value;
				next();
			}
			else {
				error("package name (string) expected");
				return;
			}
		}
		
		else if(s.equals("alias")) {
			next();
			if(token.type==AsmToken.IDENTIFIER) {
				String alias = (String) token.value;
				if(alias.indexOf('.')>=0) {
					error("bad alias name "+alias);
				}
				next();
				if(token.type==AsmToken.STRING) {
					String c = fullName((String) token.value);
					Byte id = addClass(c);
					aliasInfo.put(alias, id);
					next();
				}
				else {
					error("module class (string) expected");
					return;
				}
			}
			else {
				error("alias name expected");
				return;
			}
		}
		
		else if(s.equals("extern_in")) {
			addExtern(externInputs);
			return;
		}
		else if(s.equals("extern_out")) {
			addExtern(externOutputs);
			return;
		}
		else if(s.equals("extern_flags")) {
			addExtern(externFlags);
			return;
		}
		
		else {
			error("unknown directive #"+s);
		}
	}
	
	private void command(String s) {
		if(s.isEmpty() || s.equals("go")) {
			regLineIndex();
			write(I_NEXT);
		}
		else if(s.equals("stop")) {
			regLineIndex();
			write(I_STOP);
		}
		else if(s.equals("ack")) {
			regLineIndex();
			write(I_ACK);
		}
		else if(s.equals("begin")) {
			regLineIndex();
			write(I_RESUME);
		}
		else if(s.equals("jump")) {
			next();
			if(token.type==AsmToken.LABEL) {
				regLineIndex();
				write(I_JUMP);
				writeInt(getLabel((String) token.value));
			}
			else {
				error("expected label (start with @)");
			}
		}
		else if(s.equals("x")) {
			regLineIndex();
			write(I_UNLINK_ALL);
		}
		else if(s.equals("gox")) {
			regLineIndex();
			write(I_NEXT);
			regLineIndex();
			write(I_UNLINK_ALL);
		}
		else {
			try {
				int n = Integer.parseInt(s);
				if(n<1 || n>127) {
					error("invalid number of iterations");
					return;
				}
				else {
					regLineIndex();
					write(I_NEXT_COUNT);
					write((byte) n);
				}
			}
			catch(NumberFormatException e) {
				error("unknown command !"+s);
				return;
			}
		}
		next();
	}
	
	private void condition() {
		next();
		byte code = C_FLAG;
		if(token.is("^")) {
			code = C_NFLAG;
			next();
		}
		if(token.type==AsmToken.IDENTIFIER) {
			regLineIndex();
			write(code);
			writeInt(getFlagId((String) token.value));
		}
		else {
			error("flag identifier expected");
			return;
		}
		next();
		if(!token.is("]")) {
			error("] expected");
			return;
		}
		next();
	}
	
	private void connect(int destId) {
		next();
		String flag = null;
		byte code = I_LINK;
		if(token.is("[")) {
			next();
			code = I_LINK_FLAG;
			if(token.is("^")) {
				code = I_LINK_NFLAG;
				next();
			}
			if(token.type==AsmToken.IDENTIFIER) {
				flag = (String) token.value;
			}
			else {
				error("flag identifier expected");
				return;
			}
			next();
			if(!token.is("]")) {
				error("] expected");
				return;
			}
			next();
		}
		if(token.type==AsmToken.IDENTIFIER) {
			int srcId = getSrcId((String) token.value);
			regLineIndex();
			write(code);
			writeInt(destId);
			writeInt(srcId);
			if(flag!=null) {
				write((byte)(getFlagId((String) token.value, flag) & 0xff));
			}
			next();
		}
		else {
			error("output port identifier expected");
			return;
		}

	}
	
	private void configure(int destId, String destName) {
		next();
		regLineIndex();
		write(I_CONFIG);
		writeInt(destId);
		if(token.type==AsmToken.INTEGER) {
			writeInt((Integer) token.value);
		}
		else if(token.type==AsmToken.IDENTIFIER) {
			writeInt(getConfigId(destName, (String) token.value));
		}
		else {
			error("config number or identifier expected");
			return;
		}
		next();
		if(!token.is(")")) {
			error(") expected");
			return;
		}
		next();
	}
	
	private boolean parse() {
		for(;;) {
			next();
			if(token==null) // end of input
				return result;
			
			if(token.type==AsmToken.LABEL) {
				setLabel((String) token.value);
				next();
			}
			
			if(token.is("[")) {
				condition();
			}
			
			switch(token.type) {
				case AsmToken.NEWLINE:
					continue;
				case AsmToken.COMMAND:
					command((String) token.value);
					break;
				case AsmToken.DIRECTIVE:
					directive((String) token.value);
					break;
				case AsmToken.IDENTIFIER:
					String destName = (String) token.value;
					int destId = getDestId(destName);
					next();
					if(token.is("=")) {
						connect(destId);
					}
					else if(token.is("=X")) {
						regLineIndex();
						write(I_UNLINK);
						writeInt(destId);
						next();
					}
					else if(token.is("(")) {
						configure(destId, destName); // FIXME resId instead of destId
					}
					else {
						error("syntax error");
					}
					break;
				default:
					error("syntax error");
			}
			if(token.type!=AsmToken.NEWLINE) {
				error("newline expected");
			}
		}
	}
	
	public boolean parse(String s) {
		reset();
		tokeniser.start(s);
		return parse();
	}
	
	public boolean parse(File f) throws IOException {
		reset();
		tokeniser.start(f);
		System.out.println("Parsing file "+f.getName());
		return parse();
	}
	
	public File compile(File f, boolean writeLineMapping) {
		try {
			if(parse(f)) {
				File fOut = new File(f.getAbsolutePath()+".bin");
				writeBytecode(new File(f.getAbsolutePath()+".bin"));
				if(writeLineMapping)
					writeLineMapping(new File(f.getAbsolutePath()+".map"));
				return fOut;
			}
			else {
				return null;
			}
		}
		catch(IOException e) {
			System.err.println("Parser error: can't read file "+f.getName());
			return null;
		}
	}
	
	public static File compileFile(String path, boolean writeLineMapping) {
		File f = new File(path);
		ProgramParserBytecode p = new ProgramParserBytecode();
		f = p.compile(f, writeLineMapping);
		if(f==null) {
			System.err.println("Compiled with errors.\n\n");
			System.exit(1);
		}
		
		System.out.println("Compiled successfully.\n\n");
		return f;
	}
	
	public byte[] getBytecode() {
		return makeExternTable().append(makeAliasTable()).append(this).getBytecode();
	}
	
	public void writeBytecode(File f) {
		try {
			FileOutputStream out = new FileOutputStream(f);
			byte[] buffer = getBytecode();
			out.write(buffer);
			out.close();
		} catch (IOException e) {
			System.err.println("Parser error: can't write output file "+f.getName());
		}
	}
	
	public void writeLineMapping(File f) {
		try {
			PrintWriter out = new PrintWriter(f);
			out.printf("(Offs=%d)\n\t%8s:\t%5s\n", bytecodeOffset, "Code", "Src");
			for(Entry<Integer, Integer> e : lineIndexMapping.entrySet()) {
				out.printf("\t%8d:\t%5d\n", e.getKey()+bytecodeOffset, e.getValue());
			}
			out.close();
		} catch (IOException e) {
			System.err.println("Parser error: can't write mapping file "+f.getName());
		}
	}
	
	public Map<Integer, Integer> getLineMapping() {
		TreeMap<Integer, Integer> indexes = new TreeMap<>();
		for(Entry<Integer, Integer> e : lineIndexMapping.entrySet()) {
			indexes.put(e.getKey()+bytecodeOffset, e.getValue());
		}
		return indexes;
	}

	
	public static void main(String[] args) {
		ProgramParserBytecode p = new ProgramParserBytecode();
		if(p.compile(new File("sample.sim"), true)!=null)
			System.out.println("Done");
		else
			System.out.println("Done with errors");
		p.dumpBytecode();
	}
	
}
