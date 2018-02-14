package project;

import java.net.DatagramSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

public class TFTPRequestHandler extends Thread {
	private TFTPServer server; // server that this listener is working for
	private InetAddress address; // client address
	private int port; // client port
	private DatagramSocket socket;
	private TFTPRequestPacket packet;
	private byte[] data; // packet data
	private String filename; // filename of the request
	
	/**
	 * Constructor
	 * 
	 * @param server
	 * @param packet
	 * @param address
	 * @param port
	 */
	TFTPRequestHandler(TFTPServer server, DatagramPacket packet, InetAddress address, int port) {
		this.server = server;
		this.address = address;
		this.port = port;
		this.packet = TFTPRequestPacket.createFromPacket(packet);
		this.data = packet.getData();
		this.extractFileName(data);
	}
	
	/**
	 * Extract the filename from the data
	 * 
	 * @param data
	 */
	private void extractFileName(byte[] data) {
		int i = 1;
		StringBuilder filenameBuilder = new StringBuilder();
		while (data[++i] != 0) {
			filenameBuilder.append((char) data[i]);
		}
		filename = filenameBuilder.toString();
	}
	
	/**
	 * Handler the request
	 */
	private void handleRequest() {
		if (server.isReadRequest(data)) { // RRQ
			writeFileToClient();
		} else { // WRQ
			readFileFromClient(); 
		}
	}
	
	/**
	 * Send the request
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void sendRequest(DatagramPacket packet) throws IOException {
		socket.send(packet);
	}
	
	/**
	 * receive data packet
	 * 
	 * @param blockNumber
	 * @return TFTPDataPacket
	 */
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
	
	/**
	 * receive ack packet
	 * 
	 * @param blockNumber
	 * @return TFTPAckPacket
	 */
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
	
	private void sendFileNotFound(String errorMsg) {
		ThreadLog.print("Request handler has sent file not found error packet back to client.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createFileNotFoundErrorPacket(errorMsg, address, port);
		try {
			sendRequest(errorPacket.createDatagramPacket());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void sendFileAlreadyExist(String errorMsg) {
		ThreadLog.print("Request handler has sent file already exist error packet back to client.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createFileAlreadyExistErrorPacket(errorMsg, address, port);
		try {
			sendRequest(errorPacket.createDatagramPacket());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void sendAccessViolation(String errorMsg) {
		ThreadLog.print("Request handler has sent access violation error packet back to client.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createAccessViolationErrorPacket(errorMsg, address, port);
		try {
			sendRequest(errorPacket.createDatagramPacket());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Handle WRQ
	 */
	private void readFileFromClient() {
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
			socket = new DatagramSocket();

			file = new File(filePath);
			if (file.exists()) {
				sendFileAlreadyExist(filename + " already exists in server folder!");
				return;
			}
			if (!file.getParentFile().canWrite()) { // client has no permission to write the folder
				sendAccessViolation(filename + " cannot be modified!");
				return;
			}
			
			FileOutputStream fs = new FileOutputStream(filePath);

			int blockNumber = 0;
			
			// request handler forms the ack packet
			TFTPAckPacket AckPacket = new TFTPAckPacket(blockNumber, address, port);
			
			// request handler sends the ack packet
			sendRequest(AckPacket.createDatagramPacket());
			ThreadLog.print("Request handler have sent the Ack packet.");
			server.printInformation(AckPacket);

			TFTPDataPacket DATAPacket;
			// run until all data have been received
			do {
				// request handler receives the data packet
				DATAPacket = receiveDataPacket(++blockNumber);
				ThreadLog.print("Request handler have received the Data packet.");
				server.printInformation(DATAPacket);
				fs.write(DATAPacket.getFileData());
	
				// request handler forms the ack packet
				AckPacket = new TFTPAckPacket(blockNumber++, address, port);
	
				// request handler sends the ack packet
				sendRequest(AckPacket.createDatagramPacket());
				ThreadLog.print("Request handler have sent the Ack packet.");
				server.printInformation(AckPacket);
			} while (!DATAPacket.isLastDataPacket());
			fs.close();
		} catch (SocketException e) {
			ThreadLog.print("Request handler failed to create the socket, please check your network status and try again.\n");
			return;
		} catch (IOException e) {
			file.delete();
			ThreadLog.print("Request handler failed to send the request. Please try again.\n");
			return;
		}
		
	}

	/**
	 * Handle RRQ
	 */
	private void writeFileToClient() {
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
			socket = new DatagramSocket();

			file = new File(filePath);
			if (!file.exists()) {
				this.sendFileNotFound(filename + " not found in server's folder.");
				return;
			}
			
			if (!file.canRead()) {
				this.sendAccessViolation("Server has no permission to read " + filename);
				return;
			}

			FileInputStream fs = new FileInputStream(filePath);
			
			byte[] data = new byte[TFTPDataPacket.MAX_DATA_LENGTH];
			int byteUsed = 1;
			int blockNumber = 1;
			TFTPAckPacket AckPacket;
			do {
				byteUsed = fs.read(data);
				
				// special case when the file length is a multiple of 512,
				// then just send a empty data to indicate that the file has
				// all been transfered
				if (byteUsed == -1) {
					byteUsed = 0;
					data = new byte[0];
				}
				
				// request handler forms the data packet
				TFTPDataPacket DATAPacket = new TFTPDataPacket(blockNumber, Arrays.copyOfRange(data, 0, byteUsed), byteUsed, address, port);
				
				// request handler sends the packet
				sendRequest(DATAPacket.createDatagramPacket());
				ThreadLog.print("Request handler have sent the Data packet.");
				server.printInformation(DATAPacket);
				
				// request handler forms the ack packet
				AckPacket = receiveAckPacket(blockNumber++);
				ThreadLog.print("Request handler have received the ack packet.");
				server.printInformation(AckPacket);
			} while (byteUsed == TFTPDataPacket.MAX_DATA_LENGTH);
			fs.close();
		} catch (SocketException e) {
			ThreadLog.print("Request handler failed to create the socket, please check your network status and try again.\n");
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
