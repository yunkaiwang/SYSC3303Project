package project;

import java.io.File;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Scanner;

/**
 * TFTPServer
 * This program only waits for user commands, all the TFTP file transfer
 * and request listening are done with other threads.
 * 
 * @author yunkai wang
 *
 */
public class TFTPServer extends TFTPHost {
	public static final int TFTP_LISTEN_PORT = 69; // default port
	private int numThread; // number of threads that are currently going
	private TFTPRequestListener requestListener; // request listener
	private static final String DEFAULT_FOLDER = System.getProperty("user.dir") +
			File.separator + "server_files" + File.separator; // default folder location

	/**
	 * Constructor
	 */
	TFTPServer() {
		super(DEFAULT_FOLDER, Mode.QUITE); // default mode is quite
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
	 * Print the menu
	 */
	private static void printMenu() {
		System.out.println("Available commands:\n"
	            + "  menu             - show the menu\n"
				+ "  exit             - stop the client\n"
	            + "  mode             - show current mode\n"
				+ "  switch           - switch mode\n"
	            + "  count            - number of threads that are running\n"
				+ "  dir/pwd          - current directory\n"
	            + "  la/ls            - list of files under current directory\n"
	  		    + "  rm <filename>    - remove existing file\n"
				+ "  cd <folder>      - change directory, new directoly location should be written using /(i.e. ../new_client/)\n"
		        + "  touch <fn>       - create a new empty file for testing with the given file name(i.e. touch random.txt)\n"
		        + "  touch <fn> <size>- create a new file for testing with the given file name and the given size(i.e. touch random.txt 512)\n");
	}

	/**
	 * Terminate the server
	 */
	private void stopServer() {
		// inform the request listener to refuse any new connection, and wait for all
		// exist threads to finish
		requestListener.stopRequestListener();
		System.out.println("Waiting for all threads to finish...");
		while (getNumThread() > 0)
			continue;

		System.out.println("Terminating server.");
	}

	/**
	 * Print the number of threads that are running
	 */
	private void printCount() {
		System.out.println("Current number of threads is: " + getNumThread());
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
			case "rm":
				this.removeFile(commands[1]);
				continue;
			case "touch": // create file
				if (commands.length != 2 && commands.length != 3)
					System.out.println("Please enter a valid file name and file size(e.g. touch random.txt 512)");
				else {
					try {
						String fileName = commands[1];
						int fileSize = commands.length == 3 ? Integer.parseInt(commands[2]) : 0;
						createFile(fileName, fileSize);
					} catch (Exception e) { // parse failed, given file size is invalid
						System.out.println("Please enter a valid file name and file size(e.g. touch random.txt 512)");
					}
				}
				continue;
			default:
				System.out.println("Invalid command, please try again!\n");
				System.out.println("These are the available commands:");
				printMenu();
			}
		}
	}
	
	/**
	 * Check if the given packet data is a TFTP request packet
	 * 
	 * @param data
	 * @return true is the given packet data is a TFTP request packet, false otherwise
	 */
	public boolean isRequestPacket(byte[] data) {
		// request packet should has OPCODE 1 or 2
		return data != null && data[0] == 0 && (data[1] == 1 || data[1] == 2);
	}
	
	/**
	 * Check if the given packet data is a TFTP request packet
	 * 
	 * @param data
	 * @return true is the given packet data is a TFTP request packet, false otherwise
	 */
	public boolean isWriteRequest(byte[] data) {
		// request packet should has OPCODE 1 or 2
		return data != null && data[0] == 0 && data[1] == 2;
	}

	public static void main(String[] args) {
		new TFTPServer().waitForCommand();
	}
}
