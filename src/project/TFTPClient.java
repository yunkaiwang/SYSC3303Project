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
 * For iteration 3, when the sent package cannot get the corresponding response in a period of time
 * it will be considered as a timeout, the client will then resend the previous package 
 * after several times of timeouts, the send request is considered to be failed
 * 
 * @author lairu wu Last modified on Feb 25, 2018
 */

public class TFTPClient {
	// as required by the project, the client may run in two modes,
	// in test mode, the error simulator program will get involved,
	// in normal mode, the requests will be directly sent to the server
	private enum runningMode {
		test, normal;
	}

	private static final int DEFAULT_SERVER_PORT = 69; // default server port
	private static final int DEFAULT_ERROR_SIMULATOR_PORT = 23; // default error simulator port
	// default folder directory
	private static final String defaultFolder = System.getProperty("user.dir") + File.separator + "client_files"
			+ File.separator;
	DatagramSocket socket;
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
		// default mode is quite
		this.currentMode = Mode.QUITE;
		this.currentRunningMode = runningMode.normal;
		this.serverAddress = server;
		this.serverPort = port;
	}

	/**
	 * Switch the running mode(test/normal), after the mode has been switched, the
	 * server port will be updated accordingly
	 */
	private void switchRunningMode() {
		switch (currentRunningMode) {
		case test:
			currentRunningMode = runningMode.normal;
			serverPort = DEFAULT_SERVER_PORT;
			return;
		case normal:
			currentRunningMode = runningMode.test;
			serverPort = DEFAULT_ERROR_SIMULATOR_PORT;
			return;
		}
	}

	/**
	 * Print information in the packet
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void printInformation(TFTPRequestPacket packet) throws IOException {
		switch (this.currentMode) {
		case QUITE: // don't print detailed information in QUITE mode
			return;
		case VERBOSE:
			System.out.println("Packet type: " + packet.type());
			System.out.println("Destination: ");
			System.out.println("IP address: " + packet.getAddress());
			System.out.println("Port: " + packet.getPort());
			System.out.println("Information in this packet: ");
			System.out.println("Filename: " + packet.getFilename());
			System.out.println("Mode: " + packet.getMode() + "\n");
			return;
		}
	}

	/**
	 * Print information in the packet
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void printInformation(TFTPDataPacket packet) throws IOException {
		switch (this.currentMode) {
		case QUITE: // don't print detailed information in QUITE mode
			return;
		case VERBOSE:
			System.out.println("Packet type: " + packet.type());
			System.out.println("Destination: ");
			System.out.println("IP address: " + packet.getAddress());
			System.out.println("Port: " + packet.getPort());
			System.out.println("Information in this packet: ");
			System.out.println("Block number: " + packet.getBlockNumber());
			System.out.println("Data length: " + packet.getLength() + "\n");
			return;
		}
	}

	/**
	 * Print information in the packet
	 * 
	 * @param packet
	 * @throws IOException
	 */
	private void printInformation(TFTPAckPacket packet) throws IOException {
		switch (this.currentMode) {
		case QUITE: // don't print detailed information in QUITE mode
			return;
		case VERBOSE:
			System.out.println("Packet type: " + packet.type());
			System.out.println("Destination: ");
			System.out.println("IP address: " + packet.getAddress());
			System.out.println("Port: " + packet.getPort());
			System.out.println("Information in this packet: ");
			System.out.println("Block number: " + packet.getBlockNumber() + "\n");
			return;
		}
	}

	/**
	 * Print the menu
	 */
	private static void printMenu() {
		System.out.println("Available commands:");
		System.out.println("  menu             - show the menu");
		System.out.println("  exit             - stop the client");
		System.out.println("  mode             - show current mode");
		System.out.println("  switch           - switch print mode(verbose or quite)");
		System.out.println("  reset            - reset running mode(test or normal)");
		System.out.println("  la/ls            - list of files under current directory");
		System.out.println("  pwd/dir          - current directory");
		System.out.println(
				"  cd <folder>      - change directory, new directoly location should be written using /(i.e. ../new_client/");
		System.out.println("  read <filename>  - send RRQ(i.e. read text.txt)");
		System.out.println("  write <filename> - send WRQ(i.e. write text.txt)\n");
	}

	/**
	 * Print current printing mode(verbose/quite)
	 */
	private void printMode() {
		System.out.println("Current mode is: " + currentMode.mode());
	}

	/**
	 * Print current running mode(test/normal)
	 */
	private void printRunningMode() {
		System.out.println("Current running mode is: " + currentRunningMode + "\n");
	}

	/**
	 * Switch the printing mode(verbose/quite)
	 */
	private void switchMode() {
		this.currentMode = currentMode.switchMode();
		System.out.println("The mode has been switched to " + this.currentMode + "\n");
	}

	/**
	 * Terminate the client
	 */
	private void stopClient() {
		System.out.println("Terminating client.");
	}

	/**
	 * Print current directory
	 */
	private void printDirectory() {
		System.out.println("Current directory is: " + this.getFolder() + "\n");
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
					System.out.println("New directory path is invalid, please try again.\n");
					this.setFolder(prevFolder); // restore directory path
				}
			} else {
				this.setFolder(this.getFolder() + dir + File.separator);
				File path = new File(this.folder);
				if (!path.exists()) {
					System.out.println("New directory path is invalid, please try again.\n");
					this.setFolder(prevFolder); // restore directory path
				}
			}
		}
	}

	private void printListFiles() {
		File folder = new File(this.getFolder());
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles)
			System.out.println(file.getName());
		System.out.println();
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
			if (commands.length == 0)
				continue;

			switch (commands[0].toLowerCase()) {
			case "menu":
				printMenu();
				continue;
			case "exit":
				s.close(); // close the scanner
				stopClient();
				return;
			case "pwd":
			case "dir":
				printDirectory();
				continue;
			case "la":
			case "ls":
				printListFiles();
				continue;
			case "cd":
				switchDirectory(commands[1]);
				continue;
			case "mode":
				printMode();
				continue;
			case "switch":
				switchMode();
				continue;
			case "reset":
				switchRunningMode();
				printRunningMode();
				continue;
			case "read":
				if (commands.length != 2)
					System.out.println("Invalid request! Please enter a valid filename for "
							+ "read request(e.g. read text.txt)\n");
				else
					readFileFromServer(commands[1]);
				continue;
			case "write":
				if (commands.length != 2)
					System.out.println("Invalid request! Please enter a valid filename for "
							+ "write request(e.g. write text.txt)\n");
				else
					writeFileToServer(commands[1]);
				continue;
			default:
				System.out.println("Invalid command, please try again!\n");
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
	 * receive packet
	 * resend previous packet if time out
	 * 
	 * @param blockNumber
	 * @return TFTPDataPacket
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
	 * handle RRQ
	 * 
	 * @param filename
	 */
	public void readFileFromServer(String filename) {
		String filePath = getFilePath(filename);
		File file = null;
		FileOutputStream fs = null;
		try {
			file = new File(filePath);
			if (file.exists() && !file.canWrite()) { // file already exists and cannot be override
				System.out.println("Client don't have permission to write " + filename + ". Please try again.\n");
				return;
			} else if (!file.exists()) { // create the file
				if (!file.createNewFile())
					throw new IOException("Failed to create " + filename);
			}

			socket = new DatagramSocket();
			fs = new FileOutputStream(file);
			socket.setSoTimeout(TFTPRequestHandler.DEFAULT_TIMEOUT);

			// form the RRQ packet
			TFTPRequestPacket RRQPacket = TFTPRequestPacket.createReadRequest(filename, serverAddress, serverPort);

			// send the RRQ packet
			sendPacket(RRQPacket.createDatagramPacket());
			System.out.println("Client have sent the RRQ.");
			printInformation(RRQPacket);
			
			TFTPDataPacket DATAPacket;
			int blockNumber = 1; // the first packet that received should have block number 1

			// run until we received the last data packet
			do {
				// receive the data packet
				TFTPPacket packet = TFTPPacket.createFromPacket(receivePacket());
				
				if (packet instanceof TFTPDataPacket)
					DATAPacket = (TFTPDataPacket) packet;
				else if (packet instanceof TFTPErrorPacket)
					throw new TFTPErrorException(((TFTPErrorPacket) packet).getErrorMsg());
				else
					throw new TFTPErrorException("Unknown packet received.");
				
				System.out.println("Client have received the data packet.");
				printInformation(DATAPacket);

				// write to the file
				fs.write(DATAPacket.getFileData());

				// form the ack packet
				TFTPAckPacket AckPacket = new TFTPAckPacket(blockNumber, DATAPacket.getAddress(), DATAPacket.getPort());

				// send the ack packet
				sendPacket(AckPacket.createDatagramPacket());
				System.out.println("Client have sent the ack packet.");
				printInformation(AckPacket);
				++blockNumber;
			} while (!DATAPacket.isLastDataPacket());
			fs.close();
		} catch (TFTPErrorException e) {
			System.out.println("TFTP Error: Failed to read " + filename + " from server as client received the following error message:");
			System.out.println(e.getMessage());
			try {
				fs.close();
			} catch (IOException e1) {
			}
			return;
		} catch (SocketException e) {
			System.out.println("Client failed to create the socket, please check your network status and try again.\n");
			return;
		} catch (IOException e) {
			file.delete();
			System.out.println("Client failed to send the request. Please try again.\n");
			return;
		}
	}

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
				System.out.println(filename + " not exist in the folder. Please try again\n");
				return;
			}

			if (!file.canRead()) { // file cannot be read, notify the user
				System.out.println("Client don't have permission to read " + filename + ". Please try again.\n");
				return;
			}

			socket = new DatagramSocket();
			fs = new FileInputStream(filePath);
			socket.setSoTimeout(TFTPRequestHandler.DEFAULT_TIMEOUT);

			// form the WRQ packet
			TFTPRequestPacket WRQPacket = TFTPRequestPacket.createWriteRequest(filename, serverAddress, serverPort);

			// send the WRQ packet
			sendPacket(WRQPacket.createDatagramPacket());
			System.out.println("Client have sent the WRQ.");
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
				
				System.out.println("Client have received the ack packet.");
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
				System.out.println("Client have sent the data packet.");
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
			
			System.out.println("Client have received the ack packet.");
			fs.close();
		} catch (TFTPErrorException e) {
			System.out.println("TFTP Error: Failed to write " + filename + " to server as client received the following error message:");
			System.out.println(e.getMessage());
			try {
				fs.close();
			} catch (IOException e1) {
			}
			return;
		} catch (SocketException e) {
			System.out.println("Client failed to create the socket, please check your network status and try again.\n");
			return;
		} catch (IOException e) {
			System.out.println("Client failed to send the request. Please try again.\n");
			return;
		}
	}

	public static void main(String[] args) throws UnknownHostException {
		TFTPClient client = new TFTPClient();
		client.waitForCommand();
	}
}
