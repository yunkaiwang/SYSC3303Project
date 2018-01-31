package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.*;

public class TFTPClient {
	private static final int default_port = 69;
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;
	private Mode currentMode; // verbose or quite
	private String folder = System.getProperty("user.dir") + "/client_files/";
	public static final int MAX_FILE_DATA_LENGTH = 1024;
	
	TFTPClient() {
		// default mode is quite
		this.currentMode = Mode.QUITE;
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

	public void send() {
		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
		// message to send
		byte[] msg = new byte[] { 1, 1, 1, 1 };

		try {
			sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), default_port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// print the information that will be sent
		System.out.println("Client: Sending packet:");
		this.printInformation(sendPacket);

		// send the packet to intermediate
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("Client: Packet sent.\n");

		// start to receive
		byte data[] = new byte[100];
		receivePacket = new DatagramPacket(data, data.length);

		// block until receive a response
		try {
			System.out.println("Client: I'm waiting...");
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// print the information received
		System.out.println("Client: Packet received:");
		this.printInformation(receivePacket);
	}

	private static void printMenu() {
		System.out.println("Available commands:");
		System.out.println("1. help - show the menu");
		System.out.println("2. stop - stop the client");
		System.out.println("3. switch - switch mode(verbose or quite)");
		System.out.println("4. send - send new request(just for testing)");
		System.out.println();
	}

	private void switchMode() {
		switch (this.currentMode) {
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
				System.out.println("Stopping client...Good bye!");
				s.close();
				return;
			case "switch":
				this.switchMode();
				System.out.println("The mode has been switched to " + this.currentMode + "\n");
				continue;
			case "send":
				send();
				continue;
			default:
				System.out.println("Invalid command, please try again!\n");
			}
		}
	}

	public String getFolder() {
		return folder;
	}

	public void getFile(String filename) {
		String path = getFolder() + filename;
		File file = new File(path);

		try {
			byte[] data = new byte[MAX_FILE_DATA_LENGTH];
			int byteCount = 0;
			int byteUsed = 0;
			FileInputStream fileStream = new FileInputStream(file);

			while(byteCount < MAX_FILE_DATA_LENGTH) {
				byteUsed++;
				byteCount = fileStream.read(data);
				if (byteCount == -1) {
					byteCount = 0;
					data = new byte[0];
				}
				byte[] dataToBeSent = generateFileData(byteUsed, data, byteCount);
				prepareDatagramPacket(dataToBeSent);
			}
			
			fileStream.close();
		} catch (FileNotFoundException e) {
			System.out.println("File does not exist.");
			System.out.println("Please choose another file.");
		} catch (IOException e) {
			System.out.println("IOException" + e);
		}
	}
	
	public byte[] generateFileData(int byteUsed, byte[] data, int dataLength) throws IOException{
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(0);
		stream.write(2);
		stream.write(data);
		return stream.toByteArray();
	}

	public DatagramPacket prepareDatagramPacket(byte[] data) {
		try {
			DatagramPacket packet = new DatagramPacket(data, data.length,  InetAddress.getLocalHost(), 6900);
			return packet;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
		
	}

	public static void main(String[] args) {
		TFTPClient client = new TFTPClient();
		client.waitForCommand();
	}
}
