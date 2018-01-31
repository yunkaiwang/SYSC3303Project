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
	
	public byte[] OPCODE() {
		return ByteBuffer.allocate(2).putInt(OPCODE).array();
	}
	
	public static boolean validOPCODE(Type t, int OPCODE) {
		return OPCODE == t.OPCODE;
	}
}
