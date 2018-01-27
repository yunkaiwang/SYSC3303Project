package project;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class TFTPClient {
	DatagramPacket sendPacket, receivePacket;
	DatagramSocket sendReceiveSocket;
	private Mode currentMode; // verbose or quite

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
			 * need to add more printing statements
			 * - packet type(RRQ, WRQ, etc.)
			 * - filename(if applicable)
			 * - mode(if applicable)
			 * - block number(if applicable)
			 * - number of bytes if data(if applicable)
			 * - error code(if applicable)
			 * - error message(if applicable)
			*/ 
			System.out.println("Host: " + packet.getAddress());
			System.out.println("Port: " + packet.getPort());
			System.out.println("Length: " + packet.getLength());
			System.out.println("Containing: " + packet.getData() + "\n");
			return;
		}
	}
	
	public void send() {
		// message to send
		byte[] msg = new byte[100];

		// create the packet that will be sent to the intermediate
		try {
			sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), 23);
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
			default:
				System.out.println("Invalid command, please try again!\n");
			}
		}
	}

	public static void main(String[] args) {
		TFTPClient client = new TFTPClient();
		client.waitForCommand();
	}
}
