package project;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.*;

public class TFTPClient {
	private static final int default_port = 69;
	DatagramSocket socket;
	private Mode currentMode; // verbose or quite
	private InetAddress serverAddress;
	private int serverPort;
	private String folder = System.getProperty("user.dir") + "/client_files/";

	TFTPClient() throws UnknownHostException {
		this(InetAddress.getLocalHost(), default_port);
	}

	TFTPClient(InetAddress server, int port) {
		// default mode is quite
		this.currentMode = Mode.QUITE;
		this.serverAddress = server;
		this.serverPort = port;
	}

	private void printInformation(DatagramPacket packet) {
		switch (this.currentMode) {
		case QUITE:
			return;
		case VERBOSE:
			/*
			 * need to add more printing statements - packet type(RRQ, WRQ, etc.) -
			 * filename(if applicable) - mode(if applicable) - block number(if applicable) -
			 * number of bytes if data(if applicable) - error code(if applicable) - error
			 * message(if applicable)
			 */
			System.out.println("Host: " + packet.getAddress());
			System.out.println("Port: " + packet.getPort());
			System.out.println("Length: " + packet.getLength());
			System.out.println("Containing: " + packet.getData() + "\n");
			return;
		}
	}

	private static void printMenu() {
		System.out.println("Available commands:");
		System.out.println("1. menu - show the menu");
		System.out.println("2. exit - stop the client");
		System.out.println("3. mode - show current mode");
		System.out.println("4. switch - switch print mode(verbose or quite)");
		System.out.println("5. read <filename> - send RRQ(i.e. read text.txt)");
		System.out.println("6. write <filename> - send WRQ(i.e. write text.txt)\n");
	}

	private void printMode() {
		System.out.println("Current mode is: " + currentMode.mode());
	}

	private void switchMode() {
		this.currentMode = currentMode.switchMode();
		System.out.println("The mode has been switched to " + this.currentMode + "\n");
	}

	private void stopClient() {
		System.out.println("Terminating client.");
	}

	private void waitForCommand() {
		Scanner s = new Scanner(System.in);

		printMenu();
		while (true) {
			System.out.print("Command: ");
			String cmdLine = s.nextLine().toLowerCase(); // convert all command into lower case
			String[] commands = cmdLine.split("\\s+"); // split all command into array of command
			if (commands.length == 0) {
				System.out.println("Invalid command, please try again!\n");
				continue;
			}

			switch (commands[0]) {
			case "menu":
				printMenu();
				continue;
			case "exit":
				s.close(); // close the scanner
				stopClient();
				return;
			case "mode":
				printMode();
				continue;
			case "switch":
				switchMode();
				continue;
			case "read":
				if (commands.length != 2)
					System.out.println("Invalid request! Please enter a valid filename for "
							+ "read request(e.g. read text.txt)\n");
				readFileFromServer(commands[1]);
				continue;
			case "write":
				if (commands.length != 2)
					System.out.println("Invalid request! Please enter a valid filename for "
							+ "write request(e.g. write text.txt)\n");
				writeFileToServer(commands[1]);
				continue;
			default:
				System.out.println("Invalid command, please try again!\n");
			}
		}
	}

	private boolean createConnection() {
		try {
			socket = new DatagramSocket();
			return true;
		} catch (SocketException e) {
			return false;
		}
	}
	
	private String getFolder() {
		return folder;
	}
	
	private String getFilePath(String filename) {
		return getFolder() + filename;
	}
	
	private void sendRequest(TFTPRequestPacket packet) throws IOException {
		socket.send(packet.createDatagram(serverAddress, serverPort));
	}
	
	private void sendRequest(TFTPAckPacket packet) throws IOException {
		socket.send(packet.createDatagram(serverAddress, serverPort));
	}
	
	private void sendRequest(TFTPDataPacket packet) throws IOException {
		socket.send(packet.createDatagram(serverAddress, serverPort));
	}
	
	private TFTPDataPacket receiveDataPacket(int blockNumber) {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[TFTPDataPacket.MAX_LENGTH], TFTPDataPacket.MAX_LENGTH);
			socket.receive(receivePacket);
			return TFTPDataPacket.createFromPacket(receivePacket);
		} catch (IOException e) {
			System.out.println("Client failed to receive the response. Please try again.\n");
			return null;
		}
	}
	
	private TFTPAckPacket receiveAckPacket(int blockNumber) {
		try {
			DatagramPacket receivePacket = new DatagramPacket(new byte[TFTPAckPacket.PACKET_LENGTH], TFTPAckPacket.PACKET_LENGTH);
			socket.receive(receivePacket);
			return TFTPAckPacket.createFromPacket(receivePacket);
		} catch (IOException e) {
			System.out.println("Client failed to receive the response. Please try again.\n");
			return null;
		}
	}

	public void readFileFromServer(String filename) { // RRQ
		String filePath = getFilePath(filename);
		File file = null;
		try {
			file = new File(filePath);
			if (file.exists() || !file.canWrite()) {
				System.out.println("Client don't have permission to write " + filename + ". Please try again.\n");
				return;
			}

			FileOutputStream fs = new FileOutputStream(filePath);
			if (!createConnection()) { // socket create failed
				System.out.println("Client failed to create the socket, please check your network status and try again.\n");
				fs.close();
				return;
			}
			
			TFTPRequestPacket RRQPacket = TFTPRequestPacket.createReadRequest(filename);
			sendRequest(RRQPacket);
			
			TFTPDataPacket DATAPacket;
			int blockNumber = 1;
			do {
				DATAPacket = receiveDataPacket(blockNumber);
				fs.write(DATAPacket.getFileData());
				sendRequest(new TFTPAckPacket(blockNumber));
				++blockNumber;
			} while (!DATAPacket.isLastDataPacket());
			fs.close();
		} catch (FileNotFoundException e) {
			file.delete();
			System.out.println("Client failed to write " + filename + ". Please try again.\n");
			return;
		} catch (IOException e) {
			file.delete();
			System.out.println("Client failed to send the request. Please try again.\n");
			return;
		}
	}

	public void writeFileToServer(String filename) { // WRQ
		String filePath = getFilePath(filename);
		File file = null;
		try {
			file = new File(filePath);
			if (!file.exists() || !file.canRead()) {
				System.out.println("Client don't have permission to read " + filename + ". Please try again.\n");
				return;
			}

			FileInputStream fs = new FileInputStream(filePath);
			if (!createConnection()) { // socket create failed
				System.out.println("Client failed to create the socket, please check your network status and try again.\n");
				fs.close();
				return;
			}
			
			TFTPRequestPacket WRQPacket = TFTPRequestPacket.createWriteRequest(filename);
			sendRequest(WRQPacket);
			
			byte[] data = new byte[TFTPDataPacket.MAX_LENGTH];
			int byteUsed = 0;
			TFTPAckPacket AckPacket;
			int blockNumber = 0;
			
			do {
				AckPacket = receiveAckPacket(blockNumber++);
				byteUsed = fs.read(data);
				if (byteUsed == -1) {
					byteUsed = 0;
					data = new byte[0];
				}
				sendRequest(new TFTPDataPacket(blockNumber, data, byteUsed));
			} while (byteUsed ==  TFTPDataPacket.MAX_DATA_LENGTH);
			fs.close();
		} catch (FileNotFoundException e) {
			System.out.println("Client failed to read " + filename + ". Please try again.\n");
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
