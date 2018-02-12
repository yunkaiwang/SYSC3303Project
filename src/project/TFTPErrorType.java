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
	NOT_DEFINED(0),
	FILE_NOT_FOUND(1),
	ACCESS_VIOLATION(2),
	DISK_FULL(3),
	ILLEGAL_TFTP_OPERATION(4),
	UNKNOWN_TRANSFER_ID(5),
	FILE_ALREADY_EXISTS(6),
	NO_SUCH_USER(7);
	
	private int errorCode;
	
	private TFTPErrorType(int errorCode) {
		this.errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return this.errorCode;
	}
	
	public static TFTPErrorType getErrorType(int errorCode) {
		for (TFTPErrorType t: TFTPErrorType.values()) {
			if (t.getErrorCode() == errorCode)
				return t;
		}
		throw new IllegalArgumentException("Invalid error code.");
	}
}
