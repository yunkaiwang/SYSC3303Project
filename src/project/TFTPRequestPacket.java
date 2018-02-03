package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class TFTPRequestPacket {
	private final Type type;
	private static final String mode = "octet"; // default mode (as described in the project description, it doesn't matter
	                                           // whether the mode is netascii or octet)
	private String filename;
	private InetAddress address;
	private int port;
	
	private TFTPRequestPacket(String filename, Type type, InetAddress address, int port) {
		this.filename = filename;
		this.type = type;
		this.address = address;
		this.port = port;
	}
	
	/**
	 * Generate the data array of the current packet
	 * 
	 * @return byte array which contains all the required information
	 */
	private byte[] generateData() throws IOException {
		ByteArrayOutputStream steam = new ByteArrayOutputStream();
		steam.write(type.OPCODE());
		byte[] filenameInByte = filename.getBytes();
		steam.write(filenameInByte, 0, filenameInByte.length);
		steam.write(0);
		byte[] modeInByte = mode.toLowerCase().getBytes();
		steam.write(modeInByte, 0, modeInByte.length);
		steam.write(0);
		return steam.toByteArray();
	}

	public InetAddress getAddress() { return address; }
	public int getPort() { return port; }
	public int getLength() throws IOException { return generateData().length; }
	public String type() { return type.type(); }
	public String getFilename() { return filename; }
	public String getMode() { return mode; }
	
	public static TFTPRequestPacket createWriteRequest(String filename, InetAddress address, int port) {
		return new TFTPRequestPacket(filename, Type.WRQ, address, port);
	}

	public static TFTPRequestPacket createReadRequest(String filename, InetAddress address, int port) {
		return new TFTPRequestPacket(filename, Type.RRQ, address, port);
	}

	public static TFTPRequestPacket createFromPacket(DatagramPacket packet) {
		return createFromPacketData(packet.getData(), packet.getLength(), packet.getAddress(), packet.getPort());
	}
	
	/**
	 * Generate the packet data
	 * 
	 * @param packetData
	 * @param packetLength
	 * @return the byte array of the packet
	 */
	public static TFTPRequestPacket createFromPacketData(byte[] packetData, int packetDataLength, InetAddress address, int port) {
		// verify op code
		int OPCODE = ((packetData[0] << 8) & 0xFF00)
				| (packetData[1] & 0xFF);
		if (!(OPCODE == 1 || OPCODE == 2)) {
			throw new IllegalArgumentException("Invalid OP code");
		}
		
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder();
		while (packetData[++i] != 0 && i < packetDataLength) {
			filenameBuilder.append((char) packetData[i]);
		}
		String filename = filenameBuilder.toString();
		
		switch(OPCODE) {
		case(1):
			return createReadRequest(filename, address, port);
		case(2):
			return createWriteRequest(filename, address, port);
		}
		return null;
	}
	
	public DatagramPacket createDatagram() throws IOException {
		byte[] data = generateData();
		return new DatagramPacket(data, data.length, address, port);
	}
	
}
