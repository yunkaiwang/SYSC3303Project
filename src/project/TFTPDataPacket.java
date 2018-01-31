package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TFTPDataPacket {
	private static final Type type = Type.DATA;
	private static final int MAX_LENGTH = 516;
	private static final int HEADER_LENGTH = 4;
	private static final int MAX_DATA_LENGTH = 512;
	private static final int MIN_BLOCK_NUMBER = 0;
	private static final int MAX_BLOCK_NUMBER = 0xffff;
	private int blockNumber;
	private byte[] fileData;
	
	TFTPDataPacket(int blockNumber) {
		this(blockNumber, null, 0);
	}
	
	TFTPDataPacket(int blockNumber, byte[] fileData) {
		this(blockNumber, fileData, fileData == null ? 0 : fileData.length);
	}
	
	TFTPDataPacket(int blockNumber, byte[] fileData, int fileDataLength) {
		if (!validBlockNumber(blockNumber))
			throw new IllegalArgumentException("Invalid block number");
		
		if (!this.validFileData(fileData, fileDataLength))
			throw new IllegalArgumentException("Invalid file data");
		
		this.blockNumber = blockNumber;
		if (fileData == null) {
			this.fileData = new byte[0];
		} else {
			this.fileData = new byte[fileDataLength];
			System.arraycopy(fileData, 0, this.fileData, 0, fileDataLength);
		}
	}
	
	public static TFTPDataPacket createFromPacketData(byte[] packetData, int packetDataLength) {
		if (!validPacketData(packetData, packetDataLength))
			throw new IllegalArgumentException("Invalid packet data");
		int OPCODE = ByteBuffer.wrap(packetData, 0, 2).getInt();
		if (!Type.validOPCODE(type, OPCODE))
			throw new IllegalArgumentException("Invalid OP code");
		int BlockNumber = ByteBuffer.wrap(packetData, 2, 2).getInt();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(packetData, HEADER_LENGTH, packetDataLength - HEADER_LENGTH);
		packetData = stream.toByteArray();
		return new TFTPDataPacket(BlockNumber, packetData, packetData.length);
	}
	
	private boolean validFileData(byte[] fileData, int fileDataLength) {
		return ((fileData == null && fileDataLength == 0) ||
				fileData != null && fileData.length == fileDataLength &&
				fileDataLength <= MAX_DATA_LENGTH);
	}
	
	private boolean validBlockNumber(int blockNumber) {
		return (blockNumber >= MIN_BLOCK_NUMBER && blockNumber <= MAX_BLOCK_NUMBER);
	}
	
	private static boolean validPacketData(byte[] packetData, int packetDataLength) {
		return (packetData != null && packetData.length == packetDataLength &&
				packetDataLength <= MAX_LENGTH && packetDataLength >= HEADER_LENGTH);
	}
	
	public int getBlockNumbe() {
		return this.blockNumber;
	}
	
	public byte[] getFileData() {
		return this.fileData;
	}
	
	public boolean isLastDataPacket() {
		return this.fileData.length < MAX_BLOCK_NUMBER;
	}
	
	private byte[] blockNumber() {
		return ByteBuffer.allocate(2).putInt(blockNumber).array();
	}
	
	public byte[] generateData() throws IOException {
		ByteArrayOutputStream steam = new ByteArrayOutputStream();
		steam.write(type.OPCODE());
		steam.write(blockNumber());
		steam.write(fileData, 0, fileData.length);
		return steam.toByteArray();
	}
	
}
