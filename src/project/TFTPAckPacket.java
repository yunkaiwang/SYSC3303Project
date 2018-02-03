package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class TFTPAckPacket {
	private static final Type type = Type.ACK; // default packet type
	public static final int PACKET_LENGTH = 4; // length of a Ack packet
	private static final int MIN_BLOCK_NUMBER = 0; // minimum block number(0)
	private static final int MAX_BLOCK_NUMBER = 0xffff; // maximum block number(65535)
	private int blockNumber; // block number of the packet
	private InetAddress address; // destination address
	private int port; // destination port

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 * @param address
	 * @param port
	 */
	TFTPAckPacket(int blockNumber, InetAddress address, int port) {
		if (!validBlockNumber(blockNumber))
			throw new IllegalArgumentException("Invalid block number");

		this.blockNumber = blockNumber;
		this.address = address;
		this.port = port;
	}

	/**
	 * Getter
	 * 
	 * @return address
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * Getter
	 * 
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Getter
	 * 
	 * @return type
	 */
	public String type() {
		return type.type();
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
	 * Check if the given block number is valid
	 * 
	 * @param blockNumber - the block number
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
		return (packetData != null && packetData.length == packetDataLength && packetDataLength == PACKET_LENGTH);
	}

	/**
	 * Create new TFTPAckPacket from packet
	 * 
	 * @param packet
	 * @return TFTPAckPacket
	 */
	public static TFTPAckPacket createFromPacket(DatagramPacket packet) {
		return createFromPacketData(packet.getData(), packet.getLength(), packet.getAddress(), packet.getPort());
	}

	/**
	 * Create new TFTPAckPacket from packet data
	 * 
	 * @param packetData
	 * @param packetDataLength
	 * @param address
	 * @param port
	 * @return TFTPAckPacket
	 */
	public static TFTPAckPacket createFromPacketData(byte[] packetData, int packetDataLength, InetAddress address,
			int port) {
		// check if the data length is correct
		if (!validPacketData(packetData, packetDataLength)) {
			throw new IllegalArgumentException("Invalid packet data");
		}
		// verify op code
		int OPCODE = ((packetData[0] << 8) & 0xFF00) | (packetData[1] & 0xFF);
		if (!Type.validOPCODE(type, OPCODE)) {
			throw new IllegalArgumentException("Invalid OP code");
		}

		int blockNumber = ((packetData[2] << 8) & 0xFF00) | (packetData[3] & 0xFF);
		return new TFTPAckPacket(blockNumber, address, port);
	}

	/**
	 * Generate the byte array that contains all information
	 * 
	 * @return byteArray
	 * @throws IOException
	 */
	public byte[] generateData() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(type.OPCODE());
		stream.write(blockNumber());
		return stream.toByteArray();
	}

	/**
	 * Create new DatagramPacket from this ack packet
	 * 
	 * @return DatagramPacket
	 * @throws IOException
	 */
	public DatagramPacket createDatagram() throws IOException {
		byte[] data = generateData();
		return new DatagramPacket(data, data.length, address, port);
	}
}
