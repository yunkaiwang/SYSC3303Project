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
	 * Receive datagram packet
	 * 
	 * @return datagramPacket
	 * @throws IOException
	 */
	private DatagramPacket receivePacket() throws IOException {
		DatagramPacket packet = new DatagramPacket(new byte[TFTPPacket.MAX_LENGTH], TFTPPacket.MAX_LENGTH);
		socket.receive(packet);
		return packet;
	}

	private void sendFileNotFound(String errorMsg) throws IOException {
		ThreadLog.print("Request handler has sent file not found error packet back to client.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createFileNotFoundErrorPacket(errorMsg, address, port);
		sendRequest(errorPacket.createDatagramPacket());
	}
	
	private void sendDiskFull(String errorMsg) throws IOException {
		ThreadLog.print("Request handler has sent disk full error packet back to client.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createDiskfullErrorPacket(errorMsg, address, port);
		sendRequest(errorPacket.createDatagramPacket());
	}
	
	private void sendFileAlreadyExist(String errorMsg) throws IOException {
		ThreadLog.print("Request handler has sent file already exist error packet back to client.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createFileAlreadyExistErrorPacket(errorMsg, address, port);
		sendRequest(errorPacket.createDatagramPacket());
	}

	private void sendAccessViolation(String errorMsg) throws IOException {
		ThreadLog.print("Request handler has sent access violation error packet back to client.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createAccessViolationErrorPacket(errorMsg, address, port);
		sendRequest(errorPacket.createDatagramPacket());
	}

	/**
	 * Handle WRQ
	 */
	private void readFileFromClient() {
		String filePath = server.getFilePath(filename);
		File file = null;
		FileOutputStream fs = null;
		try {
			ThreadLog.print("Request handler have received the WRQ.");
			server.printInformation(packet);

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

			fs = new FileOutputStream(filePath);

			int blockNumber = 0;

			// request handler forms the ack packet
			TFTPAckPacket AckPacket = new TFTPAckPacket(blockNumber, address, port);

			// request handler sends the ack packet
			sendRequest(AckPacket.createDatagramPacket());
			ThreadLog.print("Request handler have sent the Ack packet.");
			server.printInformation(AckPacket);

			TFTPDataPacket DATAPacket;
			TFTPPacket packet;
			// run until all data have been received
			do {
				packet = TFTPPacket.createFromPacket(receivePacket());

				if (packet instanceof TFTPDataPacket)
					DATAPacket = (TFTPDataPacket) packet;
				else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");

				ThreadLog.print("Request handler have received the Data packet.");
				server.printInformation(DATAPacket);
				
				// get free space
				long freeSpace = file.getFreeSpace();
				
				// check if there is enough space to write the current data packet
				if (freeSpace >= DATAPacket.getLength())
					fs.write(DATAPacket.getFileData()); // write to the file
				else {
					String errorMsg = "Server don't have enough space to write " + filename;
					sendDiskFull(errorMsg);
					throw new TFTPErrorException(errorMsg); // abort the connection
				}

				// request handler forms the ack packet
				AckPacket = new TFTPAckPacket(blockNumber++, address, port);

				// request handler sends the ack packet
				sendRequest(AckPacket.createDatagramPacket());
				ThreadLog.print("Request handler have sent the Ack packet.");
				server.printInformation(AckPacket);
			} while (!DATAPacket.isLastDataPacket());
			fs.close();
		} catch (TFTPErrorException e) {
			file.delete(); // delete file in case error packet received
			ThreadLog.print("Request handler: Failed to write " + filename + 
					" from client as request handler received the following error message:" + 
					e.getMessage());
		} catch (SocketException e) {
			ThreadLog.print(
					"Request handler failed to create the socket, please check your network status and try again.\n");
		} catch (IOException e) {
			file.delete();
			ThreadLog.print("Request handler failed to send the request. Please try again.\n");
		} finally {
			try {
				if (fs != null)
					fs.close();
			} catch (IOException e) {
			}
		} // end of try-catch
	} // end of function

	/**
	 * Handle RRQ
	 */
	private void writeFileToClient() {
		String filePath = server.getFilePath(filename);
		File file = null;
		FileInputStream fs = null;
		try {
			ThreadLog.print("Request handler have received the RRQ.");
			server.printInformation(packet);

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

			fs = new FileInputStream(filePath);

			byte[] data = new byte[TFTPDataPacket.MAX_DATA_LENGTH];
			int byteUsed = 1;
			int blockNumber = 1;
			TFTPAckPacket AckPacket;
			TFTPPacket packet;
			
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
				TFTPDataPacket DATAPacket = new TFTPDataPacket(blockNumber, Arrays.copyOfRange(data, 0, byteUsed),
						byteUsed, address, port);

				// request handler sends the packet
				sendRequest(DATAPacket.createDatagramPacket());
				ThreadLog.print("Request handler have sent the Data packet.");
				server.printInformation(DATAPacket);

				packet = TFTPPacket.createFromPacket(receivePacket());
				if (packet instanceof TFTPAckPacket)
					AckPacket = (TFTPAckPacket) packet;
				else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");
				
				ThreadLog.print("Request handler have received the ack packet.");
				server.printInformation(AckPacket);
			} while (byteUsed == TFTPDataPacket.MAX_DATA_LENGTH);
			fs.close();
		} catch (TFTPErrorException e) {
			ThreadLog.print("Request handler: Failed to send " + filename + 
					" to client as request handler received the following error message:" +
					e.getMessage());
		} catch (SocketException e) {
			ThreadLog.print("Request handler failed to create the socket, please "
					+ "check your network status and try again.\n");
		} catch (IOException e) {
			ThreadLog.print("Request handler failed to send the request. Please try again.\n");
		} finally {
			try {
				if (fs != null)
					fs.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void run() {
		server.incrementNumThread(); // increase the thread count in server
		handleRequest();
		server.decrementNumThread(); // decrease the thread count in server
	}

}
