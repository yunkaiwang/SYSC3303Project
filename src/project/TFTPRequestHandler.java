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
	private byte[] data;
	private String filename;
	
	TFTPRequestHandler(TFTPServer server, DatagramPacket packet, InetAddress address, int port) {
		this.server = server;
		this.address = address;
		this.port = port;
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
	
	private void sendRequest(TFTPAckPacket packet) throws IOException {
		socket.send(packet.createDatagram(address, port));
	}
	
	private void sendRequest(TFTPDataPacket packet) throws IOException {
		socket.send(packet.createDatagram(address, port));
	}
	
	private TFTPDataPacket receiveDataPacket(int blockNumber) {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[TFTPDataPacket.MAX_LENGTH], TFTPDataPacket.MAX_LENGTH);
			socket.receive(receivePacket);
			return TFTPDataPacket.createFromPacket(receivePacket);
		} catch (IOException e) {
			System.out.println("Request handler failed to receive the response. Please try again.\n");
			return null;
		}
	}
	
	private TFTPAckPacket receiveAckPacket(int blockNumber) {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[TFTPAckPacket.PACKET_LENGTH], TFTPAckPacket.PACKET_LENGTH);
			socket.receive(receivePacket);
			return TFTPAckPacket.createFromPacket(receivePacket);
		} catch (IOException e) {
			System.out.println("Request handler failed to receive the response. Please try again.\n");
			return null;
		}
	}
	
	private void readFileFromClient() { // WRQ
		String filePath = server.getFilePath(filename);
		File file = null;
		try {
			file = new File(filePath);
			if (file.exists() && !file.canWrite()) {
				System.out.println("Request handler don't have permission to write " + filename + ". Please try again.\n");
				return;
			} else if (!file.exists()) { // create the file
				if (!file.createNewFile())
					throw new IOException("Failed to create " + filename);
			}

			FileOutputStream fs = new FileOutputStream(filePath);
			if (!createConnection()) { // socket create failed
				System.out.println("Request handler failed to create the socket, please check your network status and try again.\n");
				fs.close();
				return;
			}

			int blockNumber = 0;
			sendRequest(new TFTPAckPacket(blockNumber));
			TFTPDataPacket DATAPacket;

			do {
				DATAPacket = receiveDataPacket(++blockNumber);
				
				fs.write(DATAPacket.getFileData());
				sendRequest(new TFTPAckPacket(blockNumber));
				++blockNumber;
			} while (!DATAPacket.isLastDataPacket());
			fs.close();
		} catch (FileNotFoundException e) {
			file.delete();
			System.out.println("Request handler failed to write " + filename + ". Please try again.\n");
			return;
		} catch (IOException e) {
			file.delete();
			System.out.println("Request handler failed to send the request. Please try again.\n");
			return;
		}
		
	}

	private void writeFileToClient() { // RRQ
		String filePath = server.getFilePath(filename);
		File file = null;
		try {
			file = new File(filePath);
			if (!file.exists() || !file.canRead()) {
				System.out.println("Request handler don't have permission to read " + filename + ". Please try again.\n");
				return;
			}

			FileInputStream fs = new FileInputStream(filePath);
			if (!createConnection()) { // socket create failed
				System.out.println("Request handler failed to create the socket, please check your network status and try again.\n");
				fs.close();
				return;
			}
			
			byte[] data = new byte[TFTPDataPacket.MAX_DATA_LENGTH];
			int byteUsed = 1;
			int blockNumber = 1;
			
			do {
				byteUsed = fs.read(data);
				if (byteUsed == -1) {
					byteUsed = 0;
					data = new byte[0];
				}
				sendRequest(new TFTPDataPacket(blockNumber, Arrays.copyOfRange(data, 0, byteUsed), byteUsed));
				receiveAckPacket(blockNumber++);
			} while (byteUsed == TFTPDataPacket.MAX_DATA_LENGTH);
			fs.close();
		} catch (FileNotFoundException e) {
			System.out.println("Request handler failed to read " + filename + ". Please try again.\n");
			return;
		} catch (IOException e) {
			System.out.println("Request handler failed to send the request. Please try again.\n");
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
