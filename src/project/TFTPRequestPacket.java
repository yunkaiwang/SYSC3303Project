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
	
	private TFTPRequestPacket(String filename, Type type) {
		this.filename = filename;
		this.type = type;
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

	public static TFTPRequestPacket createWriteRequest(String filename) {
		return new TFTPRequestPacket(filename, Type.WRQ);
	}

	public static TFTPRequestPacket createReadRequest(String filename) {
		return new TFTPRequestPacket(filename, Type.RRQ);
	}

	public DatagramPacket createDatagram(InetAddress serverAddress, int serverPort) throws IOException {
		byte[] data = generateData();
		return new DatagramPacket(data, data.length, serverAddress, serverPort);
	}
	
}
