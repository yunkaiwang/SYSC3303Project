package project;

import java.io.File;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Scanner;

public class TFTPServer {
	// the port that the server will listen for request on
	public static final int TFTP_LISTEN_PORT = 69;
	public static final int MAX_LENGTH = 516; // maximum packet length
	public static final int MIN_LENGTH = 4;
	private Mode currentMode; // verbose or quite
	private int numThread; // number of threads
	private TFTPRequestListener requestListener;
	private String folder = System.getProperty("user.dir") + File.separator + "server_files" + File.separator;

	TFTPServer() {
		// default mode is quite
		this.currentMode = Mode.QUITE;
		this.requestListener = new TFTPRequestListener(this, TFTP_LISTEN_PORT);
		this.requestListener.start();
	}

	synchronized public void incrementNumThread() {
		++numThread;
	}

	synchronized public void decrementNumThread() {
		--numThread;
		if (numThread <= 0) { // an error happened
			notifyAll();
		}
	}

	synchronized public int getNumThread() {
		return numThread;
	}
	
	public TFTPRequestHandler createNewRequestHandler(DatagramPacket packet, InetAddress address, int port) {
		return new TFTPRequestHandler(this, packet, address, port);
	}
	
	public Mode getMode() { // return current mode
		return currentMode;
	}
	
	private static void printMenu() {
		System.out.println("Available commands:");
		System.out.println("1. menu - show the menu");
		System.out.println("2. exit - stop the client");
		System.out.println("3. mode - show current mode");
		System.out.println("4. switch - switch mode");
		System.out.println("5. count - number of threads that are running");
		System.out.println();
	}
	
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
	
	private void switchMode() {
		this.currentMode = currentMode.switchMode();
		System.out.println("The mode has been switched to " + this.currentMode + "\n");
	}
	
	private void printMode() {
		System.out.println("Current mode is: " + currentMode.mode());
	}
	
	private void printCount() {
		System.out.println("Current number of threads is: " + getNumThread());
	}
	
	private String getFolder() {
		return folder;
	}
	
	public String getFilePath(String filename) {
		return getFolder() + filename;
	}
	
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
				s.close();
				stopServer();
				return;
			case "mode":
				printMode();
				continue;
			case "switch":
				this.switchMode();
				System.out.println("The mode has been switched to " + this.currentMode + "\n");
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

	public static void main(String[] args) {
		TFTPServer server = new TFTPServer();
		server.waitForCommand();
	}

	public boolean isReadRequest(byte[] data) {
		return data != null && data[1] == 1;
	}

	public boolean isWriteRequest(byte[] data) {
		return data != null && data[1] == 2;
	}
}
