package ncl.cs.prime.archon.arch;

public class ArchitectureException extends RuntimeException {

	public static final String CONNECTION_DENIED = "Module connection denied by the architecture";
	public static final String MODULE_INSTANTIATION_ERROR = "Module class cannot be instantiated";
	
	public ArchitectureException(String message) {
		super(message);
	}
	
}
