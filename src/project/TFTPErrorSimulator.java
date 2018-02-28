package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * For iteration 2, the error simulator will not touch any of the packets
 * received, it will just forward all those packets.
 * 
 * @author yunkai wang Last modified on Feb 15, 2018
 */
public class TFTPErrorSimulator {
	
	// The error that will be simulated
	private enum ErrorType {
		lose("Lose a packet"),
		delay("Delay a packet"),
		duplicate("Duplicate a packet");
		
		String representation;
		
		ErrorType(String repr) {
			this.representation = repr;
		}
		
		@Override
		public String toString() {
			return this.representation;
		}

		public static ErrorType getFromKeywork(String commands) {
			if (commands.equalsIgnoreCase("lose"))
				return ErrorType.lose;
			else if (commands.equalsIgnoreCase("delay"))
				return ErrorType.delay;
			else if (commands.equalsIgnoreCase("duplicate"))
				return ErrorType.duplicate;
			else
				return null;
		}
	}
	
	// the packet that we will simulate the error
	private enum PacketType {
		request("TFTP request packet (WRQ or RRQ)"),
		data("TFTP data packet"),
		ack("TFTP ack packet");
		
		String representation;
		
		PacketType(String repr) {
			this.representation = repr;
		}
		
		@Override
		public String toString() {
			return this.representation;
		}
	}
	
	public static final int TFTP_LISTEN_PORT = 23; // default error simulator port
	private DatagramSocket receiveSocket, sendReceiveSocket;
	private ErrorType errorType; // current simulating error
	private PacketType packetType; // current simulating tftp error packet
	private int packetCount; // the number of packet to raise an error
	private long delayTime; // number of seconds to delay
	
	/**
	 * Constructor
	 */
	public TFTPErrorSimulator() {
		try {
			receiveSocket = new DatagramSocket(TFTP_LISTEN_PORT);
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Handle read request
	 * 
	 * @param packet
	 * @throws IOException
	 * @throws TFTPErrorException
	 */
	private void handleRRQ(TFTPRequestPacket packet) throws IOException, TFTPErrorException {
		// form the RRQ packet
		DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(), packet.getAddress(),
				TFTPServer.TFTP_LISTEN_PORT);
		// send the RRQ packet
		sendReceiveSocket.send(sendPacket);
		System.out.println("Error simulator has forward the packet to the server.");
		byte data[];

		TFTPPacket tftppacket;
		TFTPDataPacket dataPacket = null;
		do {
			data = new byte[TFTPPacket.MAX_LENGTH]; // clean old byte

			// form the packet for receiving
			DatagramPacket receivePacket = new DatagramPacket(data, data.length);

			// receive data packet from server
			sendReceiveSocket.receive(receivePacket);

			int serverResponsePort = receivePacket.getPort();
			System.out.println("Error simulator has received packet from server.");

			// form the data packet that will be sent to the client
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
					packet.getPort());

			// send the data packet
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has forward the packet to the client.");

			// check if error packet is received from the server, if so, abort the
			// connection
			// expect TFTPDataPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPDataPacket)) {
				System.out.println("Error simulator has received error packet from the server.");
				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
						: "Unknown packet received.";

				// terminate the connection
				throw new TFTPErrorException(errorMsg);
			}

			dataPacket = (TFTPDataPacket) tftppacket;
			data = new byte[TFTPPacket.MAX_LENGTH]; // clean old byte

			// prepare packet for receiving
			receivePacket = new DatagramPacket(data, data.length);

			// receive the packet from client
			sendReceiveSocket.receive(receivePacket);

			System.out.println("Error simulator has received the packet from the client.");
			// prepare the packet for sending to the server
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
					serverResponsePort);

			// send the packet to the server
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has sent the packet to the server.");

			// check if error packet is received from the client, if so, terminate the
			// connection
			// expect TFTPAckPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPAckPacket)) {
				System.out.println("Error simulator has received error packet from the client.");
				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
						: "Unknown packet received.";

				throw new TFTPErrorException(errorMsg);
			}
		} while (!dataPacket.isLastDataPacket());
	}

	/**
	 * Handle write request
	 * 
	 * @param packet
	 * @throws TFTPErrorException
	 * @throws IOException
	 */
	private void handleWRQ(TFTPRequestPacket packet) throws IOException, TFTPErrorException {
		// form the WRQ packet
		DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(), packet.getAddress(),
				TFTPServer.TFTP_LISTEN_PORT);
		// send the WRQ packet
		sendReceiveSocket.send(sendPacket);
		System.out.println("Error simulator has forward the packet to the server.");

		byte data[] = new byte[TFTPPacket.MAX_LENGTH];

		// prepare the packet for receiving
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);

		// receive the packet
		sendReceiveSocket.receive(receivePacket);

		// remember the server port (the handler's port, not 69)
		int serverResponsePort = receivePacket.getPort();
		System.out.println("Error simulator has received packet from server.");

		TFTPPacket tftppacket;
		TFTPDataPacket dataPacket;

		do {
			// prepare the packet to send
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
					packet.getPort());

			// send the packet
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has forward the packet to the client.");

			// check if error packet is received from server, if so terminate the connection
			// expect TFTPAckPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPAckPacket)) {
				System.out.println("Error simulator has received error packet from the server.");
				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
						: "Unknown packet received.";

				// terminates the connection
				throw new TFTPErrorException(errorMsg);
			}

			data = new byte[TFTPPacket.MAX_LENGTH]; // clean old byte

			// prepare the packet for receiving
			receivePacket = new DatagramPacket(data, data.length);
			// receive the packet from client
			sendReceiveSocket.receive(receivePacket);

			System.out.println("Error simulator has received the packet from the client.");

			// prepare the packet for sending
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
					serverResponsePort);

			// send the packet to the server
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has sent the packet to the server.");

			// check if error packet is received from client, if so terminate the connection
			// expect TFTPDataPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPDataPacket)) {
				System.out.println("Error simulator has received error packet from the client.");
				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
						: "Unknown packet received.";

				// terminates the connection
				throw new TFTPErrorException(errorMsg);
			}

			dataPacket = (TFTPDataPacket) tftppacket;
			data = new byte[TFTPPacket.MAX_LENGTH]; // clean old byte

			// prepare the packet for receiving
			receivePacket = new DatagramPacket(data, data.length);

			// receive the packet from server
			sendReceiveSocket.receive(receivePacket);

			// check if error packet is received from server, if so terminate the connection
			// expect TFTPAckPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPAckPacket)) {
				System.out.println("Error simulator has received error packet from the server.");

				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
						: "Unknown packet received.";

				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
						packet.getPort());
				sendReceiveSocket.send(sendPacket);
				System.out.println("Error simulator has forward the error packet to the client.");

				// terminates the connection
				throw new TFTPErrorException(errorMsg);
			}

		} while (!dataPacket.isLastDataPacket());
		sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
				packet.getPort());
		sendReceiveSocket.send(sendPacket);
		System.out.println("Error simulator has forward the packet to the client.");
	}

	public void printError() {
		System.out.println("This is the type of error that will be simulated:\n"
				+ "Error Type: " + this.errorType.toString() + "\n"
				+ "Error Packet: " + this.packetType.toString() + "\n"
				+ (this.packetType == PacketType.request ? "" : // print count only if it's not simulating an error on request packet
				  "Count: " + this.packetCount + "\n"));
	}
	
	/**
	 * Wait for a new request
	 */
	public void waitForRequest() {
		byte data[] = new byte[TFTPPacket.MAX_LENGTH];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		System.out.println("Error simulator is waiting for new requests.");

		try {
			// received a new request
			receiveSocket.receive(packet);
			System.out.println("Error simulator received new requests.");
			// create the request packet from the packet received from the client
			TFTPRequestPacket requestPacket = TFTPRequestPacket.createFromPacket(packet);

			if (this.packetType == PacketType.request) {
				if (this.errorType == ErrorType.lose) { // lose request packet
					System.out.println("Error simulator 'lost' the request packet\n");
					return;
				} else if (this.errorType == ErrorType.delay) { // delay request packet
					try {
						Thread.sleep(this.delayTime);
					} catch (InterruptedException e) {
						System.exit(1); // got interrupted exception, so exit...
					}
				}
			}
			if (requestPacket.isReadRequest()) {
				handleRRQ(requestPacket);
			} else if (requestPacket.isWriteRequest()) {
				handleWRQ(requestPacket);
			} else {
				System.out.println("Error simulator have received an invalid request.");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (TFTPErrorException e) {
			System.out.println("Connection is aborted as the following error message is received:\n" + e.getMessage());
		}
	}

	/**
	 * Print the menu
	 */
	private static void printMenu() {
		System.out.println("Available commands:\n"
	         + "  menu         - display the menu\n"
			 + "  normal       - normal operation\n"
			 + "  lose         - lose a packet\n"
			 + "  delay <time> - delay a packet for specific time where time is given as second(e.g., delay 2.5)\n"
			 + "  duplicate    - duplicate a packet\n"
			 + "  exit         - exit the error simulator\n");
	}

	/**
	 * Main loop of this file, the user can choose whether starts a normal request or simulating an error, after
	 * the user made the choice, the error simulator will start to listen for new request
	 */
	private void waitForCommand() {
		Scanner s = new Scanner(System.in);

		printMenu();
		while (true) {
			System.out.print("Command: ");
			String[] commands = s.nextLine().split(" ");

			if (commands[0].equalsIgnoreCase("menu")) {
				printMenu();
				continue;
			} else if (commands[0].equalsIgnoreCase("normal")) {
				this.waitForRequest();
				continue;
			} else if (commands[0].equalsIgnoreCase("lose") ||
					   commands[0].equalsIgnoreCase("delay") ||
					   commands[0].equalsIgnoreCase("duplicate")) {
				this.errorType = ErrorType.getFromKeywork(commands[0]);
				if (commands[0].equalsIgnoreCase("delay")) {
					try {
						this.delayTime = Long.parseLong(commands[1]);
					} catch (Exception e) {
						continue; // parse int failed, need to try again
					}
				}
				
				// this while loop chooces the type of packet to simulate the error
				while (true) {
					System.out.println("Plase select the type pf packet you want to simulate the error:\n"
							+ "request - next WRQ or next RRQ received\n"
							+ "data <count> - simulate the error on the n-th data packet where n equals count\n"
							+ "ack <count> - simulate the error on the n-th ack packet where n equals count\n");
					System.out.print("Choice: ");
					String[] choices = s.nextLine().split(" ");
					if (choices[0].equalsIgnoreCase("request")) {
						this.packetType = PacketType.request;
						break;
					} else if (choices[0].equalsIgnoreCase("data")) {
						if (choices.length != 2) // data packet must specify which one to simulate the error
							continue;
						this.packetType = PacketType.data;
						try {
							this.packetCount = Integer.parseInt(choices[1]);
						} catch (Exception e) {
							continue; // parse int failed, need to try again
						}
						break;
					} else if (choices[0].equalsIgnoreCase("ack")) {
						if (choices.length != 2) // ack packet must specify which one to simulate the error
							continue;
						this.packetType = PacketType.ack;
						try {
							this.packetCount = Integer.parseInt(choices[1]);
						} catch (Exception e) {
							continue; // parse int failed, need to try again
						}
						break;
					} else { // error command received, try again
						continue;
					}
				}
				this.printError();
				this.waitForRequest();
			}else if (commands[0].equalsIgnoreCase("exit")) {
				s.close();
				return;
			} else {
				System.out.println("Invalid command, please try again!\n");
				System.out.println("These are the available commands:");
				printMenu();
			}
		}
	}

	public static void main(String[] args) {
		new TFTPErrorSimulator().waitForCommand();
	}
}
