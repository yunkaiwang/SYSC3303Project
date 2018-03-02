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

public class TFTPClient {
	// as required by the project, the client may run in two modes,
	// in test mode, the error simulator program will get involved,
	// in normal mode, the requests will be directly sent to the server
	/**
	 * For iteration 3, when the sent package cannot get the corresponding response in a period of time
	 * it will be considered as a timeout, the client will then resend the previous package 
	 * after several times of timeouts, the send request is considered to be failed
	 * 
	 */
	private enum runningMode {
		test, normal;
	}

	private static final int DEFAULT_SERVER_PORT = 69; // default server port
	private static final int DEFAULT_ERROR_SIMULATOR_PORT = 23; // default error simulator port
	
	private static final String defaultFolder = System.getProperty("user.dir") + File.separator
			+ "client_files" + File.separator; // default folder directory
	
	DatagramSocket socket; // socket used for sending and receiving
	private Mode currentMode; // verbose or quite, in verbose mode, all information will get printed
	private runningMode currentRunningMode; // test or normal, as described above
	
	private InetAddress serverAddress; // server address
	private int serverPort; // server port
	private String folder = defaultFolder; // current folder directory
	
	private DatagramPacket resendPacket; // duplicate of the last packet

	/**
	 * Constructor
	 * 
	 * @throws UnknownHostException
	 */
	TFTPClient() throws UnknownHostException {
		this(InetAddress.getLocalHost(), DEFAULT_SERVER_PORT);
	}

	/**
	 * Constructor
	 * 
	 * @param server
	 * @param port
	 */
	TFTPClient(InetAddress server, int port) {
		this.currentMode = Mode.QUITE; //default mode is quite
		this.currentRunningMode = runningMode.normal; // default mode is normal
		this.serverAddress = server;
		this.serverPort = port;
		
		try { // create the socket
			socket = new DatagramSocket();
			socket.setSoTimeout(TFTPRequestHandler.DEFAULT_TIMEOUT);
		} catch (SocketException e) {
			System.exit(0); // failed to create socket, exit
		}
	}

	/**
	 * Switch the running mode(test/normal), after the mode has been switched, the
	 * server port will be updated accordingly
	 */
	private void switchRunningMode() {
		switch (currentRunningMode) {
		case test:
			currentRunningMode = runningMode.normal;
			serverPort = DEFAULT_SERVER_PORT; // all requests are sent to server
			return;
		case normal:
			currentRunningMode = runningMode.test;
			serverPort = DEFAULT_ERROR_SIMULATOR_PORT; // send requests to error simulator
			return;
		}
	}

	/**
	 * Print information in the packet
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void printInformation(TFTPPacket packet) throws IOException {
		switch (this.currentMode) {
		case QUITE: // don't print detailed information in QUITE mode
			return;
		case VERBOSE:
			print(packet);
			return;
		}
	}

	/**
	 * Helper method for printing message
	 * These print methods are included so that we don't have
	 * to type System.out.println every time, instead, we can
	 * simply use print(), and these three versions of print
	 * functions are included so that the print function can
	 * by used as same as System.out.println.
	 * 
	 * @param msg
	 */
	private static void print(String msg) {
		System.out.println(msg);
	}
	
	/**
	 * Helper method for printing object
	 * 
	 * @param o
	 */
	private static void print(Object o) {
		print(o.toString());
	}
	
	/**
	 * Helper method for printing newline
	 */
	private static void print() {
		print("");
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
		  "  la               - list of files under current directory\n" +
		  "  pwd/dir          - current directory\n" +
		  "  cd <folder>      - change directory, new directoly location should be written using /(i.e. ../new_client/)\n" +
		  "  read <filename>  - send RRQ(i.e. read text.txt)\n" + 
		  "  write <filename> - send WRQ(i.e. write text.txt)\n");
	}

	/**
	 * Print current printing mode(verbose/quite)
	 */
	private void printMode() {
		print("Current mode is: " + currentMode.mode());
	}

	/**
	 * Print current running mode(test/normal)
	 */
	private void printRunningMode() {
		print("Current running mode is: " + currentRunningMode + "\n");
	}

	/**
	 * Switch the printing mode(verbose/quite)
	 */
	private void switchMode() {
		this.currentMode = currentMode.switchMode();
		print("The mode has been switched to " + this.currentMode + "\n");
	}

	/**
	 * Terminate the client
	 */
	private void stopClient() {
		print("Terminating client.");
	}

	/**
	 * Print current directory
	 */
	private void printDirectory() {
		print("Current directory is: " + this.getFolder() + "\n");
	}

	/**
	 * Switch the directory to a new path
	 * 
	 * @param newDirectoryPath
	 */
	private void switchDirectory(String newDirectoryPath) {
		String prevFolder = this.getFolder(); // previous directory path. used for restoring directory path
		String[] directoryPath = newDirectoryPath.split("/");
		for (String dir : directoryPath) {
			if (dir.length() == 0)
				continue;
			else if (dir.equals("..")) {
				if (this.folder.indexOf(File.separator) != -1) {
					this.setFolder(this.folder.substring(0,
							this.folder.substring(0, this.folder.length() - 1).lastIndexOf(File.separator))
							+ File.separator);
				} else {
					print("New directory path is invalid, please try again.\n");
					this.setFolder(prevFolder); // restore directory path
				}
			} else {
				this.setFolder(this.getFolder() + dir + File.separator);
				File path = new File(this.folder);
				if (!path.exists()) {
					print("New directory path is invalid, please try again.\n");
					this.setFolder(prevFolder); // restore directory path
				}
			}
		}
	}

	/**
	 * Print all files under current directory
	 */
	private void printListFiles() {
		File folder = new File(this.getFolder());
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles)
			print(file.getName());
		print();
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
				printListFiles();
				continue;
			case "cd": // change directory
				switchDirectory(commands[1]);
				continue;
			case "mode": // print current mode
				printMode();
				continue;
			case "switch": // switch current print mode
				switchMode();
				continue;
			case "reset": // switch current test mode
				switchRunningMode();
				printRunningMode();
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
	 * Setter
	 * 
	 * @return folder
	 */
	private void setFolder(String newFolder) {
		this.folder = newFolder;
	}

	/**
	 * Getter
	 * 
	 * @return folder
	 */
	private String getFolder() {
		return folder;
	}

	/**
	 * Create the file path by combining the folder location and filename
	 * 
	 * @param filename
	 * @return filePath
	 */
	private String getFilePath(String filename) {
		return getFolder() + filename;
	}

	/**
	 * Send packet
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void sendPacket(DatagramPacket packet) throws IOException {
		resendPacket = packet; // get the previous packet
		socket.send(packet);
	}

	/**
	 * Receive datagram packet
	 * 
	 * @return datagramPacket
	 * @throws IOException
	 */
	private DatagramPacket receivePacket() throws IOException {
		int timeouts = 0;
		while (timeouts < TFTPRequestHandler.MAX_SEND_TIMES) {
			try {
				DatagramPacket receivePacket = new DatagramPacket(new byte[TFTPPacket.MAX_LENGTH],
						TFTPPacket.MAX_LENGTH);
				socket.receive(receivePacket);
				return receivePacket;
			} catch (SocketTimeoutException e) {
				if (++timeouts >= TFTPRequestHandler.MAX_SEND_TIMES) 
					throw new IOException("Connection timed out. ");
			}
			ThreadLog.print("Receive timed out " + timeouts + " times. Try it again. ");
			sendPacket(this.resendPacket);
			continue;
		}
		return null;
	}

	/**
	 * Send disk full error packet, used only when the client sends a read request,
	 * but the client don't have enough space to write the entire file during the
	 * file transfer
	 * 
	 * @param errorMsg
	 * @throws IOException
	 */
	private void sendDiskFull(String errorMsg, InetAddress address, int port) throws IOException {
		print("Client has sent disk full error packet to server.");
		TFTPErrorPacket errorPacket = TFTPErrorPacket.createDiskfullErrorPacket(errorMsg, address, port);
		sendPacket(errorPacket.createDatagramPacket());
	}
	
	/**
	 * handle RRQ
	 * 
	 * @param filename
	 */
	public void readFileFromServer(String filename) {
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

			// send the RRQ packet
			sendPacket(RRQPacket.createDatagramPacket());
			
			// print the information
			print("Client have sent the RRQ.");
			printInformation(RRQPacket);

			TFTPPacket packet; // used for receiving packet
			TFTPDataPacket DATAPacket; // used for receiving packet
			int blockNumber = 1; // the first packet that received should have block number 1

			// run until we received the last data packet
			do {
				// receive the data packet and create TFTPPacket from it
				packet = TFTPPacket.createFromPacket(receivePacket());

				// if received packet is not TFTPDataPacket, raise an exception
				if (packet instanceof TFTPDataPacket)
					DATAPacket = (TFTPDataPacket) packet;
				else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");

				// if no exception is thrown, then print the information
				print("Client have received the data packet.");
				printInformation(DATAPacket);

				// get free space in disk
				long freeSpace = file.getFreeSpace();
				
				// check if there is enough space to write the current data packet
				if (freeSpace >= DATAPacket.getLength())
					fs.write(DATAPacket.getFileData()); // write to the file
				else { // disk is full
					String errorMsg = "Client don't have enough space to write " + filename + ".";
					sendDiskFull(errorMsg, DATAPacket.getAddress(), DATAPacket.getPort());
					throw new TFTPErrorException(errorMsg); // abort the connection
				}

				// form the ack packet
				TFTPAckPacket AckPacket = new TFTPAckPacket(blockNumber, DATAPacket.getAddress(), DATAPacket.getPort());

				// send the ack packet
				sendPacket(AckPacket.createDatagramPacket());
				
				// print the information in the packet
				print("Client have sent the ack packet.");
				printInformation(AckPacket);
				
				// increment the block number
				++blockNumber;
			} while (!DATAPacket.isLastDataPacket());
		} catch (TFTPErrorException e) { // handle TFTPErrorPacket
			print("TFTP Error: Failed to read " + filename
					+ " from server as client received the following error message:");
			print(e.getMessage());
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

			// send the WRQ packet
			sendPacket(WRQPacket.createDatagramPacket());
			print("Client have sent the WRQ.");
			printInformation(WRQPacket);

			byte[] data = new byte[TFTPDataPacket.MAX_DATA_LENGTH];
			int byteUsed = 0;
			int blockNumber = 0; // the first packet received should have block number 0

			TFTPAckPacket AckPacket = null; // for receiving ack packet
			// run until we have sent all the information
			do {
				// receive the datagram packet
				TFTPPacket packet = TFTPPacket.createFromPacket(receivePacket());

				if (packet instanceof TFTPAckPacket)
					AckPacket = (TFTPAckPacket) packet;
				else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");

				print("Client have received the ack packet.");
				printInformation(AckPacket);
				byteUsed = fs.read(data);

				// special case when the file length is a multiple of 512,
				// then just send a empty data to indicate that the file has
				// all been transfered
				if (byteUsed == -1) {
					byteUsed = 0;
					data = new byte[0];
				}

				// form the data packet that will be sent to the server
				TFTPDataPacket DATAPacket = new TFTPDataPacket(blockNumber, Arrays.copyOfRange(data, 0, byteUsed),
						byteUsed, AckPacket.getAddress(), AckPacket.getPort());

				// send the data packet
				sendPacket(DATAPacket.createDatagramPacket());
				print("Client have sent the data packet.");
				printInformation(DATAPacket);
			} while (byteUsed == TFTPDataPacket.MAX_DATA_LENGTH);

			// receive the last ack packet from the server
			TFTPPacket packet = TFTPPacket.createFromPacket(receivePacket());

			if (packet instanceof TFTPAckPacket)
				AckPacket = (TFTPAckPacket) packet;
			else if (packet instanceof TFTPErrorPacket)
				throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
			else
				throw new TFTPErrorException("Unknown packet received.");

			print("Client have received the ack packet.");
			fs.close();
		} catch (TFTPErrorException e) {
			print("TFTP Error: Failed to write " + filename
					+ " to server as client received the following error message:");
			print(e.getMessage());
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
		TFTPClient client = new TFTPClient();
		client.waitForCommand();
	}
}  
