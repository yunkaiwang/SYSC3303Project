package project;

import java.nio.ByteBuffer;

public enum Type {
	RRQ(1),
	WRQ(2),
	ACK(3),
	DATA(4),
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
		return ByteBuffer.allocate(2).putInt(OPCODE).array();
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
