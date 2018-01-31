package project;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Scanner;

public class TFTPServer {
	// the port that the server will listen for request on
	private static final int TFTP_LISTEN_PORT = 69;
	private Mode currentMode; // verbose or quite
	private int numThread; // number of threads
	private TFTPRequestListener requestListener;

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
		System.out.println("1. help - show the menu");
		System.out.println("2. stop - stop the client");
		System.out.println("3. switch - switch mode");
		System.out.println();
	}
	
	private void stopServer() {
		// inform the request listener to refuse any new connection, and wait for all
		// exist threads to finish
		requestListener.refuseNewConnection();
		System.out.println("Waiting for all threads to finish...");
		while (getNumThread() > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Stoping process is interrupted, failed to stop server properly.");
				System.exit(1);
			}
		}

		System.out.println("Stopping server...Good bye!");
		System.exit(0);
	}
	
	private void switchMode() {
		switch(this.currentMode) {
		case QUITE:
			this.currentMode = Mode.VERBOSE;
			return;
		case VERBOSE:
			this.currentMode = Mode.QUITE;
			return;
		}
	}
	
	private void waitForCommand() {
		Scanner s = new Scanner(System.in);

		printMenu();
		while (true) {
			System.out.print("Command: ");
			String cmdLine = s.nextLine().toLowerCase();
			switch (cmdLine) {
			case "help":
				printMenu();
				continue;
			case "stop":
				s.close();
				this.stopServer();
			case "switch":
				this.switchMode();
				System.out.println("The mode has been switched to " + this.currentMode + "\n");
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
}
