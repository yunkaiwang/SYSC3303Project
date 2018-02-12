package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public abstract class TFTPPacket {
	public static final int MAX_LENGTH = 516; // max packet data length (complete data packet)
	public static final int MIN_LENGTH = 4; // min packet data length (ack packet)
	private final Type type;
	private InetAddress address; // destination address
	private int port; // destination port

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
	protected abstract byte[] generateData() throws IOException;

	/**
	 * This function simply calls generateData function, implemented so that the
	 * TFTPRequestPacket instance can be used as same as DatagramPacket.
	 * 
	 * @return byteArray
	 * @throws IOException
	 */
	public byte[] getData() throws IOException {
		return this.generateData();
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
		byte[] data = generateData();
		return new DatagramPacket(data, data.length, address, port);
	}
}
