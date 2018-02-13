package project;

/**
 * This is the enum class that defines all types of error that
 * may happen during TFTP file transfer process.
 * For project iteration 2, all the possible errors are
 * 	- file not found(1)
 * 	- access violation(2)
 *  - disk full(3)
 *  - file already exists(6)
 * All the other error types will be used in future iterations. 
 * 
 * @author yunkai wang
 * Last modified on Feb 11th, 2018
 *
 */
public enum TFTPErrorType {
	NOT_DEFINED(0, "Unknown error"),
	FILE_NOT_FOUND(1, "File not found"),
	ACCESS_VIOLATION(2, "Access violation"),
	DISK_FULL(3, "Disk full"),
	ILLEGAL_TFTP_OPERATION(4, "Illegal TFTP operation"),
	UNKNOWN_TRANSFER_ID(5, "Unknown transfer ID"),
	FILE_ALREADY_EXISTS(6, "File already exists"),
	NO_SUCH_USER(7, "No such user");
	
	private int errorCode; // error code following the defined TFTP standard
	private String defaultErrorMsg; // default error message
	
	/**
	 * Constructor
	 * 
	 * @param errorCode
	 * @param errorMsg
	 */
	private TFTPErrorType(int errorCode, String errorMsg) {
		this.errorCode = errorCode;
		this.defaultErrorMsg = errorMsg;
	}
	
	/**
	 * Getter
	 * 
	 * @return errorCode
	 */
	public int getErrorCode() {
		return this.errorCode;
	}
	
	/**
	 * Getter
	 * 
	 * @return defaultErrorMsg
	 */
	public String defaultErrorMsg() {
		return defaultErrorMsg;
	}
	
	/**
	 * Return TFTP error type based on error code
	 * 
	 * @param errorCode
	 * @return TFTPErrorType
	 */
	public static TFTPErrorType getErrorType(int errorCode) {
		for (TFTPErrorType t: TFTPErrorType.values()) {
			if (t.getErrorCode() == errorCode)
				return t;
		}
		throw new IllegalArgumentException("Invalid error code.");
	}
}
