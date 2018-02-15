package project;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Scanner;

public class TFTPServer {
	public static final int TFTP_LISTEN_PORT = 6900; // default port
	public static final int MAX_LENGTH = 516; // maximum packet length
	public static final int MIN_LENGTH = 4; // minimum packet length
	private Mode currentMode; // verbose or quite
	private int numThread; // number of threads that are currently going
	private TFTPRequestListener requestListener; // request listener
	// default folder
	private String DEFAULT_FOLDER = System.getProperty("user.dir") + File.separator +
			"server_files" + File.separator;
	private String folder = DEFAULT_FOLDER; // current folder
	
	/**
	 * Constructor
	 */
	TFTPServer() {
		this.currentMode = Mode.QUITE; // default mode is quite
		this.requestListener = new TFTPRequestListener(this, TFTP_LISTEN_PORT);
		this.requestListener.start();
	}

	/**
	 * Increase the thread count
	 */
	synchronized public void incrementNumThread() {
		++numThread;
	}

	/**
	 * Decrease the thread count
	 */
	synchronized public void decrementNumThread() {
		--numThread;
		if (numThread <= 0) { // an error happened
			notifyAll();
		}
	}

	/**
	 * Getter
	 * 
	 * @return numThread
	 */
	synchronized public int getNumThread() {
		return numThread;
	}
	
	/**
	 * Create new request handler thread
	 * 
	 * @param packet
	 * @param address
	 * @param port
	 * @return TFTPRequestHandler
	 */
	public TFTPRequestHandler createNewRequestHandler(DatagramPacket packet, InetAddress address, int port) {
		return new TFTPRequestHandler(this, packet, address, port);
	}
	
	/**
	 * Getter
	 * 
	 * @return currentMode
	 */
	public Mode getMode() {
		return currentMode;
	}
	
	/**
	 * Print the menu
	 */
	private static void printMenu() {
		System.out.println("Available commands:");
		System.out.println("1. menu - show the menu");
		System.out.println("2. exit - stop the client");
		System.out.println("3. mode - show current mode");
		System.out.println("4. switch - switch mode");
		System.out.println("5. count - number of threads that are running");
		System.out.println();
	}
	
	/**
	 * Print information stored in TFTPRequestPacket
	 * 
	 * @param packet
	 * @throws IOException
	 */
	public void printInformation(TFTPRequestPacket packet) throws IOException {
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
	 * Print information stored in TFTPDataPacket
	 * 
	 * @param packet
	 * @throws IOException
	 */
	public void printInformation(TFTPDataPacket packet) throws IOException {
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
	 * Print information stored in TFTPAckPacket
	 * 
	 * @param packet
	 * @throws IOException
	 */
	public void printInformation(TFTPAckPacket packet) throws IOException {
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
	 * Terminate the server
	 */
	private void stopServer() {
		// inform the request listener to refuse any new connection, and wait for all
		// exist threads to finish
		requestListener.stopRequestListener();
		System.out.println("Waiting for all threads to finish...");
		while (getNumThread() > 0) {
			continue;
		}

		System.out.println("Terminating server.");
	}
	
	/**
	 * Switch the printing mode(verbose/quite)
	 */
	private void switchMode() {
		this.currentMode = currentMode.switchMode();
		System.out.println("The mode has been switched to " + this.currentMode + "\n");
	}
	
	/**
	 * Print current printing mode(verbose/quite)
	 */
	private void printMode() {
		System.out.println("Current mode is: " + currentMode.mode());
	}
	
	/**
	 * Print the number of threads that are running
	 */
	private void printCount() {
		System.out.println("Current number of threads is: " + getNumThread());
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
	public String getFilePath(String filename) {
		return getFolder() + filename;
	}
	
	/**
	 * Main loop of this file, wait for new requests
	 */
	private void waitForCommand() {
		Scanner s = new Scanner(System.in);

		printMenu();
		while (true) {
			System.out.print("Command: ");
			String cmdLine = s.nextLine().toLowerCase();
			switch (cmdLine) {
			case "menu":
				printMenu();
				continue;
			case "exit":
				s.close(); // close the scanner
				stopServer();
				return;
			case "mode":
				printMode();
				continue;
			case "switch":
				this.switchMode();
				continue;
			case "count":
				this.printCount();
				continue;
			default:
				System.out.println("Invalid command, please try again!\n");
				System.out.println("These are the available commands:");
				printMenu();
			}
		}
	}

	/**
	 * Check if the given packet data is a read request
	 * 
	 * @param data
	 * @return true is the given packet data is a read request, false otherwise
	 */
	public boolean isReadRequest(byte[] data) {
		return data != null && data[1] == 1; // RRQ should have a OPCODE 1
	}

	/**
	 * Check if the given packet data is a write request
	 * 
	 * @param data
	 * @return true is the given packet data is a write request, false otherwise
	 */
	public boolean isWriteRequest(byte[] data) {
		return data != null && data[1] == 2; // RRQ should have a OPCODE 2
	}
	
	public static void main(String[] args) {
		TFTPServer server = new TFTPServer();
		server.waitForCommand();
	}
}
