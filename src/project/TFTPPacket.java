package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Abstract parent class for all TFTPPackets
 * 
 * @author yunkai wang
 *
 */
public abstract class TFTPPacket {
	public static final int TIMEOUT = 2000; // time out
	public static final int MAX_RETRY = 5; // maximum retry time
	public static final int MAX_LENGTH = 516; // max packet data length (complete data packet)
	public static final int MIN_LENGTH = 4; // min packet data length (ack packet)
	protected static final int MIN_BLOCK_NUMBER = 0; // minimum block number(0)
	protected static final int MAX_BLOCK_NUMBER = 0xffff; // maximum block number(65535)
	private final Type type; // type
	private InetAddress address; // destination address
	private int port; // destination port

	/**
	 * Constructor, can only be accessed using child classes
	 * 
	 * @param type
	 * @param address
	 * @param port
	 */
	protected TFTPPacket(Type type, InetAddress address, int port) {
		this.type = type;
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
	public Type type() {
		return this.type;
	}

	/**
	 * Generate the byte array that contains all information
	 * 
	 * @return byteArray
	 * @throws IOException
	 */
	protected abstract byte[] getData() throws IOException;

	/**
	 * Getter the length of bytes contained in the packet
	 * 
	 * @return dataLength
	 * @throws IOException
	 */
	public int getLength() throws IOException {
		return getData().length;
	}

	
	/**
	 * Create new TFTPPacket from packet
	 * 
	 * @param packet
	 * @return TFTPAckPacket
	 */
	public static TFTPPacket createFromPacket(DatagramPacket packet) {
		return createFromPacketData(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()), packet.getLength(),
				packet.getAddress(), packet.getPort());
	}

	/**
	 * Create new datagram packet for receive
	 * 
	 * @return DatagramPacket
	 */
	public static DatagramPacket createDatagramPacketForReceive() {
		return new DatagramPacket(new byte[MAX_LENGTH * 2], MAX_LENGTH * 2);
	}
	
	/**
	 * Create new TFTPPacket from packet data
	 * 
	 * @param packetData
	 * @param packetDataLength
	 * @param address
	 * @param port
	 * @return TFTPAckPacket
	 */
	public static TFTPPacket createFromPacketData(byte[] packetData, int packetDataLength, InetAddress address,
			int port) {
		int OPCODE = ((packetData[0] << 8) & 0xFF00) | (packetData[1] & 0xFF);
		switch (OPCODE) {
		case (1):
		case (2):
			return TFTPRequestPacket.createFromPacketData(packetData, packetDataLength, address, port);
		case (3):
			return TFTPDataPacket.createFromPacketData(packetData, packetDataLength, address, port);
		case (4):
			return TFTPAckPacket.createFromPacketData(packetData, packetDataLength, address, port);
		case (5):
			return TFTPErrorPacket.createFromPacketData(packetData, packetDataLength, address, port);
		default:
			throw new IllegalArgumentException("Invalid OP code");
		}
	}

	/**
	 * Create new DatagramPacket from this packet
	 * 
	 * @return DatagramPacket
	 * @throws IOException
	 */
	public DatagramPacket createDatagramPacket() throws IOException {
		byte[] data = getData();
		return new DatagramPacket(data, data.length, address, port);
	}
	
	/**
	 * require all TFTPPacket classes to provide toString function
	 */
	public abstract String toString();
}
