package project;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Scanner;

public class TFTPServer {
	public static final int TFTP_LISTEN_PORT = 69; // default port
	private Mode currentMode; // verbose or quite
	private int numThread; // number of threads that are currently going
	private TFTPRequestListener requestListener; // request listener
	private String DEFAULT_FOLDER = System.getProperty("user.dir") +
			File.separator + "server_files" + File.separator; // default folder location
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
		System.out.println("Available commands:\n"
	            + "  menu        - show the menu\n"
				+ "  exit        - stop the client\n"
	            + "  mode        - show current mode\n"
				+ "  switch      - switch mode\n"
	            + "  count       - number of threads that are running\n"
				+ "  dir/pwd     - current directory\n"
	            + "  la          - list of files under current directory\n"
				+ "  cd <folder> - change directory, new directoly location should be written using /(i.e. ../new_client/)\n");
	}

	/**
	 * Print information stored in TFTPPacket
	 * 
	 * @param packet
	 * @throws IOException
	 */
	public void printInformation(TFTPPacket packet) throws IOException {
		switch (this.currentMode) {
		case QUITE: // don't print detailed information in QUITE mode
			return;
		case VERBOSE: // print detailed information in VERBOSE mode
			System.out.println(packet);
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

	/**
	 * Setter
	 * 
	 * @return folder
	 */
	private void setFolder(String newFolder) {
		this.folder = newFolder;
	}

	/**
	 * Print the list of files under current folder
	 */
	private void printListFiles() {
		File folder = new File(this.getFolder());
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles)
			System.out.println(file.getName());
		System.out.println();
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
	 * Print current directory
	 */
	private void printDirectory() {
		System.out.println("Current directory is: " + this.getFolder() + "\n");
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
				printListFiles();
				continue;
			case "cd":
				switchDirectory(commands[1]);
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
		return data != null && data[0] == 0 && data[1] == 1; // RRQ should have a OPCODE 1
	}

	/**
	 * Check if the given packet data is a write request
	 * 
	 * @param data
	 * @return true is the given packet data is a write request, false otherwise
	 */
	public boolean isWriteRequest(byte[] data) {
		return data != null && data[0] == 0 &&data[1] == 2; // RRQ should have a OPCODE 2
	}

	public static void main(String[] args) {
		TFTPServer server = new TFTPServer();
		server.waitForCommand();
	}
}
