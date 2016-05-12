package ncl.cs.prime.archon.parse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ArmInstructionMapper {

	public static final int FAULT_TYPE_LINK = 1;
	public static final int FAULT_TYPE_EXEC = 2;
	public static final int FAULT_TYPE_FLOW = 4;
	
	public class GraphLine {
		public String graph;
		public int faultType = 0;
		public int lineIndex = 0; 
		
		public GraphLine(String s) {
			graph = s;
			if(s.contains("#"))
				faultType |= FAULT_TYPE_FLOW;
			else if(s.contains("!"))
				faultType |= FAULT_TYPE_EXEC;
			else if(s.contains("=") || s.contains("("))
				faultType |= FAULT_TYPE_LINK;
		}
	}
	
	public class ArmLine {
		public String arm = null;
		public ArrayList<GraphLine> graphLines = new ArrayList<>();
		public int faultType = 0;
		public int reinforceType = 0;
		public int armIndex = 0; 
		public int lineIndex = 0; 
		
		public ArmLine() {
			lines.add(this);
		}
		
		public ArmLine(String arm) {
			this.arm = arm;
			lines.add(this);
		}
		
		public void add(String s) {
			GraphLine g = new GraphLine(s);
			graphLines.add(g);
			faultType |= g.faultType;
		}
	}
	
	private ArrayList<ArmLine> lines = new ArrayList<>();
	private Map<Integer, Integer> lineMapping = null;
	public int reinforceScale = 4;
	
	public ArmInstructionMapper(File f) throws IOException {
		Scanner scan = new Scanner(f);
		ArmLine line = new ArmLine();
		String s;
		boolean comment = false;
		for(;;) {
			try {
				s = scan.nextLine();
				if(comment) {
					if(s.endsWith("*/")) {
						comment = false;
					}
				}
				else {
					if(s.matches("\\/\\/\\-+")) {
						s = scan.nextLine();
						if(!s.startsWith("// "))
							continue;
						line = new ArmLine(s.substring(3));
					}
					else if(s.startsWith("/*")) {
						comment = true;
					}
					else {
						if(s.startsWith("//"))
							continue;
						line.add(s);
					}
				}
			}
			catch(NoSuchElementException e) {
				break;
			}
		}
		scan.close();
		getGraphCode();
	}
	
	public Iterable<String> armListing() {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					private Iterator<ArmLine> i = lines.iterator();
					@Override
					public boolean hasNext() {
						return i.hasNext();
					}
					@Override
					public String next() {
						ArmLine line = i.next();
						if(line.arm==null)
							line = i.next();
						return line.arm;
					}
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
	
	public Iterable<ArmLine> lines() {
		return lines;
	}
	
	public int findArmIndexForAddr(int addr) {
		if(lineMapping==null)
			return -1;
		int i = -1;
		int index = lineMapping.get(addr);
//		return index;
		for(ArmLine line : lines) {
			if(line.lineIndex>index)
				return i;
			i = line.armIndex;
		}
		return -1;
	}
	
	public String getGraphCode() {
		StringBuilder out = new StringBuilder();
		int i = 0;
		int armIndex = 0;
		for(ArmLine line : lines) {
			if(line.arm!=null) {
				out.append("//------------------------------------------------\r\n");
				i++;
				out.append("// "+line.arm+"\r\n");
				line.lineIndex = i;
				i++;
				line.armIndex = armIndex;
				armIndex++;
			}
			for(GraphLine g : line.graphLines) {
				g.lineIndex = i;
				int rmax = 1;
				if((g.faultType & line.reinforceType)!=0)
					rmax = reinforceScale;
				for(int r=0; r<rmax; r++) {
					out.append(g.graph+"\r\n");
					i++;
				}
			}
		}
		return out.toString();
	}
	
	public byte[] compile() {
		ProgramParserBytecode p = new ProgramParserBytecode();
		if(!p.parse(getGraphCode()))
			return null;
		byte[] buffer = p.getBytecode();
		lineMapping = p.getLineMapping();
		return buffer;
	}
	
	public static ArmInstructionMapper mapFile(String path) {
		try {
			ArmInstructionMapper mapper = new ArmInstructionMapper(new File(path));
			return mapper;
		}
		catch(IOException e) {
			return null;
		}
	}
	
	public static void main(String[] args) {
		ArmInstructionMapper mapper = ArmInstructionMapper.mapFile("convo_arm.sim");
		System.out.println(mapper.getGraphCode());
		for(ArmLine line : mapper.lines()) {
			System.out.println("\t"+line.armIndex+" | \t"+line.lineIndex+" | "+line.arm);
		}
		mapper.compile();
		for(Entry<Integer, Integer> e : mapper.lineMapping.entrySet()) {
			System.out.println(e.getKey()+" > "+e.getValue());
		}
	}

}
