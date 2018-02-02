package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class TFTPAckPacket {
	private static final Type type = Type.ACK; // type will always be ack
	public static final int PACKET_LENGTH = 4; // header length
	private static final int MIN_BLOCK_NUMBER = 0; // minimum block number(0)
	private static final int MAX_BLOCK_NUMBER = 0xffff; // maximum block number(65535)
	private int blockNumber; // block number of the packet

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 *            - the block number of the packet
	 */
	TFTPAckPacket(int blockNumber) {
		if (!validBlockNumber(blockNumber))
			throw new IllegalArgumentException("Invalid block number");
		
		this.blockNumber = blockNumber;
	}

	/**
	 * Getter for the block number
	 * 
	 * @return block number of the packet
	 */
	public int getBlockNumber() {
		return this.blockNumber;
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
				packetDataLength == PACKET_LENGTH);
	}
	

	public static TFTPAckPacket createFromPacket(DatagramPacket packet) {
		return createFromPacketData(packet.getData(), packet.getLength());
	}
	
	/**
	 * Generate the packet data
	 * 
	 * @param packetData
	 * @param packetLength
	 * @return the byte array of the packet
	 */
	public static TFTPAckPacket createFromPacketData(byte[] packetData, int packetDataLength) {
		// check if the data length is correct
		if (!validPacketData(packetData, packetDataLength)) {
			throw new IllegalArgumentException("Invalid packet data");
		}
		// verify op code
		int OPCODE = ByteBuffer.wrap(packetData, 0, 2).getInt();
		if (!Type.validOPCODE(type, OPCODE)) {
			throw new IllegalArgumentException("Invalid OP code");
		}

		int BlockNumber = ByteBuffer.wrap(packetData, 2, 2).getInt();
		return new TFTPAckPacket(BlockNumber);
	}

	/**
	 * Generate the data array of the current packet
	 * 
	 * @return byte array which contains all the required information
	 */
	public byte[] generateData() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(type.OPCODE());
		stream.write(blockNumber());
		return stream.toByteArray();
	}

	public DatagramPacket createDatagram(InetAddress serverAddress, int serverPort) throws IOException {
		byte[] data = generateData();
		return new DatagramPacket(data, data.length, serverAddress, serverPort);
	}
}
