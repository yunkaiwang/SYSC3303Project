package project;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * TFTPClient
 * 
 * @author yunkai wang
 *
 */
public class TFTPClient extends TFTPHost {
	// as required by the project, the client may run in two modes,
	// in test mode, the error simulator program will get involved,
	// in normal mode, the requests will be directly sent to the server
	private enum runningMode {
		test, normal;
	}

	private static final String DEFAULT_FOLDER = System.getProperty("user.dir") + File.separator
			+ "client_files" + File.separator; // default folder directory
	
	DatagramSocket socket; // socket used for sending and receiving
	private runningMode currentRunningMode; // test or normal, as described above
	
	private InetAddress serverAddress; // server address
	private int serverPort; // server port
	private int serverResponsePort; // server response port
	private TFTPPacket lastPacket; // last packet sent
	
	
	/**
	 * Constructor
	 * 
	 * @throws UnknownHostException
	 * @throws SocketException 
	 */
	TFTPClient() throws UnknownHostException, SocketException {
		super(DEFAULT_FOLDER, Mode.QUITE); //default mode is quite
		this.currentRunningMode = runningMode.normal; // default mode is normal
		this.serverAddress = InetAddress.getLocalHost(); // default server address is localhost
		this.serverPort = TFTPServer.TFTP_LISTEN_PORT; // default server port is 69
		this.serverResponsePort = -1;
		this.socket = new DatagramSocket();
		this.socket.setSoTimeout(TFTPPacket.TIMEOUT);
	}

	/**
	 * Switch the running mode(test/normal), after the mode has been switched, the
	 * server port will be updated accordingly
	 */
	private void switchRunningMode() {
		switch (currentRunningMode) {
		case test:
			currentRunningMode = runningMode.normal;
			serverPort = TFTPServer.TFTP_LISTEN_PORT; // all requests are sent to server
			return;
		case normal:
			currentRunningMode = runningMode.test;
			serverPort = TFTPErrorSimulator.TFTP_LISTEN_PORT; // send requests to error simulator
			return;
		}
	}

	/**
	 * Helper method for printing message
	 * 
	 * @param o
	 */
	private static void print(Object o) {
		System.out.println(o.toString());
	}
	
	/**
	 * Print the menu
	 */
	private static void printMenu() {
		print("Available commands:\n" +
		  "  menu             - show the menu\n" +
		  "  exit             - stop the client\n" +
		  "  mode             - show current mode\n" +
		  "  switch           - switch print mode(verbose or quite)\n" +
		  "  reset            - reset running mode(test or normal)\n" +
		  "  la/ls            - list of files under current directory\n" +
		  "  rm <filename>    - remove existing file\n" +
		  "  pwd/dir          - current directory\n" +
		  "  cd <folder>      - change directory, new directoly location should be written using /(i.e. ../new_client/)\n" +
		  "  touch <fn>       - create a new empty file for testing with the given file name(i.e. touch random.txt)\n" +
		  "  touch <fn> <size>- create a new file for testing with the given file name and the given size(i.e. touch random.txt 512)\n" +
		  "  read <filename>  - send RRQ(i.e. read text.txt)\n" + 
		  "  write <filename> - send WRQ(i.e. write text.txt)\n" +
		  "  ip               - print current server ip and port\n" +
          "  connect <ip>        - change server ip to the given address\n" +
		  "  connect <ip>:<port> - change server ip and port to the given address and port\n");
	}

	/**
	 * Print current running mode(test/normal)
	 */
	private void printRunningMode() {
		print("Current running mode is: " + currentRunningMode + "\n");
	}

	/**
	 * Terminate the client
	 */
	private void stopClient() {
		print("Terminating client.");
	}
	
	/**
	 * Main loop of this file, wait for new requests
	 */
	private void waitForCommand() {
		Scanner s = new Scanner(System.in);

		printMenu();
		while (true) {
			System.out.print("Command: ");
			String[] commands = s.nextLine().split("\\s+"); // split all command into array of command
			if (commands.length == 0) // no commands, continue
				continue;

			switch (commands[0].toLowerCase()) {
			case "menu": // print the menu
				printMenu();
				continue;
			case "exit": // exit the client
				s.close(); // close the scanner
				stopClient();
				return;
			case "pwd": // print current directory
			case "dir":
				printDirectory();
				continue;
			case "la": // print list of files
			case "ls":
				printListFiles();
				continue;
			case "ip":
				printServerIP();
				continue;
			case "connect":
				setServerIP(commands[1]);
				continue;
			case "rm": // print list of files
				removeFile(commands[1]);
				continue;
			case "cd": // change directory
				switchDirectory(commands[1]);
				continue;
			case "mode": // print current mode
				printMode();
				printRunningMode();
				continue;
			case "switch": // switch current print mode
				switchMode();
				continue;
			case "reset": // switch current test mode
				switchRunningMode();
				printRunningMode();
				continue;
			case "touch": // create file
				if (commands.length != 2 && commands.length != 3)
					print("Please enter a valid file name and file size(e.g. touch random.txt 512)");
				else {
					try {
						String fileName = commands[1];
						int fileSize = commands.length == 3 ? Integer.parseInt(commands[2]) : 0;
						createFile(fileName, fileSize);
					} catch (Exception e) { // parse failed, given file size is invalid
						print("Please enter a valid file name and file size(e.g. touch random.txt 512)");
					}
				}
				continue;
			case "read": // send RRQ
				if (commands.length != 2)
					print("Invalid request! Please enter a valid filename for "
							+ "read request(e.g. read text.txt)\n");
				else
					readFileFromServer(commands[1]);
				continue;
			case "write": // send WRQ
				if (commands.length != 2)
					print("Invalid request! Please enter a valid filename for "
							+ "write request(e.g. write text.txt)\n");
				else
					writeFileToServer(commands[1]);
				continue;
			default: // invalid request
				print("Invalid command, please try again!\n");
			}
		}
	}

	/**
	 * Print current server ip address
	 */
	private void printServerIP() {
		print("Current server ip is : " + addressToString(this.serverAddress, this.serverPort));
	}
	
	/**
	 * Set server ip address to the given new address
	 * 
	 * @param newAddress
	 */
	private void setServerIP(String newAddress) {
		try {
			String[] newAddressComponents = newAddress.split(":");
			if (newAddressComponents.length == 2)
				this.serverPort = Integer.parseInt(newAddressComponents[1]);
			if (newAddressComponents[0].equalsIgnoreCase("localhost"))
				this.serverAddress = InetAddress.getLocalHost();
			else
				this.serverAddress = InetAddress.getByName(newAddressComponents[0]);
		} catch (Exception e) {
			print(e.getMessage());
			print("Failed to change server ip to " + newAddress + ". Please try again.");
		}
	}

	/**
	 * Send packet
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void sendPacket(TFTPPacket packet) throws IOException {
		sendPacket(packet, false);
	}

	/**
	 * Send packet
	 * 
	 * @param packet
	 * @param recordForResend
	 * @throws IOException
	 */
	private void sendPacket(TFTPPacket packet, boolean recordForResend) throws IOException {
		if (recordForResend)
			lastPacket = packet;
		else
			lastPacket = null;
		socket.send(packet.createDatagramPacket());
	}
	
	/**
	 * Receive datagram packet
	 * 
	 * @return datagramPacket
	 * @throws IOException
	 * @throws SocketTimeoutException
	 */
	private DatagramPacket receivePacket() throws IOException, SocketTimeoutException {
		DatagramPacket packet = TFTPPacket.createDatagramPacketForReceive();
		socket.receive(packet);
		return packet;
	}

	/**
	 * Send disk full error packet, used only when the client sends a read request,
	 * but the client don't have enough space to write the entire file during the
	 * file transfer
	 * 
	 * @param errorMsg
	 * @throws IOException
	 * @throws TFTPErrorException 
	 */
	private void sendDiskFull(String errorMsg, InetAddress address, int port) throws IOException, TFTPErrorException {
		print("Client has sent disk full error packet to server.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createDiskfullErrorPacket(errorMsg, address, port);
		sendPacket(errorPacket);
		throw new TFTPErrorException(errorMsg); // abort the connection
	}
	
	/**
	 * Send illegal TFTP operation error packet
	 * 
	 * @param errorMsg
	 * @throws IOException
	 * @throws TFTPErrorException 
	 */
	private void sendIllegalTFTPOperation(String errorMsg, InetAddress address, int port) throws IOException, TFTPErrorException {
		print("Client has sent illegal TFTP operation packet to server.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createIllegalTFTPOperation(errorMsg, address, port);
		sendPacket(errorPacket);
		throw new TFTPErrorException(errorMsg); // abort the connection
	}
	
	/**
	 * Send unknown tid error packet
	 * 
	 * @param errorMsg
	 * @throws IOException
	 */
	private void sendUnknownTid(String errorMsg, InetAddress address, int port) throws IOException {
		print("Client has sent unknown tid error packet to " + addressToString(address, port) + ".");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createUnknownTID(errorMsg, address, port);
		sendPacket(errorPacket);
	}
	
	/**
	 * re-send the last packet send
	 * @throws IOException 
	 */
	private void resendPacket() throws IOException {
		print("Last packet might be lost, sending last packet again...");
		if (lastPacket == null)
			return;
		sendPacket(lastPacket, true);
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
				if (serverResponsePort == -1)
					serverResponsePort = receivePacket.getPort();
				else if (serverResponsePort != receivePacket.getPort() ||
						!serverAddress.equals(receivePacket.getAddress())) {
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
						print("Client has received one old ack packet, will ignore it...");
					else if (AckPacket.getBlockNumber() > blockNumber) { // received future ack packet, this is invalid
						String errorMsg = "Client has received future ack packet with block number: " + AckPacket.getBlockNumber();
						sendIllegalTFTPOperation(errorMsg, AckPacket.getAddress(), serverResponsePort);
					}
				} else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");
			} catch (IllegalArgumentException e) {
				sendIllegalTFTPOperation(e.getMessage(), receivePacket.getAddress(), serverResponsePort);
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
				if (serverResponsePort == -1)
					serverResponsePort = receivePacket.getPort();
				else if (serverResponsePort != receivePacket.getPort() ||
						!serverAddress.equals(receivePacket.getAddress())) {
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
						print("Client have received one old data packet, sending the ack packet");
						sendPacket(new TFTPAckPacket(DATAPacket.getBlockNumber(), 
								DATAPacket.getAddress(), serverResponsePort));
					} else if (DATAPacket.getBlockNumber() > blockNumber) { // received future data packet, this is invalid
						String errorMsg = "Client has received future data packet with block number: " + DATAPacket.getBlockNumber();
						sendIllegalTFTPOperation(errorMsg, DATAPacket.getAddress(), serverResponsePort);
					}
				} else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");
			} catch (IllegalArgumentException e) {
				sendIllegalTFTPOperation(e.getMessage(), receivePacket.getAddress(), serverResponsePort);
			} catch (SocketTimeoutException e) {
				if (numRetry >= TFTPPacket.MAX_RETRY)
					throw new TFTPErrorException("Connection lost.");
				if (blockNumber == 1) // request packet is lost
					resendPacket();
				++numRetry;
			}
		}
	}
	
	/**
	 * handle RRQ
	 * 
	 * @param filename
	 */
	public void readFileFromServer(String filename) {
		this.serverResponsePort = -1; // clean response port from old file transfer
		String filePath = getFilePath(filename);
		File file = null;
		FileOutputStream fs = null;
		// in case any error happen, this will be set to true,
		// if it's true but the end of the function, the file
		// that is created will be deleted
		boolean shouldDeleteFile = false;
		
		try {
			file = new File(filePath);
			if (file.exists() && !file.canWrite()) { // file already exists and cannot be override
				print("Client don't have permission to write " + filename + ". Please try again.\n");
				return;
			} else if (!file.exists()) { // file not exist, we need to create the file
				if (!file.createNewFile()) // failed to create the file
					throw new IOException("Failed to create " + filename);
			}
			
			fs = new FileOutputStream(file);

			// form the RRQ packet
			TFTPRequestPacket RRQPacket = TFTPRequestPacket.createReadRequest(filename, serverAddress, serverPort);
			sendPacket(RRQPacket, true); // send the RRQ packet
			printInformation("Client have sent the RRQ.", RRQPacket); // print the information

			TFTPAckPacket AckPacket; // used for sending packet
			TFTPDataPacket DATAPacket; // used for receiving packet
			int blockNumber = 1;
			
			// run until we received the last data packet
			do {
				DATAPacket = receiveData(blockNumber);
				
				// if no exception is thrown, then print the information
				printInformation("Client have received the data packet.", DATAPacket);
				
				// check if there is enough space to write the current data packet
				if (file.getFreeSpace() >= DATAPacket.getLength())
					fs.write(DATAPacket.getFileData()); // write to the file
				else { // disk is full
					String errorMsg = "Client don't have enough space to write " + filename + ".";
					sendDiskFull(errorMsg, DATAPacket.getAddress(), serverResponsePort);
				}

				// form the ack packet
				AckPacket = new TFTPAckPacket(blockNumber, DATAPacket.getAddress(), serverResponsePort);				
				sendPacket(AckPacket); // send the ack packet
				
				// print the information in the packet
				printInformation("Client have sent the ack packet.", AckPacket);
				
				++blockNumber; // increment the block number
			} while (!DATAPacket.isLastDataPacket());
		} catch (TFTPErrorException e) { // handle TFTPErrorPacket
			print("TFTP Error: Failed to read " + filename
					+ " from server as client received the following error message:\n"
					+ e.getMessage());
			shouldDeleteFile = true;
		} catch (IOException e) { // handle IOException
			shouldDeleteFile = true;
			print("Client failed to send the request. Please try again.\n");
		} finally { // close the file stream at the end
			try {
				if (fs != null)
					fs.close();
				// if any error happens, we delete the file
				// this is put here since the file must be delete
				// after all related file streams have been closed
				if (shouldDeleteFile)
					file.delete();
			} catch (IOException e) { }
		} // end of try-catch
	} // end of function
	
	/**
	 * handler WRQ
	 * 
	 * @param filename
	 */
	public void writeFileToServer(String filename) {
		this.serverResponsePort = -1; // clean response port from old file transfer
		String filePath = getFilePath(filename);
		File file = null;
		FileInputStream fs = null;
		try {
			file = new File(filePath);
			if (!file.exists()) { // file not exist, notify the user
				print(filename + " not exist in the folder. Please try again\n");
				return;
			}

			if (!file.canRead()) { // file cannot be read, notify the user
				print("Client don't have permission to read " + filename + ". Please try again.\n");
				return;
			}

			fs = new FileInputStream(filePath);

			// form the WRQ packet
			TFTPRequestPacket WRQPacket = TFTPRequestPacket.createWriteRequest(filename, serverAddress, serverPort);
			sendPacket(WRQPacket, true); // send the WRQ packet
			printInformation("Client have sent the WRQ.", WRQPacket);

			byte[] data = new byte[TFTPDataPacket.MAX_DATA_LENGTH];
			int byteUsed = 0;
			int blockNumber = 0;
			
			TFTPDataPacket DATAPacket; // used for sending packet
			TFTPAckPacket AckPacket = null; // for receiving ack packet
			
			// run until we have sent all the information
			do {
				AckPacket = receiveAck(blockNumber);
				printInformation("Client have received the ack packet.", AckPacket);
				++blockNumber;
				byteUsed = fs.read(data);

				// special case when the file length is a multiple of 512,
				// then just send a empty data to indicate that the file has
				// all been transfered
				if (byteUsed == -1) {
					byteUsed = 0;
					data = new byte[0];
				}
				// form the data packet that will be sent to the server
				DATAPacket = new TFTPDataPacket(blockNumber, Arrays.copyOfRange(data, 0, byteUsed),
						byteUsed, AckPacket.getAddress(),serverResponsePort);				
				sendPacket(DATAPacket, true); // send the data packet
				printInformation("Client have sent the data packet.", DATAPacket);
			} while (byteUsed == TFTPDataPacket.MAX_DATA_LENGTH);
			
			receiveAck(blockNumber);
			printInformation("Client have received the ack packet.", AckPacket);
			fs.close();
		} catch (TFTPErrorException e) {
			print("TFTP Error: Failed to write " + filename
					+ " to server as client received the following error message:\n"
					+ e.getMessage());
		} catch (IOException e) {
			print("Client failed to send the request. Please try again.\n");
		} finally { // close the file stream after everything is finished
			try {
				if (fs != null)
					fs.close();
			} catch (IOException e) { }
		} // end of try-catch
	} // end of function

	public static void main(String[] args) throws UnknownHostException {
		try {
			new TFTPClient().waitForCommand();
		} catch (Exception e) {
			System.exit(0);
		}
	}
}
