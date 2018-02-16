package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * TFTPAckPacket class
 * 
 * @author Qingyi Yin
 *
 */
public class TFTPAckPacket extends TFTPPacket {
	private static final Type DEFAULT_TYPE = Type.ACK; // default packet type
	public static final int PACKET_LENGTH = 4; // length of a Ack packet
	private static final int MIN_BLOCK_NUMBER = 0; // minimum block number(0)
	private static final int MAX_BLOCK_NUMBER = 0xffff; // maximum block number(65535)
	private int blockNumber; // block number of the packet

	/**
	 * Constructor
	 * 
	 * @param blockNumber
	 * @param address
	 * @param port
	 */
	TFTPAckPacket(int blockNumber, InetAddress address, int port) {
		super(DEFAULT_TYPE, address, port);
		if (!validBlockNumber(blockNumber))
			throw new IllegalArgumentException("Invalid block number");
		this.blockNumber = blockNumber;
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
		return createFromPacketData(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()), packet.getLength(),
				packet.getAddress(), packet.getPort());
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
		if (!Type.validOPCODE(DEFAULT_TYPE, OPCODE)) {
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
	@Override
	protected byte[] generateData() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(DEFAULT_TYPE.OPCODE());
		stream.write(blockNumber());
		return stream.toByteArray();
	}
	
	/**
	 * Override toString method
	 */
	@Override
	public String toString() {
		return ("Packet type: " + this.type() + "\nDestination: \n" + "IP address: " +
	            this.getAddress() + "\nPort: " + this.getPort() + "\nInformation in this packet: " +
		        "\nBlock number: " + this.getBlockNumber() + "\n");
	}
}
