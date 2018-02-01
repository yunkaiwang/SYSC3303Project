package project;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class TFTPRequestPacket {
	protected final Type type;
	protected static final int MAX_LENGTH = 516;
	protected static final int MIN_LENGTH = 4;
	
	TFTPRequestPacket(Type type) {
		this.type = type;
	}

	public DatagramPacket createDatagram(InetAddress serverAddress, int serverPort) {
		// TODO Auto-generated method stub
		return null;
	}

	public static TFTPRequestPacket createReadRequest(String filename) {
		// TODO Auto-generated method stub
		return null;
	}

	public static TFTPRequestPacket createWriteRequest(String filename) {
		// TODO Auto-generated method stub
		return null;
	}
}
