package project;

import java.io.ByteArrayOutputStream;

public enum Type {
	RRQ(1),
	WRQ(2),
	DATA(3),
	ACK(4),
	ERROR(5);
	
	private int OPCODE;
	
	private Type(final int OPCODE) {
		this.OPCODE = OPCODE;
	}
	
	/**
	 * Convert the OPCODE into byte array of length 2
	 * 
	 * @return byte array which contains the OPCODE
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
}
