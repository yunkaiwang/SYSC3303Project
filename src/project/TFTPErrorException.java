package project;

/**
 * TFTP Error exception
 * throwed when client received TFTP error packet
 * 
 * @author yunkai wang
 * Last modified on Feb 11th, 2018
 *
 */
public class TFTPErrorException extends Exception {
	/**
	 * default serial version uid
	 */
	private static final long serialVersionUID = 1L;

	public TFTPErrorException(String message) {
		super(message);
	}
}
