package ncl.cs.prime.archon.bytecode;

import java.util.ArrayList;

public class BytecodeWriter {

	private ArrayList<Byte> bytes = new ArrayList<>();

	public void reset() {
		bytes = new ArrayList<>();
	}
	
	public void write(byte bt) {
		bytes.add(bt);
	}
	
	public void writeAt(int addr, byte bt) {
		bytes.set(addr, bt);
	}
	
	public void writeInt(int x) {
		write((byte)(x & 0xff));
		write((byte)((x>>8) & 0xff));
		write((byte)((x>>16) & 0xff));
		write((byte)((x>>24) & 0xff));
	}

	public void writeIntAt(int addr, int x) {
		writeAt(addr, (byte)(x & 0xff));
		writeAt(addr+1, (byte)((x>>8) & 0xff));
		writeAt(addr+2, (byte)((x>>16) & 0xff));
		writeAt(addr+3, (byte)((x>>24) & 0xff));
	}
	
	public void writeString(String s) {
		for(int i=0; i<s.length(); i++) {
			write((byte) s.charAt(i));
		}
		write((byte) 0);

	}
	
	public int address() {
		return bytes.size();
	}
	
	public void dumpBytecode() {
		byte[] buffer = getBytecode();
		for(int i=0; i<buffer.length; i++) {
			if(i%16==0)
				System.out.printf("\n%08X ", i);
			if(i%8==0)
				System.out.printf(" ");
			System.out.printf("%02X ", ((int) buffer[i]) & 0xff);
		}
	}
	
	public byte[] getBytecode() {
		byte[] buffer = new byte[bytes.size()];
		for(int i=0; i<buffer.length; i++)
			buffer[i] = bytes.get(i);
		return buffer;
	}
	
	public BytecodeWriter append(BytecodeWriter next) {
		BytecodeWriter b = new BytecodeWriter();
		b.bytes.addAll(bytes);
		b.bytes.addAll(next.bytes);
		return b;
	}
}
