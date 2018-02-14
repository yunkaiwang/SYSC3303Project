package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class TFTPDataPacket extends TFTPPacket {
	private static final Type DEFAULT_TYPE = Type.DATA; // default packet type
	private static final int HEADER_LENGTH = 4; // packet header length
	public static final int MAX_DATA_LENGTH = 512; // max data length
	private static final int MIN_BLOCK_NUMBER = 0; // minimum block number(0)
	private static final int MAX_BLOCK_NUMBER = 0xffff; // maximum block number(65535)
	private int blockNumber; // block number of the packet
	private byte[] fileData; // file data of the packet

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 * @param fileData
	 * @param fileDataLength
	 * @param address
	 * @param port
	 */
	TFTPDataPacket(int blockNumber, byte[] fileData, int fileDataLength, InetAddress address, int port) {
		super(DEFAULT_TYPE, address, port);
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

	/**
	 * Getter
	 * 
	 * @return length
	 */
	public int getLength() {
		return fileData.length;
	}

	/**
	 * Getter
	 * 
	 * @return blockNumber
	 */
	public int getBlockNumber() {
		return this.blockNumber;
	}
	
	/**
	 * Getter
	 * 
	 * @return fileData
	 */
	public byte[] getFileData() {
		return this.fileData;
	}
	
	/**
	 * Create new data packet from datagram packet
	 * 
	 * @param packet
	 * @return TFTPDataPacket
	 */
	public static TFTPDataPacket createFromPacket(DatagramPacket packet) {
		return createFromPacketData(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()), packet.getLength(),
				packet.getAddress(), packet.getPort());
	}

	/**
	 * Create new data packet from datagram packet data
	 * 
	 * @param packetData
	 * @param packetDataLength
	 * @param address
	 * @param port
	 * @return TFTPDataPacket
	 */
	public static TFTPDataPacket createFromPacketData(byte[] packetData, int packetDataLength, InetAddress address,
			int port) {
		if (!validPacketData(packetData, packetDataLength))
			throw new IllegalArgumentException("Invalid packet data");
		int OPCODE = ((packetData[0] << 8) & 0xFF00) | (packetData[1] & 0xFF);
		if (!Type.validOPCODE(DEFAULT_TYPE, OPCODE))
			throw new IllegalArgumentException("Invalid OP code " + OPCODE);
		int blockNumber = ((packetData[2] << 8) & 0xFF00) | (packetData[3] & 0xFF);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(packetData, HEADER_LENGTH, packetDataLength - HEADER_LENGTH);
		packetData = stream.toByteArray();
		return new TFTPDataPacket(blockNumber, packetData, packetData.length, address, port);
	}

	/**
	 * Check if the given fileData byte array is valid
	 * 
	 * @param fileData
	 * @param fileDataLength
	 * @return true if the file data is valid, false otherwise
	 */
	private static boolean validFileData(byte[] fileData, int fileDataLength) {
		return ((fileData == null && fileDataLength == 0)
				|| fileData != null && fileData.length == fileDataLength && fileDataLength <= MAX_DATA_LENGTH);
	}

	/**
	 * Check if the given block number is valid
	 * 
	 * @param blockNumber
	 * @return true if the block number is valid, false otherwise
	 */
	private static boolean validBlockNumber(int blockNumber) {
		return (blockNumber >= MIN_BLOCK_NUMBER && blockNumber <= MAX_BLOCK_NUMBER);
	}

	/**
	 * Check if the given packet data is valid
	 * 
	 * @param packetData
	 * @param packetDataLength
	 * @return true if the packet data is valid, false otherwise
	 */
	private static boolean validPacketData(byte[] packetData, int packetDataLength) {
		return (packetData != null && packetData.length == packetDataLength && packetDataLength <= MAX_LENGTH
				&& packetDataLength >= HEADER_LENGTH);
	}

	/**
	 * Check if the current packet is the last data packet
	 * 
	 * @return true if the packet is the last packet, false otherwise
	 */
	public boolean isLastDataPacket() {
		return this.fileData != null && this.fileData.length < MAX_DATA_LENGTH;
	}

	/**
	 * Convert the block number into byte array of length 2
	 * 
	 * @return byteArray
	 */
	private byte[] blockNumber() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(blockNumber >> 8);
		stream.write(blockNumber);
		return stream.toByteArray();
	}

	/**
	 * Generate the byte array that contains all information
	 * 
	 * @return byteArray
	 * @throws IOException
	 */
	@Override
	public byte[] generateData() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(DEFAULT_TYPE.OPCODE());
		stream.write(blockNumber());
		stream.write(fileData, 0, fileData.length);
		return stream.toByteArray();
	}
	
	public String toString() {
		return ("Packet type: " + this.type() + "\nDestination: \n" + 
	            "IP address: " + this.getAddress() + "\nPort: " + this.getPort() +
	            "\nInformation in this packet: \n" + "Block number: " +
	            this.getBlockNumber() + "\nData length: " + this.getLength() + "\n");
	}
}
