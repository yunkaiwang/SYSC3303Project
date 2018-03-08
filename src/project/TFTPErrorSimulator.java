package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

		public static ErrorType getFromKeyword(String commands) {
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
	private int dataPacketCount; // number of data packets that have received, use to keep track of
									// the packet that we will simulate the error
	private int ackPacketCount; // number of ack packets that have received, use to keep track of
								// the packet that we will simulate the error

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
	 * Check if we should simulate error on current packet
	 * 
	 * @param type
	 * @return true if current packet is the packet that we should simulate error, false otherwise
	 */
	private boolean shouldSimulateError(PacketType type) {
		return type == this.packetType &&
			(this.packetType == PacketType.data ? this.dataPacketCount : this.ackPacketCount) == this.packetCount;
	}
	
	/**
	 * Handle read request
	 * 
	 * @param packet
	 * @throws IOException
	 * @throws TFTPErrorException
	 */
	private void handleRRQ(TFTPRequestPacket packet) throws IOException, TFTPErrorException {
		InetAddress address = packet.getAddress();
		int clientPort = packet.getPort();
		
		// form the RRQ packet
		DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(),
				address, TFTPServer.TFTP_LISTEN_PORT);
		// send the RRQ packet
		sendReceiveSocket.send(sendPacket);
		System.out.println("Error simulator has forward the packet to the server.");

		DatagramPacket receivePacket;
		TFTPPacket tftppacket;
		TFTPDataPacket dataPacket = null;
		do {
			// form the packet for receiving
			receivePacket = TFTPPacket.createDatagramPacketForReceive();

			// receive data packet from server
			sendReceiveSocket.receive(receivePacket);
			System.out.println("Error simulator has received packet from server.");
			
			int serverResponsePort = receivePacket.getPort(); // remember server port
			
			// check if error packet is received from the server, if so, abort the connection
			// expect TFTPDataPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPDataPacket)) {
				System.out.println("Error packet received, connection will be closed.");
				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
						: "Unknown packet received.";
				// send the error packet to client
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
						address, clientPort);
				sendReceiveSocket.send(sendPacket); // send the error packet
				System.out.println("Error simulator has forward the error packet to the client.");
				
				throw new TFTPErrorException(errorMsg); // terminate the connection
			}

			++this.dataPacketCount; // received 1 data packet
			if (this.shouldSimulateError(PacketType.data)) { // check if we should simulate error
				if (this.errorType == ErrorType.lose) { // lose the data packet
					System.out.println("*****Lose packet*****");
					// prepare to receive a new data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received data packet again.");
				} else if (this.errorType == ErrorType.delay) { // delay the data packet
					System.out.println("*****Delay packet*****");
					
					// keep the old packet
					DatagramPacket oldPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, clientPort);
					
					// prepare to receive a new data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received data packet again.");
					
					// keep the new packet received
					DatagramPacket newPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());

					sendReceiveSocket.send(oldPacket);
					System.out.println("Error simulator send old data packet to client.");
					
					// prepare to receive ack packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received ack packet from client.");
					
					// forward the ack packet to server
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					sendReceiveSocket.send(sendPacket);
					System.out.println("Error simulator send ack packet to server.");

					System.out.println("Error simulator send delayed data packet to client.");
					// recover the delayed data packet
					receivePacket = newPacket;
				} else if (this.errorType == ErrorType.duplicate) { // duplicate the data packet
					System.out.println("*****Duplicate packet*****");
					
					// prepare the duplicate packet
					DatagramPacket duplicatePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());
					
					// send the data packet to client
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, clientPort);
					sendReceiveSocket.send(sendPacket); // send the data packet
					System.out.println("Error simulator has forward the packet to the client.");
					
					// prepare to receive ack packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received ack packet from client.");
					
					// forward the ack packet to server
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					sendReceiveSocket.send(sendPacket);
					System.out.println("Error simulator send ack packet to server.");

					// send duplicate packet to client
					receivePacket = duplicatePacket;
					System.out.println("Error simulator will send the duplicate packet to client.");
				}
			}

			// form the data packet that will be sent to the client
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					address, clientPort);
			sendReceiveSocket.send(sendPacket); // send the data packet
			System.out.println("Error simulator has forward the packet to the client.");

			dataPacket = (TFTPDataPacket) tftppacket;
			
			// prepare packet for receiving
			receivePacket = TFTPPacket.createDatagramPacketForReceive();
			sendReceiveSocket.receive(receivePacket); // receive the packet from client
			System.out.println("Error simulator has received the packet from the client.");
			
			// check if error packet is received from the client, if so, terminate the connection
			// expect TFTPAckPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPAckPacket)) {
				System.out.println("Error packet received, connection will be closed.");
				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
					: "Unknown packet received.";

				// send the error packet to server
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
						address, serverResponsePort);
				sendReceiveSocket.send(sendPacket); // send the error packet
				System.out.println("Error simulator has forward the error packet to the server.");
				
				throw new TFTPErrorException(errorMsg); // terminate the connection
			}
			
			++this.ackPacketCount; // received 1 ack packet
			if (this.shouldSimulateError(PacketType.ack)) { // check if we should simulate the error
				if (this.errorType == ErrorType.lose) { // lose the ack packet
					System.out.println("*****Lose packet*****");
					// prepare to receive a new data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive the same data packet from server
					System.out.println("Error simulator received data packet from server.");
					
					// send the data packet to client again
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, clientPort);
					sendReceiveSocket.send(sendPacket); // send the packet
					System.out.println("Error simulator send the same data packet to client.");
					
					// prepare to receive a new ack packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket);
					System.out.println("Error simulator received ack packet from client.");
				} else if (this.errorType == ErrorType.delay) {
					System.out.println("*****Delay packet*****");
					
					// keep the old packet
					DatagramPacket oldPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					
					// prepare to receive a new data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received data packet again.");
					
					sendReceiveSocket.send(oldPacket);
					System.out.println("Error simulator send old ack packet to server.");
					
					// prepare to receive a new ack packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new ack packet
					System.out.println("Error simulator received ack packet again.");
				} else if (this.errorType == ErrorType.duplicate) {
					System.out.println("*****Duplicate packet*****");
					
					// prepare the duplicate packet
					DatagramPacket duplicatePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());
					
					// send the ack packet to server
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					sendReceiveSocket.send(sendPacket); // send the data packet
					System.out.println("Error simulator send ack packet to server.");
					
					// prepare to receive data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received data packet from server.");
					
					// forward the ack packet to server
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, clientPort);
					sendReceiveSocket.send(sendPacket);
					System.out.println("Error simulator send ack packet to server.");
					
					// send duplicate packet to server
					System.out.println("Error simulator will send the duplicate packet to server.");
					receivePacket = duplicatePacket;
				}
			}
			
			// prepare the packet for sending to the server
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					address, serverResponsePort);
			sendReceiveSocket.send(sendPacket); // send the packet to the server
			System.out.println("Error simulator has sent the packet to the server.");
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
		InetAddress address = packet.getAddress();
		int clientPort = packet.getPort();
		
		// form the WRQ packet
		DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(),
				address, TFTPServer.TFTP_LISTEN_PORT);
		
		// send the WRQ packet
		sendReceiveSocket.send(sendPacket);
		System.out.println("Error simulator has forward the packet to the server.");

		// prepare the packet for receiving
		DatagramPacket receivePacket = TFTPPacket.createDatagramPacketForReceive();

		// receive the packet
		sendReceiveSocket.receive(receivePacket);
		System.out.println("Error simulator has received packet from server.");

		TFTPPacket tftppacket;
		TFTPDataPacket dataPacket;
		
		// check if error packet is received from server, if so terminate the connection
		// expect TFTPAckPacket, if any other packet is received, raise an exception
		tftppacket = TFTPPacket.createFromPacket(receivePacket);
		if (!(tftppacket instanceof TFTPAckPacket)) {
			System.out.println("Error packet received, connection will be closed.");
			String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
				: "Unknown packet received.";
			
			// prepare the packet to send
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					address, clientPort);

			// send the packet
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has forward the error packet to the client.");
			
			// terminates the connection
			throw new TFTPErrorException(errorMsg);
		}

		int serverResponsePort = receivePacket.getPort(); // remember the server port (the handler's port, not 69)

		do {
			++this.ackPacketCount; // received ack packet
			
			if (this.shouldSimulateError(PacketType.ack)) { // check if we should simulate the error
				if (this.errorType == ErrorType.lose) { // lose the ack packet
					System.out.println("*****Lose packet*****");
					
					// prepare to receive a new data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive the same data packet from client
					System.out.println("Error simulator received data packet from client.");
					
					// send the data packet to server again
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					sendReceiveSocket.send(sendPacket); // send the packet
					System.out.println("Error simulator send the same data packet to server.");
					
					// prepare to receive a new ack packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket);
					System.out.println("Error simulator received ack packet from server.");
				} else if (this.errorType == ErrorType.delay) {
					System.out.println("*****Delay packet*****");
					
					// keep the old packet
					DatagramPacket oldPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, clientPort);
					
					// prepare to receive a new data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received data packet again.");
					
					// send the data packet to server
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					sendReceiveSocket.send(sendPacket);
					System.out.println("Error simulator send data packet to server.");
					
					// send the ack packet to client
					sendReceiveSocket.send(oldPacket);
					System.out.println("Error simulator send old ack packet to client.");
					
					// prepare to receive a new ack packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new ack packet
					System.out.println("Error simulator received ack packet again.");
				} else if (this.errorType == ErrorType.duplicate) {
					System.out.println("*****Duplicate packet*****");
					
					// prepare the duplicate packet
					DatagramPacket duplicatePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());
					
					// send the ack packet to client
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, clientPort);
					sendReceiveSocket.send(sendPacket); // send the data packet
					System.out.println("Error simulator has forward the ack packet to the client.");
					
					// prepare to receive data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received data packet from client.");
					
					// forward the data packet to server
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					sendReceiveSocket.send(sendPacket);
					System.out.println("Error simulator send data packet to server.");
					
					// send duplicate packet to server
					System.out.println("Error simulator will send the duplicate packet to client.");
					receivePacket = duplicatePacket;
				}
			}
			// prepare the packet to send
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					address, clientPort);

			sendReceiveSocket.send(sendPacket); // send the packet
			System.out.println("Error simulator has forward the packet to the client.");

			receivePacket = TFTPPacket.createDatagramPacketForReceive(); // prepare the packet for receiving
			sendReceiveSocket.receive(receivePacket); // receive the packet from client
			System.out.println("Error simulator has received the packet from the client.");
		
			// check if error packet is received from client, if so terminate the connection
			// expect TFTPDataPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPDataPacket)) {
				System.out.println("Error packet received, connection will be closed.");
				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
					: "Unknown packet received.";
				
				// prepare the packet for sending
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
						address, serverResponsePort);
				sendReceiveSocket.send(sendPacket); // send the packet to the server
				System.out.println("Error simulator has sent the error packet to the server.");
				
				// terminates the connection
				throw new TFTPErrorException(errorMsg);
			}
			++this.dataPacketCount; // received 1 data packet
			
			if (this.shouldSimulateError(PacketType.data)) { // check if we should simulate error
				if (this.errorType == ErrorType.lose) { // lose the data packet
					System.out.println("*****Lose packet*****");
					// prepare to receive a new data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received data packet again.");
				} else if (this.errorType == ErrorType.delay) { // delay the data packet
					System.out.println("*****Delay packet*****");
					
					// keep the old packet
					DatagramPacket oldPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					
					// prepare to receive a new data packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received data packet again.");
					
					// keep the new packet received
					DatagramPacket newPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());

					sendReceiveSocket.send(oldPacket);
					System.out.println("Error simulator send old data packet to server.");
					
					// prepare to receive ack packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new data packet
					System.out.println("Error simulator received ack packet from server.");
					
					// forward the ack packet to client
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, clientPort);
					sendReceiveSocket.send(sendPacket);
					System.out.println("Error simulator send ack packet to client.");
					
					System.out.println("Error simulator send delayed data packet to server.");
					// recover the delayed data packet
					receivePacket = newPacket;
				} else if (this.errorType == ErrorType.duplicate) { // duplicate the data packet
					System.out.println("*****Duplicate packet*****");
					
					// prepare the duplicate packet
					DatagramPacket duplicatePacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());
					
					// send the data packet to server
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, serverResponsePort);
					sendReceiveSocket.send(sendPacket); // send the data packet
					System.out.println("Error simulator has forward the packet to the server.");
					
					// prepare to receive ack packet
					receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					sendReceiveSocket.receive(receivePacket); // receive new ack packet
					System.out.println("Error simulator received ack packet from server.");
					
					// forward the ack packet to client
					sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
							address, clientPort);
					sendReceiveSocket.send(sendPacket);
					System.out.println("Error simulator send ack packet to client.");

					// send duplicate packet to server
					receivePacket = duplicatePacket;
					System.out.println("Error simulator will send the duplicate packet to server.");
				}
			}
			
			// prepare the packet for sending
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					address, serverResponsePort);

			// send the packet to the server
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has sent the packet to the server.");

			dataPacket = (TFTPDataPacket) tftppacket;

			// prepare the packet for receiving
			receivePacket = TFTPPacket.createDatagramPacketForReceive();

			// receive the packet from server
			sendReceiveSocket.receive(receivePacket);

			// check if error packet is received from server, if so terminate the connection
			// expect TFTPAckPacket, if any other packet is received, raise an exception
			tftppacket = TFTPPacket.createFromPacket(receivePacket);
			if (!(tftppacket instanceof TFTPAckPacket)) {
				System.out.println("Error packet received, connection will be closed.");

				String errorMsg = (tftppacket instanceof TFTPErrorPacket) ? ((TFTPErrorPacket) tftppacket).getErrorMsg()
						: "Unknown packet received.";

				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
						address, clientPort);
				sendReceiveSocket.send(sendPacket);
				System.out.println("Error simulator has forward the error packet to the client.");

				// terminates the connection
				throw new TFTPErrorException(errorMsg);
			}
		} while (!dataPacket.isLastDataPacket());
		sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
				address, clientPort);
		sendReceiveSocket.send(sendPacket);
		System.out.println("Error simulator has forward the packet to the client.");
	}

	public void printError() {
		System.out.println("This is the type of error that will be simulated:\n" + "Error Type: "
				+ this.errorType.toString() + "\n" + "Error Packet: " + this.packetType.toString() + "\n"
				+ (this.packetType == PacketType.request ? "" : // print count only if it's not simulating an error on
																// request packet
						"Count: " + this.packetCount + "\n"));
	}

	/**
	 * Wait for a new request
	 */
	public void waitForRequest() {
		DatagramPacket packet = TFTPPacket.createDatagramPacketForReceive();
		System.out.println("Error simulator is waiting for new requests.");

		try {
			// received a new request
			receiveSocket.receive(packet);
			System.out.println("Error simulator received new requests.");
			// create the request packet from the packet received from the client
			TFTPRequestPacket requestPacket = TFTPRequestPacket.createFromPacket(packet);

			// check if the user wants to simulate the error on TFTP request packet
			// as a request packet is received
			if (this.packetType == PacketType.request) {
				// for iteration 3, duplicate or delay TFTP request packet will not be handled,
				// so even if you specify duplicate a TFTP request packet, the error will
				// not be simulated
				if (this.errorType == ErrorType.lose) { // lose request packet
					System.out.println("*****Lose packet*****");

					// prepare to receive a new request packet
					packet = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
					receiveSocket.receive(packet); // receive new request packet from client
					requestPacket = TFTPRequestPacket.createFromPacket(packet);
					System.out.println("Error simulator received request packet again.");
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
	 * Clear the attributes so that all previous attributes are cleared
	 */
	private void initialize() {
		this.errorType = null;
		this.packetCount = -1;
		this.packetType = null;
		this.dataPacketCount = -1;
		this.ackPacketCount = -1;
	}

	/**
	 * Print the menu
	 */
	private static void printMenu() {
		System.out.println("Available commands:\n"
	            + "  menu         - display the menu\n"
				+ "  normal       - normal operation\n"
	            + "  lose         - lose a packet\n"
				+ "  delay        - delay a packet until another data packet is received\n"
				+ "  duplicate    - duplicate a packet\n"
				+ "  exit         - exit the error simulator\n");
	}

	/**
	 * Main loop of this file, the user can choose whether starts a normal request
	 * or simulating an error, after the user made the choice, the error simulator
	 * will start to listen for new request
	 */
	private void waitForCommand() {
		Scanner s = new Scanner(System.in);

		printMenu();
		while (true) {
			this.initialize(); // clean old attributes
			System.out.print("Command: ");
			String commands = s.nextLine();

			if (commands.equalsIgnoreCase("menu")) {
				printMenu();
				continue;
			} else if (commands.equalsIgnoreCase("normal")) {
				this.waitForRequest();
				continue;
			} else if (commands.equalsIgnoreCase("lose") || commands.equalsIgnoreCase("delay")
					|| commands.equalsIgnoreCase("duplicate")) {
				this.errorType = ErrorType.getFromKeyword(commands);

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
			} else if (commands.equalsIgnoreCase("exit")) {
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
