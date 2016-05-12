package ncl.cs.prime.archon.bytecode;

public interface Instructions {

	public static final byte D_PRINT = -1; // i:dest
	public static final byte D_PRINT_STR = -2; // s:string
	public static final byte D_PRINT_LN = -3; //
	public static final byte D_INIT_INT = -4; // i:dest i:value
	public static final byte D_PARAM_INT = -5; // i:arg i:dest i:defvalue 

	public static final byte D_ESTIM = -8; // s:class
	public static final byte D_EST_PRINT = -9; //
	
	public static final byte I_UNLINK = 0; // i:dest
	public static final byte I_LINK = 1; // i:dest i:src
	public static final byte I_LINK_FLAG = 2; // i:dest i:src b:(src.)flag
	public static final byte I_LINK_NFLAG = 3; // i:dest i:src b:(src.)flag

	public static final byte I_UNLINK_ALL = 8; //

	public static final byte I_CONFIG = 16; // i:dest i:cfg
	public static final byte I_JUMP = 17; // i:+addr
	
	public static final byte I_NOP = 24; //

	public static final byte I_NEXT = 32; //
	public static final byte I_NEXT_COUNT = 33; // b:count
	public static final byte I_STOP = 34; //
	public static final byte I_RESUME = 35; //
	public static final byte I_ACK = 36; //

	public static final byte C_FLAG = 48; // i:dest.flag
	public static final byte C_NFLAG = 49; // i:dest.flag

	public static final byte I_ASSIGN = 64; // b:class i:var
	public static final byte I_FREE = 65; // i:var
	
	public static final byte I_SIM = 66; // s:file i:sim
	public static final byte I_SIM_NAME = 67; // i:sim s:string
	public static final byte I_SIM_SILENT = 68; // i:sim
	public static final byte I_SIM_STEP = 69; // i:sim
	public static final byte I_SIM_DEP = 70; // i:sim
	public static final byte I_SIM_END_INIT = 71; // i:sim
	
	public static final byte I_DELEGATE = 72; // TODO i:var i:sim i:simvar

	public static final byte EXTERN_IN = 100; // TODO b:num { s:name i:var }
	public static final byte EXTERN_OUT = 101; // TODO
	public static final byte EXTERN_FLAG = 102; // TODO

	public static final byte ALIASES_START = 112;
	public static final byte ALIASES_END = -1;
	
}
