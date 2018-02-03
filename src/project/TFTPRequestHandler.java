package project;

import java.net.DatagramSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class TFTPRequestHandler extends Thread {
	private TFTPServer server; // server that this listener is working for
	private InetAddress address;
	private int port;
	private DatagramSocket socket;
	private TFTPRequestPacket packet;
	private byte[] data;
	private String filename;
	
	TFTPRequestHandler(TFTPServer server, DatagramPacket packet, InetAddress address, int port) {
		this.server = server;
		this.address = address;
		this.port = port;
		this.packet = TFTPRequestPacket.createFromPacket(packet);
		this.data = packet.getData();
		this.extractFileName(data);
	}
	
	private void extractFileName(byte[] data) {
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder();
		while (data[++i] != 0) {
			filenameBuilder.append((char) data[i]);
		}
		filename = filenameBuilder.toString();
	}
	
	// handle the request
	private void handleRequest() {
		if (server.isReadRequest(data)) { // RRQ
			writeFileToClient();
		} else { // WRQ
			readFileFromClient(); 
		}
	}
	
	private boolean createConnection() {
		try {
			socket = new DatagramSocket();
			return true;
		} catch (SocketException e) {
			return false;
		}
	}
	
	private void sendRequest(DatagramPacket packet) throws IOException {
		socket.send(packet);
	}
	
	private TFTPDataPacket receiveDataPacket(int blockNumber) {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[TFTPDataPacket.MAX_LENGTH], TFTPDataPacket.MAX_LENGTH);
			socket.receive(receivePacket);
			return TFTPDataPacket.createFromPacket(receivePacket);
		} catch (IOException e) {
			ThreadLog.print("Request handler failed to receive the response. Please try again.\n");
			return null;
		}
	}
	
	private TFTPAckPacket receiveAckPacket(int blockNumber) {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[TFTPAckPacket.PACKET_LENGTH], TFTPAckPacket.PACKET_LENGTH);
			socket.receive(receivePacket);
			return TFTPAckPacket.createFromPacket(receivePacket);
		} catch (IOException e) {
			ThreadLog.print("Request handler failed to receive the response. Please try again.\n");
			return null;
		}
	}
	
	private void readFileFromClient() { // WRQ
		try {
			ThreadLog.print("Request handler have received the WRQ.");
			server.printInformation(packet);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String filePath = server.getFilePath(filename);
		File file = null;
		try {
			file = new File(filePath);
			if (file.exists() && !file.canWrite()) {
				ThreadLog.print("Request handler don't have permission to write " + filename + ". Please try again.\n");
				return;
			} else if (!file.exists()) { // create the file
				if (!file.createNewFile())
					throw new IOException("Failed to create " + filename);
			}

			FileOutputStream fs = new FileOutputStream(filePath);
			if (!createConnection()) { // socket create failed
				ThreadLog.print("Request handler failed to create the socket, please check your network status and try again.\n");
				fs.close();
				return;
			}

			int blockNumber = 0;
			TFTPAckPacket AckPacket = new TFTPAckPacket(blockNumber, address, port);
			sendRequest(AckPacket.createDatagram());
			ThreadLog.print("Request handler have sent the Ack packet.");
			server.printInformation(AckPacket);

			TFTPDataPacket DATAPacket;

			do {
				DATAPacket = receiveDataPacket(++blockNumber);
				ThreadLog.print("Request handler have received the Data packet.");
				server.printInformation(DATAPacket);
				fs.write(DATAPacket.getFileData());
				AckPacket = new TFTPAckPacket(blockNumber, address, port);
				sendRequest(AckPacket.createDatagram());
				ThreadLog.print("Request handler have sent the Ack packet.");
				server.printInformation(AckPacket);
				++blockNumber;
			} while (!DATAPacket.isLastDataPacket());
			fs.close();
		} catch (FileNotFoundException e) {
			file.delete();
			ThreadLog.print("Request handler failed to write " + filename + ". Please try again.\n");
			return;
		} catch (IOException e) {
			file.delete();
			ThreadLog.print("Request handler failed to send the request. Please try again.\n");
			return;
		}
		
	}

	private void writeFileToClient() { // RRQ
		try {
			ThreadLog.print("Request handler have received the RRQ.");
			server.printInformation(packet);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String filePath = server.getFilePath(filename);
		File file = null;
		try {
			file = new File(filePath);
			if (!file.exists() || !file.canRead()) {
				ThreadLog.print("Request handler don't have permission to read " + filename + ". Please try again.\n");
				return;
			}

			FileInputStream fs = new FileInputStream(filePath);
			if (!createConnection()) { // socket create failed
				ThreadLog.print("Request handler failed to create the socket, please check your network status and try again.\n");
				fs.close();
				return;
			}
			
			byte[] data = new byte[TFTPDataPacket.MAX_DATA_LENGTH];
			int byteUsed = 1;
			int blockNumber = 1;
			TFTPAckPacket AckPacket;
			do {
				byteUsed = fs.read(data);
				if (byteUsed == -1) {
					byteUsed = 0;
					data = new byte[0];
				}
				TFTPDataPacket DATAPacket = new TFTPDataPacket(blockNumber, Arrays.copyOfRange(data, 0, byteUsed), byteUsed, address, port);
				sendRequest(DATAPacket.createDatagram());
				ThreadLog.print("Request handler have sent the Data packet.");
				server.printInformation(DATAPacket);
				AckPacket = receiveAckPacket(blockNumber++);
				ThreadLog.print("Request handler have received the ack packet.");
				server.printInformation(AckPacket);
			} while (byteUsed == TFTPDataPacket.MAX_DATA_LENGTH);
			fs.close();
		} catch (FileNotFoundException e) {
			ThreadLog.print("Request handler failed to read " + filename + ". Please try again.\n");
			return;
		} catch (IOException e) {
			ThreadLog.print("Request handler failed to send the request. Please try again.\n");
			return;
		}
	}

	@Override
	public void run() {
		server.incrementNumThread(); // increase the thread count in server
		handleRequest();
		server.decrementNumThread(); // decrease the thread count in server
	}
	
}
