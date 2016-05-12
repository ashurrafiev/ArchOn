package ncl.cs.prime.archon.genetic.smart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ncl.cs.prime.archon.bytecode.InstructionPointer;
import ncl.cs.prime.archon.bytecode.Instructions;

public class SmartGene implements Instructions {

	public static enum ParamType {
		INT_ADDR(4),
		INT_VAR(4),
		INT_SRC(4),
		INT_DEST(4),
		INT_NUMBER(4),
		INT_FLAG(4),
		INT_CONFIG_ONLY(4),
		BYTE_FLAG_ONLY(1),
		BYTE_ALIAS(1),
		BYTE_NUMBER(1),
		REF_ADDR(4);
		
		private int length;
		
		private ParamType(int length) {
			this.length = length;
		}
		
		public int length() {
			return length;
		}
	}
	
	public int index = 0;
	public int address = 0;
	private SmartGene sourceGene = null;
	
	private byte cmd;
	private ArrayList<ParamType> paramTypes = new ArrayList<>();
	private ArrayList<Object> paramValues = new ArrayList<>(); 
	private boolean hasAddressParam = false;
	
	private SmartGene(byte cmd) {
		this.cmd = cmd;
	}
	
	private void addParam(ParamType type) {
		paramTypes.add(type);
		if(type==ParamType.INT_ADDR)
			hasAddressParam = true;
	}

	private void addParamValue(Object value) {
		paramValues.add(value);
	}
	
	public void cleanupSources() {
		sourceGene = null;
	}
	
	public void updateAddressRefs(int addr, HashMap<Integer, SmartGene> addressMap) {
		if(!hasAddressParam)
			return;
		for(int i=0; i<paramTypes.size(); i++) {
			if(paramTypes.get(i)==ParamType.INT_ADDR) {
				SmartGene ref = addressMap.get(addr+(Integer) paramValues.get(i));
				if(ref!=null) {
					paramTypes.set(i, ParamType.REF_ADDR);
					paramValues.set(i, ref);
				}
			}	
		}
	}
	
	public void updateAddressRefs(ArrayList<SmartGene> genes) {
		if(!hasAddressParam)
			return;
		for(int i=0; i<paramTypes.size(); i++) {
			if(paramTypes.get(i)==ParamType.REF_ADDR) {
				SmartGene oldRef = (SmartGene) paramValues.get(i);
				SmartGene ref = null;
				SmartGene indexRef = null;
				int index = 0;
				for(SmartGene g : genes) {
					if(oldRef==g.sourceGene) {
						ref = g;
						break;
					}
					if(index==oldRef.index)
						indexRef = g;
					index++;
				}
				if(ref!=null)
					paramValues.set(i, ref);
				else if(indexRef!=null)
					paramValues.set(i, indexRef);
				else
					paramValues.set(i, this);
			}
		}
	}

	public int length() {
		int s = 1;
		for(ParamType t : paramTypes)
			s += t.length();
		return s;
	}
	
	public void mutate() {
		// TODO mutate gene
	}
	
	public SmartGene copy() {
		SmartGene g = new SmartGene(cmd);
		g.sourceGene = this;
		g.hasAddressParam = hasAddressParam;
		g.paramTypes.addAll(paramTypes);
		g.paramValues.addAll(paramValues);
		return g;
	}
	
	private static void write(ArrayList<Byte> bytecode, byte bt) {
		bytecode.add(bt);
	}
	
	private static void writeInt(ArrayList<Byte> bytecode, int x) {
		write(bytecode, (byte)(x & 0xff));
		write(bytecode, (byte)((x>>8) & 0xff));
		write(bytecode, (byte)((x>>16) & 0xff));
		write(bytecode, (byte)((x>>24) & 0xff));
	}
	
	public static void encode(ArrayList<Byte> bytecode, List<SmartGene> genes) {
		for(SmartGene g : genes) {
			write(bytecode, g.cmd);
			for(int i=0; i<g.paramTypes.size(); i++) {
				if(g.paramTypes.get(i)==ParamType.REF_ADDR)
					writeInt(bytecode, ((SmartGene) g.paramValues.get(i)).address - (g.address+g.length()));
				else if(g.paramTypes.get(i).length()==4)
					writeInt(bytecode, (Integer) g.paramValues.get(i));
				else
					write(bytecode, (Byte) g.paramValues.get(i));
			}
		}
	}
	
	public static SmartGene forCommand(byte cmd) {
		SmartGene g = new SmartGene(cmd);
		switch(cmd) {
			case D_ESTIM:
			case D_EST_PRINT:
			case D_PRINT:
			case D_PRINT_STR:
			case D_PRINT_LN:
				return null;
			case D_INIT_INT:
				g.addParam(ParamType.INT_DEST);
				g.addParam(ParamType.INT_NUMBER);
				break;
			case D_PARAM_INT:
				g.addParam(ParamType.INT_NUMBER);
				g.addParam(ParamType.INT_DEST);
				g.addParam(ParamType.INT_NUMBER);
				break;
			case I_UNLINK:
				g.addParam(ParamType.INT_DEST);
				break;
			case I_UNLINK_ALL:
				break;
			case I_LINK:
				g.addParam(ParamType.INT_DEST);
				g.addParam(ParamType.INT_SRC);
				break;
			case I_LINK_FLAG:
				g.addParam(ParamType.INT_DEST);
				g.addParam(ParamType.INT_SRC);
				g.addParam(ParamType.BYTE_FLAG_ONLY);
				break;
			case I_LINK_NFLAG:
				g.addParam(ParamType.INT_DEST);
				g.addParam(ParamType.INT_SRC);
				g.addParam(ParamType.BYTE_FLAG_ONLY);
				break;	
			case I_CONFIG:
				g.addParam(ParamType.INT_DEST);
				g.addParam(ParamType.INT_CONFIG_ONLY);
				break;
			case I_JUMP:
				g.addParam(ParamType.INT_ADDR);
				break;
			case I_NEXT:
				break;
			case I_NEXT_COUNT:
				g.addParam(ParamType.BYTE_NUMBER);
				break;
			case I_STOP:
				break;
			case C_FLAG:
				g.addParam(ParamType.INT_FLAG);
				break;
			case C_NFLAG:
				g.addParam(ParamType.INT_FLAG);
				break;
			case I_ASSIGN:
				g.addParam(ParamType.BYTE_ALIAS);
				g.addParam(ParamType.INT_VAR);
				break;
			case I_FREE:
				g.addParam(ParamType.INT_VAR);
				break;
			
			case I_NOP:
				break;
			default:
				g = new SmartGene(I_NOP);
				break;
		}
		
		return g;
	}
	
	public static SmartGene decode(InstructionPointer ip) {
		byte cmd = ip.next();
		if(cmd==ALIASES_START) {
			ip.skipAliases();
			return null;
		}
		SmartGene g = forCommand(cmd);
		if(g==null)
			return null;
		for(int i=0; i<g.paramTypes.size(); i++) {
			if(g.paramTypes.get(i).length()==4)
				g.addParamValue(ip.nextInt());
			else
				g.addParamValue(ip.next());
		}
		return g;
	}
	
}
