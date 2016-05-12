package ncl.cs.prime.archon.bytecode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class InstructionPointer implements Instructions {

	private int ip; 
	private byte[] code;
	
	public void setCode(byte[] code) {
		this.code = code;
		ip = 0;
	}
	
	public byte[] copyCode() {
		return Arrays.copyOf(code, code.length);
	}
	
	public void loadCode(File f) throws IOException {
		FileInputStream in = new FileInputStream(f);
		code = new byte[in.available()];
		in.read(code);
		in.close();
		ip = 0;
	}
	
	public byte[] getHeader() {
		int size = getHeaderSize();
		return Arrays.copyOf(code, size);
	}
	
	public int getHeaderSize() {
		reset();
		if(next()!=ALIASES_START)
			return 0;
		for(int i=0; ; i++) {
			if((int) next()!=i) {
				int addr = getAddress();
				reset();
				return addr;
			}
			nextString();
		}
	}
	
	public void skipAliases() {
		for(int i=0; ; i++) {
			if((int) next()!=i) {
				return;
			}
			nextString();
		}
	}
	
	public boolean outOfRange() {
		return ip<0 || ip>=code.length;
	}
	
	public int getAddress() {
		return ip;
	}
	
	public void reset() {
		ip = 0;
	}
	
	public void jump(int rel) {
		ip += rel;
	}
	
	public void jumpAbs(int addr) {
		ip = addr;
	}
	
	public byte next() {
		return code[ip++];
	}
	
	public void back() {
		ip--;
	}
	
	public byte codeAt(int addr) {
		return code[addr];
	}
	
	public int nextInt() {
		int n = ((int) code[ip+3]) & 0xff; // for negative byte values we need to mask with 0xff after casting to int
		n = (n<<8) | ((int) code[ip+2]) & 0xff;
		n = (n<<8) | ((int) code[ip+1]) & 0xff;
		n = (n<<8) | ((int) code[ip]) & 0xff;
		ip += 4;
		return n;
	}
	
	public String nextString() {
		String s = "";
		for(;;) {
			char ch = (char) next();
			if(ch=='\0') return s;
			s += ch;
		}
	}
	
}
