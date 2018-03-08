package project;

import java.net.DatagramSocket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * TFTPRequesthandler class that will handle RRQ or WRQ received
 * by the Request listener thread
 * 
 * @author yunkai wang
 *
 */
public class TFTPRequestHandler extends Thread {
	private TFTPServer server; // server that this listener is working for
	private InetAddress address; // client address
	private int port; // client port
	private DatagramSocket socket; // socket for sending and receiving
	private TFTPRequestPacket packet; // the packet that initialized this handler thread
	private byte[] data; // packet data
	private String filename; // filename of the request
	private TFTPPacket lastPacket; // last packet sent
	
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
	 * Extract the filename from the packet data
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
	 * Handler the request based on the packet data
	 */
	private void handleRequest() {
		if (server.isReadRequest(data)) { // RRQ
			writeFileToClient();
		} else { // WRQ
			readFileFromClient();
		}
	}

	/**
	 * Send the datagram packet
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void sendRequest(TFTPPacket packet) throws IOException {
		lastPacket = packet;
		socket.send(packet.createDatagramPacket());
	}

	/**
	 * Receive datagram packet
	 * 
	 * @return datagramPacket
	 * @throws IOException
	 */
	private DatagramPacket receivePacket() throws IOException {
		DatagramPacket packet = TFTPPacket.createDatagramPacketForReceive();
		socket.receive(packet);
		return packet;
	}

	/**
	 * Send TFTPErrorPacket with file not found error to client
	 * 
	 * @param errorMsg
	 * @throws IOException
	 */
	private void sendFileNotFound(String errorMsg) throws IOException {
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createFileNotFoundErrorPacket(errorMsg, address, port);
		server.printInformation(
				ThreadLog.formatThreadPrint("Request handler has sent file not found error packet back to client."),
				errorPacket);
		sendRequest(errorPacket);
	}
	
	/**
	 * Send TFTPErrorPacket with disk full error to client
	 * 
	 * @param errorMsg
	 * @throws IOException
	 */
	private void sendDiskFull(String errorMsg) throws IOException {
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createDiskfullErrorPacket(errorMsg, address, port);
		server.printInformation(
				ThreadLog.formatThreadPrint("Request handler has sent disk full error packet back to client."),
				errorPacket);
		sendRequest(errorPacket);
	}
	
	/**
	 * Send TFTPErrorPacket with file already exist error to client
	 * 
	 * @param errorMsg
	 * @throws IOException
	 */
	private void sendFileAlreadyExist(String errorMsg) throws IOException {
		ThreadLog.print("Request handler has sent file already exist error packet back to client.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createFileAlreadyExistErrorPacket(errorMsg, address, port);
		server.printInformation(
				ThreadLog.formatThreadPrint("Request handler has sent file already exist error packet back to client."),
				errorPacket);
		sendRequest(errorPacket);
	}

	/**
	 * Send TFTPErrorPacket with access violation error to client
	 * 
	 * @param errorMsg
	 * @throws IOException
	 */
	private void sendAccessViolation(String errorMsg) throws IOException {
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createAccessViolationErrorPacket(errorMsg, address, port);
		server.printInformation(
				ThreadLog.formatThreadPrint("Request handler has sent access violation error packet back to client."),
				errorPacket);
		sendRequest(errorPacket);
	}

	/**
	 * Handle WRQ
	 */
	private void readFileFromClient() {
		String filePath = server.getFilePath(filename); // get full file path
		
		File file = null;
		FileOutputStream fs = null;
		boolean shouldDeleteFile = false; // in case any error happen, this will be set to true
		
		try {
			server.printInformation(
					ThreadLog.formatThreadPrint("Request handler have received the WRQ."),
					packet);
			
			file = new File(filePath);
			if (file.exists()) { // check if file already exist
				sendFileAlreadyExist(filename + " already exists in server folder!");
				return;
			}
			if (!file.getParentFile().canWrite()) { // check if client has permission to write
				sendAccessViolation(filename + " cannot be modified!");
				return;
			}

			fs = new FileOutputStream(filePath);
			
			// packets used for receiving
			TFTPDataPacket DATAPacket;
			TFTPPacket packet;
			int blockNumber = 0, numRetry;
			
			// request handler forms the ack packet
			TFTPAckPacket AckPacket = new TFTPAckPacket(blockNumber++, address, port);
			
			// request handler sends the ack packet
			sendRequest(AckPacket);
			server.printInformation(
					ThreadLog.formatThreadPrint("Request handler have sent the Ack packet."),
					AckPacket);

			// run until all data have been received
			do {
				numRetry = 0;
				while (true) {
					try {
						packet = TFTPPacket.createFromPacket(receivePacket());
		
						// check if received packet if TFTPDataPacket, if not, raise an exception
						if (packet instanceof TFTPDataPacket) {
							DATAPacket = (TFTPDataPacket) packet;
							// received correct data packet
							if (blockNumber == DATAPacket.getBlockNumber())
								break;
							// received old data packet, send the ack packet and
							// wait for the correct data packet
							else if (DATAPacket.getBlockNumber() < blockNumber) {
								sendRequest(new TFTPAckPacket(DATAPacket.getBlockNumber(), 
										address, port));
							}
						} else if (packet instanceof TFTPErrorPacket)
							throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
						else
							throw new TFTPErrorException("Unknown packet received.");
					} catch (SocketTimeoutException e) {
						if (numRetry >= TFTPPacket.MAX_RETRY)
							throw new TFTPErrorException("Connection lost.");
						++numRetry;
					}
				}
				
				// received packet is data packet
				server.printInformation(
						ThreadLog.formatThreadPrint("Request handler have received the Data packet."),
						DATAPacket);
				
				// get free space left in disk
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
				sendRequest(AckPacket);
				server.printInformation(
						ThreadLog.formatThreadPrint("Request handler have sent the Ack packet."),
						AckPacket);
			} while (!DATAPacket.isLastDataPacket());
			fs.close();
		} catch (TFTPErrorException e) {
			shouldDeleteFile = true;
			ThreadLog.print("Request handler: Failed to write " + filename + 
					" from client since following error message:\n" + 
					e.getMessage());
		} catch (SocketException e) {
			shouldDeleteFile = true;
			ThreadLog.print("Request handler failed to create the socket, " +
			        "please check your network status and try again.\n");
		} catch (IOException e) {
			shouldDeleteFile = true;
			ThreadLog.print("Request handler failed to send the request. Please try again.\n");
		} finally {
			try {
				if (fs != null)
					fs.close();
				if (shouldDeleteFile) // delete the file if any error happens
					file.delete();
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
			server.printInformation(
					ThreadLog.formatThreadPrint("Request handler have received the RRQ."), 
					packet);

			file = new File(filePath);
			if (!file.exists()) { // check if file exist
				this.sendFileNotFound(filename + " not found in server's folder.");
				return;
			}

			if (!file.canRead()) { // check if the file can be read
				this.sendAccessViolation("Server has no permission to read " + filename);
				return;
			}

			fs = new FileInputStream(filePath);

			byte[] data = new byte[TFTPDataPacket.MAX_DATA_LENGTH];
			int blockNumber = 1, byteUsed = 1, numRetry;
			
			// packets used for receiving
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
				sendRequest(DATAPacket);
				server.printInformation(
						ThreadLog.formatThreadPrint("Request handler have sent the Data packet."),
						DATAPacket);

				numRetry = 0;
				while (true) {
					try {
						packet = TFTPPacket.createFromPacket(receivePacket());
						
						// check if TFTPAckPacket is received from client, if not, raise an exception
						if (packet instanceof TFTPAckPacket) {
							AckPacket = (TFTPAckPacket) packet;
							if (AckPacket.getBlockNumber() == blockNumber)
								break;
						} else if (packet instanceof TFTPErrorPacket)
							throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
						else
							throw new TFTPErrorException("Unknown packet received.");
					} catch (SocketTimeoutException e) {
						if (numRetry == TFTPPacket.MAX_RETRY)
							throw new TFTPErrorException("Connection lost.");
						resendPacket(); // last packet might be lost, re-send last packet
						++numRetry;
					}
				}
				server.printInformation(
						ThreadLog.formatThreadPrint("Request handler have received the ack packet."),
						AckPacket);
				++blockNumber;
			} while (byteUsed == TFTPDataPacket.MAX_DATA_LENGTH);
			fs.close();
		} catch (TFTPErrorException e) { // handler TFTP error exception
			ThreadLog.print("Request handler: Failed to send " + filename + 
					" to client since the following error message:\n" +
					e.getMessage());
		} catch (SocketException e) { // handle socket exception
			ThreadLog.print("Request handler failed to create the socket, please "
					+ "check your network status and try again.\n");
		} catch (IOException e) { // handle IOException
			ThreadLog.print("Request handler failed to send the request. Please try again.\n");
		} finally { // close the file stream as the last step
			try {
				if (fs != null)
					fs.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * re-send the last packet send
	 * @throws IOException 
	 */
	private void resendPacket() throws IOException {
		ThreadLog.print("Last packet might be lost, sending last packet again...");
		if (lastPacket == null)
			return;
		sendRequest(lastPacket);
	}

	/**
	 * Override run method
	 */
	@Override
	public void run() {
		try {
			this.socket = new DatagramSocket();
			this.socket.setSoTimeout(TFTPPacket.TIMEOUT);
		} catch (SocketException e) {
			ThreadLog.print("Request handler failed to create the socket," 
					+ " cannot handle the request");
			return;
		}
		server.incrementNumThread(); // increase the thread count in server
		handleRequest();
		server.decrementNumThread(); // decrease the thread count in server
	}

}
