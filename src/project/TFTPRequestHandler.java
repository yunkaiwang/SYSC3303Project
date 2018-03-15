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
	private DatagramPacket packet; // the packet that initialized this handler thread
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
		this.packet = packet;
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
		try {
			TFTPPacket requestPacket = null;
			try {
				requestPacket = TFTPPacket.createFromPacket(packet);
			} catch (IllegalArgumentException e) { // received packet is invalid
				sendIllegalTFTPOperation(e.getMessage());
			}
			if (!(requestPacket instanceof TFTPRequestPacket))
				throw new IllegalArgumentException("Request handler is handling unknown packet");
			if (((TFTPRequestPacket) requestPacket).isReadRequest()) { // RRQ
				server.printInformation(ThreadLog.formatThreadPrint("Request handler has received the RRQ."), 
						requestPacket);
				writeFileToClient();
			} else { // WRQ
				server.printInformation(ThreadLog.formatThreadPrint("Request handler has received the WRQ."),
						requestPacket);
				readFileFromClient();
			}
		} catch (TFTPErrorException e) {
			ThreadLog.print("Request handler: Failed to send " + filename + 
					" to client since the following error message:\n" +
					e.getMessage());
		} catch (IOException e) {
			ThreadLog.print("Request handler failed to send the request. Please try again.\n");
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
	 * Send TFTPErrorPacket with illegal TFTP operation error to client
	 * 
	 * @param errorMsg
	 * @throws IOException
	 * @throws TFTPErrorException 
	 */
	private void sendIllegalTFTPOperation(String errorMsg) throws IOException, TFTPErrorException {
		this.sendIllegalTFTPOperation(errorMsg, address, port);
	}
	
	/**
	 * Send TFTPErrorPacket with illegal TFTP operation error to the given address and port
	 * 
	 * @param errorMsg
	 * @throws IOException
	 * @throws TFTPErrorException 
	 */
	private void sendIllegalTFTPOperation(String errorMsg, InetAddress address, int port) throws IOException, TFTPErrorException {
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createIllegalTFTPOperation(errorMsg, address, port);
		server.printInformation(
				ThreadLog.formatThreadPrint("Request handler has sent illegal TFTP operation packet back to client."),
				errorPacket);
		sendRequest(errorPacket);
		throw new TFTPErrorException(errorMsg);
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
	 * Send unknown tid error packet
	 * 
	 * @param errorMsg
	 * @throws IOException
	 */
	private void sendUnknownTid(String errorMsg, InetAddress address, int port) throws IOException {
		ThreadLog.print("Request handler has sent unknown tid error packet to " + addressToString(address, port) + ".");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createUnknownTID(errorMsg, address, port);
		sendRequest(errorPacket);
	}
	
	/**
	 * Convert an address and port into string for printing
	 * 
	 * @param address
	 * @param port
	 * @return
	 */
	private String addressToString(InetAddress address, int port) {
		return address.toString() + ":" + port;
	}
	
	/**
	 * Receive an ack packet with the specified block number
	 * 
	 * @param blockNumber
	 * @return AckPacket
	 * @throws IOException 
	 * @throws TFTPErrorException 
	 */
	private TFTPAckPacket receiveAck(int blockNumber) throws IOException, TFTPErrorException {
		// create packets for receiving and validating the packet
		DatagramPacket receivePacket = null;
		TFTPPacket packet;
		TFTPAckPacket AckPacket;
		int numRetry = 0; // record the number of times we have retried
		while (true) {
			try {
				receivePacket = receivePacket();
				// if this is the first packet received from server, record its port
				if (port == -1)
					port = receivePacket.getPort();
				else if (port != receivePacket.getPort() ||
						!address.equals(receivePacket.getAddress())) {
					String errorMsg = "This tid is invalid, please use the correct tid!";
					sendUnknownTid(errorMsg, receivePacket.getAddress(), receivePacket.getPort());
					continue;
				}
					
				packet = TFTPPacket.createFromPacket(receivePacket);

				if (packet instanceof TFTPAckPacket) {
					AckPacket = (TFTPAckPacket) packet;
					// received correct ack packet
					if (AckPacket.getBlockNumber() == blockNumber)
						return AckPacket;
					else if (AckPacket.getBlockNumber() < blockNumber)
						ThreadLog.print("Request handler has received one old ack packet, will ignore it...");
					else if (AckPacket.getBlockNumber() > blockNumber) { // received future ack packet, this is invalid
						String errorMsg = "Request handler has received future ack packet with block number: " + AckPacket.getBlockNumber();
						sendIllegalTFTPOperation(errorMsg);
					}
				} else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");
			} catch (IllegalArgumentException e) {
				sendIllegalTFTPOperation(e.getMessage(), receivePacket.getAddress(), port);
			} catch (SocketTimeoutException e) {
				if (numRetry >= TFTPPacket.MAX_RETRY)
					throw new TFTPErrorException("Connection lost.");
				resendPacket(); // last packet might be lost, re-send last packet
				++numRetry;
			}
		}
	}
	
	/**
	 * Receive a data packet with the specified block number
	 * 
	 * @param blockNumber
	 * @return AckPacket
	 * @throws IOException 
	 * @throws TFTPErrorException 
	 */
	private TFTPDataPacket receiveData(int blockNumber) throws IOException, TFTPErrorException {
		// create packets for receiving and validating the packet
		DatagramPacket receivePacket = null;
		TFTPPacket packet;
		TFTPDataPacket DATAPacket;
		int numRetry = 0;
		while (true) {
			try {
				// receive the data packet and create TFTPPacket from it
				receivePacket = receivePacket();
				// if this is the first packet received from server, record its port
				if (port == -1)
					port = receivePacket.getPort();
				else if (port != receivePacket.getPort() ||
						!address.equals(receivePacket.getAddress())) {
					String errorMsg = "This tid is invalid, please use the correct tid!";
					sendUnknownTid(errorMsg, receivePacket.getAddress(), receivePacket.getPort());
					continue;
				}
				
				packet = TFTPPacket.createFromPacket(receivePacket);
				// if received packet is not TFTPDataPacket, raise an exception
				if (packet instanceof TFTPDataPacket) {
					DATAPacket = (TFTPDataPacket) packet;
					// received correct data packet, continue transfer
					if (DATAPacket.getBlockNumber() == blockNumber)
						return DATAPacket;
					// received old data packet, send the ack packet and
					// wait for the correct data packet
					else if (DATAPacket.getBlockNumber() < blockNumber) {
						ThreadLog.print("Request handler has received one old data packet, sending the ack packet");
						sendRequest(new TFTPAckPacket(DATAPacket.getBlockNumber(), 
								address, port));
					} else if (DATAPacket.getBlockNumber() > blockNumber) { // received future data packet, this is invalid
						String errorMsg = "Request handler has received future data packet with block number: " + DATAPacket.getBlockNumber();
						sendIllegalTFTPOperation(errorMsg);
					}
				} else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");
			} catch (IllegalArgumentException e) {
				sendIllegalTFTPOperation(e.getMessage(), receivePacket.getAddress(), port);
			} catch (SocketTimeoutException e) {
				if (numRetry >= TFTPPacket.MAX_RETRY)
					throw new TFTPErrorException("Connection lost.");
				++numRetry;
			}
		}
	}
	
	/**
	 * Handle WRQ
	 * 
	 * @throws IOException 
	 */
	private void readFileFromClient() throws IOException {
		String filePath = server.getFilePath(filename); // get full file path
		
		File file = null;
		FileOutputStream fs = null;
		boolean shouldDeleteFile = false; // in case any error happen, this will be set to true
		
		try {
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
			int blockNumber = 0;
			
			// request handler forms the ack packet
			TFTPAckPacket AckPacket = new TFTPAckPacket(blockNumber++, address, port);
			
			// request handler sends the ack packet
			sendRequest(AckPacket);
			server.printInformation(
					ThreadLog.formatThreadPrint("Request handler has sent the Ack packet."),
					AckPacket);

			// run until all data has been received
			do {
				DATAPacket = receiveData(blockNumber);
				
				// received packet is data packet
				server.printInformation(
						ThreadLog.formatThreadPrint("Request handler has received the Data packet."),
						DATAPacket);
				
				// get free space left in disk
				long freeSpace = file.getFreeSpace();
				
				// check if there is enough space to write the current data packet
				if (freeSpace >= DATAPacket.getLength())
					fs.write(DATAPacket.getFileData()); // write to the file
				else {
					String errorMsg = "Server don't has enough space to write " + filename;
					sendDiskFull(errorMsg);
					throw new TFTPErrorException(errorMsg); // abort the connection
				}

				// request handler forms the ack packet
				AckPacket = new TFTPAckPacket(blockNumber++, address, port);
				
				// request handler sends the ack packet
				sendRequest(AckPacket);
				server.printInformation(
						ThreadLog.formatThreadPrint("Request handler has sent the Ack packet."),
						AckPacket);
			} while (!DATAPacket.isLastDataPacket());
		} catch (TFTPErrorException e) {
			shouldDeleteFile = true;
			ThreadLog.print("Request handler: Failed to write " + filename + 
					" from client since following error message:\n" + 
					e.getMessage());
		} finally {
			if (fs != null)
				fs.close();
			if (shouldDeleteFile) // delete the file if any error happens
				file.delete();
		} // end of try-catch
	} // end of function

	/**
	 * Handle RRQ
	 * 
	 * @throws IOException
	 * @throws TFTPErrorException 
	 */
	private void writeFileToClient() throws IOException, TFTPErrorException {
		String filePath = server.getFilePath(filename);
		File file = null;
		FileInputStream fs = null;
		try {
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
			int blockNumber = 1, byteUsed = 1;
			
			// packets used for receiving
			TFTPAckPacket AckPacket;
			TFTPDataPacket DATAPacket;
			
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
				DATAPacket = new TFTPDataPacket(blockNumber, Arrays.copyOfRange(data, 0, byteUsed),
						byteUsed, address, port);
				
				// request handler sends the packet
				sendRequest(DATAPacket);
				server.printInformation(
						ThreadLog.formatThreadPrint("Request handler has sent the Data packet."),
						DATAPacket);

				AckPacket = receiveAck(blockNumber);
				server.printInformation(
						ThreadLog.formatThreadPrint("Request handler has received the ack packet."),
						AckPacket);
				++blockNumber;
			} while (byteUsed == TFTPDataPacket.MAX_DATA_LENGTH);
		} finally { // close the file stream as the last step
			if (fs != null)
				fs.close();
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
