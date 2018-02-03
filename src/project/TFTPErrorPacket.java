package project;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

public class TFTPErrorPacket {
	private static final Type type = Type.ERROR; // type will always be error
	private int errorCode; // error code
	private String errorMsg; // error message
	private byte[] fileData; // file data of the packet
	private InetAddress address;
	private int port;

	/**
	 * Constructor
	 * 
	 * @param blockNumber    - the block number of the packet
	 * @param fileData       - the file data in the packet
	 * @param fileDataLength - the length of the file data in the packet
	 */
	TFTPErrorPacket(int errorCode, String errorMsg, InetAddress address, int port) {
		this.errorCode = errorCode;
		this.errorMsg = errorMsg;
		this.address = address;
		this.port = port;
	}

	public InetAddress getAddress() { return address; }
	public int getPort() { return port; }
	public int getErrorCode() { return errorCode; }
	public String type() { return type.type(); }
	public String getErrorMsg() { return errorMsg; }
	
	public static TFTPErrorPacket createFromPacket(DatagramPacket packet) {
		return createFromPacketData(Arrays.copyOfRange(packet.getData(), 0, packet.getLength()), packet.getLength(), packet.getAddress(), packet.getPort());
	}
	
	public static TFTPErrorPacket createFromPacketData(byte[] packetData, int packetDataLength, InetAddress address, int port) {
		int OPCODE = ((packetData[0] << 8) & 0xFF00)
				| (packetData[1] & 0xFF);
		if (!Type.validOPCODE(type, OPCODE))
			throw new IllegalArgumentException("Invalid OP code");
		int errorCode = ((packetData[2] << 8) & 0xFF00)
				| (packetData[3] & 0xFF);
		
		int i = 3;
		StringBuilder filenameBuilder = new StringBuilder();
		while (packetData[++i] != 0 && i < packetDataLength) {
			filenameBuilder.append((char) packetData[i]);
		}
		String errorMsg = filenameBuilder.toString();
		return new TFTPErrorPacket(errorCode, errorMsg, address, port);
	}
	
	/**
	 * Convert the block number into byte array of length 2
	 * 
	 * @return byte array which contains the block number
	 */
	private byte[] errorCode() {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(errorCode >> 8);
		stream.write(errorCode);
		return stream.toByteArray();
	}
	
	/**
	 * Generate the data array of the current packet
	 * 
	 * @return byte array which contains all the required information
	 */
	public byte[] generateData() throws IOException {
		ByteArrayOutputStream steam = new ByteArrayOutputStream();
		steam.write(type.OPCODE());
		steam.write(errorCode());
		steam.write(fileData, 0, fileData.length);
		return steam.toByteArray();
	}

	public DatagramPacket createDatagram() throws IOException {
		byte[] data = generateData();
		return new DatagramPacket(data, data.length, address, port);
	}
	
}
