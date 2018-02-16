package project;

import java.io.ByteArrayOutputStream;

/**
 * Enum class that represent different TFTP packet type
 * 
 * @author yunkai wang
 *
 */
public enum Type {
	RRQ(1, "RRQ"),
	WRQ(2, "WRQ"),
	DATA(3, "DATA"),
	ACK(4, "ACK"),
	ERROR(5, "ERROR");
	
	private int OPCODE;
	private String type;
	
	/**
	 * Constructor
	 * 
	 * @param OPCODE
	 * @param type
	 */
	private Type(final int OPCODE, final String type) {
		this.OPCODE = OPCODE;
		this.type = type;
	}
	
	/**
	 * Convert the OPCODE into byte array of length 2
	 * 
	 * @return byteArray
	 */
	public byte[] OPCODE() {
		ByteArrayOutputStream steam = new ByteArrayOutputStream();
		steam.write(0);
		steam.write(OPCODE);
		return steam.toByteArray();
	}
	
	/**
	 * Check if the given OPCODE is valid by checking if the OPCODE match
	 * the OPCODE standard(1 for RRQ, 2 for WRQ, etc.)
	 * 
	 * @return true if the OPCODE matches the standard, false otherwise
	 */
	public static boolean validOPCODE(Type t, int OPCODE) {
		return OPCODE == t.OPCODE;
	}
	
	/**
	 * Getter
	 * 
	 * @return type
	 */
	public String type() {
		return type;
	}
}
