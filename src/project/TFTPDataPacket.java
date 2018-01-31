package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TFTPDataPacket {
	private static final Type type = Type.DATA; // type will always be data
	private static final int MAX_LENGTH = 516; // max packet data length
	private static final int HEADER_LENGTH = 4; // header length
	private static final int MAX_DATA_LENGTH = 512; // max data length
	private static final int MIN_BLOCK_NUMBER = 0; // minimum block number(0)
	private static final int MAX_BLOCK_NUMBER = 0xffff; // maximum block number(65535)
	private int blockNumber; // block number of the packet
	private byte[] fileData; // file data of the packet
	
	/**
	 * Constructor
	 * 
	 * @param blockNumber - the block number of the packet
	 */
	TFTPDataPacket(int blockNumber) {
		this(blockNumber, null, 0);
	}
	
	/**
	 * Constructor
	 * 
	 * @param blockNumber - the block number of the packet
	 * @param fileData    - the file data in the packet
	 */
	TFTPDataPacket(int blockNumber, byte[] fileData) {
		this(blockNumber, fileData, fileData == null ? 0 : fileData.length);
	}
	
	/**
	 * Constructor
	 * 
	 * @param blockNumber    - the block number of the packet
	 * @param fileData       - the file data in the packet
	 * @param fileDataLength - the length of the file data in the packet
	 */
	TFTPDataPacket(int blockNumber, byte[] fileData, int fileDataLength) {
		if (!validBlockNumber(blockNumber))
			throw new IllegalArgumentException("Invalid block number");
		
		if (!validFileData(fileData, fileDataLength))
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
	
	/**
	 * Check if the given fileData byte array is valid
	 * 
	 * @param fileData       - the file data
	 * @param fileDataLength - the length of the file data
	 * @return true if the file data is valid, false otherwise
	 */
	private static boolean validFileData(byte[] fileData, int fileDataLength) {
		return ((fileData == null && fileDataLength == 0) ||
				fileData != null && fileData.length == fileDataLength &&
				fileDataLength <= MAX_DATA_LENGTH);
	}
	
	/**
	 * Check if the given block number is valid
	 * 
	 * @param blockNumber - the block number
	 * @return true if the block number is valid, false otherwise
	 */
	private static boolean validBlockNumber(int blockNumber) {
		return (blockNumber >= MIN_BLOCK_NUMBER && blockNumber <= MAX_BLOCK_NUMBER);
	}
	
	
	private static boolean validPacketData(byte[] packetData, int packetDataLength) {
		return (packetData != null && packetData.length == packetDataLength &&
				packetDataLength <= MAX_LENGTH && packetDataLength >= HEADER_LENGTH);
	}
	
	/**
	 * Getter for the block number
	 * 
	 * @return block number of the packet
	 */
	public int getBlockNumbe() {
		return this.blockNumber;
	}
	
	/**
	 * Getter for the file data
	 * 
	 * @return file data of the packet
	 */
	public byte[] getFileData() {
		return this.fileData;
	}
	
	/**
	 * Check if the current packet is the last data packet
	 * 
	 * @return true if the packet is the last packet, false otherwise
	 */
	public boolean isLastDataPacket() {
		return this.fileData.length < MAX_BLOCK_NUMBER;
	}
	
	/**
	 * Convert the block number into byte array of length 2
	 * 
	 * @return byte array which contains the block number
	 */
	private byte[] blockNumber() {
		return ByteBuffer.allocate(2).putInt(blockNumber).array();
	}
	
	/**
	 * Generate the data array of the current packet
	 * 
	 * @return byte array which contains all the required information
	 */
	public byte[] generateData() throws IOException {
		ByteArrayOutputStream steam = new ByteArrayOutputStream();
		steam.write(type.OPCODE());
		steam.write(blockNumber());
		steam.write(fileData, 0, fileData.length);
		return steam.toByteArray();
	}
	
}
