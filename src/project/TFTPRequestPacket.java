package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class TFTPRequestPacket extends TFTPPacket {
	private static final String mode = "octet"; // default mode (as described in the project description, it doesn't
												// matter
												// whether the mode is netascii or octet)
	private String filename; // filename in this packet

	/**
	 * Constructor
	 * 
	 * @param filename
	 * @param type
	 * @param address
	 * @param port
	 */
	private TFTPRequestPacket(String filename, Type type, InetAddress address, int port) {
		super(type, address, port);
		this.filename = filename;
	}

	/**
	 * Check if the packet is a read request
	 * 
	 * @return true is the packet is a read request, false otherwise
	 */
	public boolean isReadRequest() {
		return this.type() == Type.RRQ;
	}
	
	/**
	 * Check if the packet is a write request
	 * 
	 * @return true is the packet is a write request, false otherwise
	 */
	public boolean isWriteRequest() {
		return this.type() == Type.WRQ;
	}
	
	/**
	 * Generate the byte array that contains all information
	 * 
	 * @return byteArray
	 * @throws IOException
	 */
	protected byte[] generateData() throws IOException {
		ByteArrayOutputStream steam = new ByteArrayOutputStream();
		steam.write(type().OPCODE());
		byte[] filenameInByte = filename.getBytes();
		steam.write(filenameInByte, 0, filenameInByte.length);
		steam.write(0);
		byte[] modeInByte = mode.toLowerCase().getBytes();
		steam.write(modeInByte, 0, modeInByte.length);
		steam.write(0);
		return steam.toByteArray();
	}

	/**
	 * Getter
	 * 
	 * @return dataLength
	 * @throws IOException
	 */
	public int getLength() throws IOException {
		return generateData().length;
	}

	/**
	 * Getter
	 * 
	 * @return fileName
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Getter
	 * 
	 * @return mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * Create new write request
	 * 
	 * @param filename
	 * @param address
	 * @param port
	 * @return TFTPRequestPacket
	 */
	public static TFTPRequestPacket createWriteRequest(String filename, InetAddress address, int port) {
		return new TFTPRequestPacket(filename, Type.WRQ, address, port);
	}

	/**
	 * Create new read request
	 * 
	 * @param filename
	 * @param address
	 * @param port
	 * @return TFTPRequestPacket
	 */
	public static TFTPRequestPacket createReadRequest(String filename, InetAddress address, int port) {
		return new TFTPRequestPacket(filename, Type.RRQ, address, port);
	}

	/**
	 * Create new TFTPRequestPacket from packet
	 * 
	 * @return TFTPRequestPacket
	 * @throws IOException
	 */
	public static TFTPRequestPacket createFromPacket(DatagramPacket packet) {
		return createFromPacketData(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()), packet.getLength(),
				packet.getAddress(), packet.getPort());
	}

	/**
	 * Create new TFTPRequestPacket from packet data
	 * 
	 * @param packetData
	 * @param packetDataLength
	 * @param address
	 * @param port
	 * @return TFTPRequestPacket
	 */
	public static TFTPRequestPacket createFromPacketData(byte[] packetData, int packetDataLength, InetAddress address,
			int port) {
		// verify op code
		int OPCODE = ((packetData[0] << 8) & 0xFF00) | (packetData[1] & 0xFF);
		if (!(OPCODE == 1 || OPCODE == 2)) {
			throw new IllegalArgumentException("Invalid OP code");
		}

		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder();
		while (packetData[++i] != 0 && i < packetDataLength) {
			filenameBuilder.append((char) packetData[i]);
		}
		String filename = filenameBuilder.toString();

		switch (OPCODE) {
		case (1):
			return createReadRequest(filename, address, port);
		case (2):
			return createWriteRequest(filename, address, port);
		}
		return null;
	}
	
	public String toString() {
		return ("Packet type: " + this.type() + "\nDestination: \n" + 
	            "IP address: " + this.getAddress() + "\nPort: " + this.getPort() +
	            "\nInformation in this packet: \n" + "Filename: " + this.getFilename() +
	            "\nMode: " + this.getMode() + "\n");
	}
}
