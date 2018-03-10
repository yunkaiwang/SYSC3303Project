package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * For iteration 3, the error simulator will simulate the following errors
 *  - lose any packet (WRQ, RRQ, DATA, ACK)
 *  - delay DATA or ACK packet with a specified delay time
 *  - duplicate DATA or ACK packet
 * Now the error simulator is a multi-thread program, but it will be single
 * thread most of the time, the only time when it became multi-thread is
 * when the user simulate a delay error. In that case, another thread will
 * be created who is responsible to delay the packet and send it after the
 * specified time has passed, but in the meanwhile, the other thread will
 * still be receiving and sending the packets.
 *  
 * 
 * @author yunkai wang
 */
public class TFTPErrorSimulator {
	/**
	 * This is a simple delay thread who is responsible to delay for a given time, and
	 * send the packet after the specified time has passed.
	 */
	private class DelayThread implements Runnable {
		private DatagramSocket socket;
		private DatagramPacket packet;
		private long delayTime;
		
		public DelayThread(DatagramSocket socket, DatagramPacket packet, long delayTime) {
			this.socket = socket;
			this.packet = packet;
			this.delayTime = delayTime;
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(delayTime);
				socket.send(packet);
				ThreadLog.print("Delay thread have sent the delayed packet");
			} catch (InterruptedException e) {
				ThreadLog.print("Delay thread is interrupted while delaying the packet");
			} catch (IOException e) {
				ThreadLog.print("Delay thread failed to send the delayed packet");
			}
		}
	}

	/**
	 * This enum class represents different types of errors that will be simulated
	 */
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

	/**
	 * This enum class represents different types of packet that we
	 * will simulate the error.
	 */
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
	private int blockNumber; // the block number of the packet to raise an error
	private InetAddress address; // address to send
	private int clientPort; // client port
	private int serverPort; // request handler port
	private long delayTime; // the delay time that the user specified
	private boolean errorSimulated; // if the error has been simulated, this is important
								    // since if the user choose to delay the data packet
									// with block number 5, that packet will be received
									// twice, so if we don't keep this variable, the error
									// will be simulated twice which is unexpected
	
	/**
	 * Constructor
	 */
	public TFTPErrorSimulator() {
		try {
			receiveSocket = new DatagramSocket(TFTP_LISTEN_PORT);
			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.setSoTimeout(10000); // wait for 10 seconds
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}
	
	
	/**
	 * Print the error that will be simulated
	 */
	public void printError() {
		System.out.println("This is the type of error that will be simulated:\n" + "Error Type: "
				+ this.errorType + "\n" + "Error Packet: " + this.packetType + "\n"
				+ (this.packetType == PacketType.request ? "" : // print count only if it's not simulating an error on
																// request packet
						"Block number: " + this.blockNumber + "\n"));
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
			if (requestPacket.isReadRequest() || requestPacket.isWriteRequest()) {
				address = requestPacket.getAddress();
				clientPort = requestPacket.getPort();
				// forward the request packet to server
				DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(),
						requestPacket.getAddress(), TFTPServer.TFTP_LISTEN_PORT);
				sendReceiveSocket.send(sendPacket);
				System.out.println("Error simulator has forward the packet to the server.");
				while (true)
					receiveAndSend();
			} else
				System.out.println("Error simulator have received an invalid request.");
		} catch (SocketTimeoutException e) {
			System.out.println("Error simulator doesn't received any packet within 10 seconds,\n"
		        + "the connection should have finished");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Receive a packet on port 23 and forward it to the client/server depending on the port on which
	 * the packet is received
	 * 
	 * @throws IOException
	 */
	private void receiveAndSend() throws IOException, SocketTimeoutException {
		DatagramPacket receivePacket, sendPacket;
		receivePacket = TFTPPacket.createDatagramPacketForReceive(); // create new datagram packet for receiving
		sendReceiveSocket.receive(receivePacket); // receive new request packet from client
		System.out.println("Error simulator has received the packet.");

		// remember the server port if the server port is still unknown
		if (serverPort == -1)
			serverPort = receivePacket.getPort();
		
		// if the request is from server, then we send it to client
		if (receivePacket.getPort() == serverPort)
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					address, clientPort);
		// if the request if from client, then we send it to server
		else
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					address, serverPort);
		TFTPPacket packet = TFTPPacket.createFromPacket(receivePacket);
		// check if current packet is the packet that we should simulate the error
		if (!errorSimulated && packet instanceof TFTPDataPacket && packetType == PacketType.data &&
				((TFTPDataPacket) packet).getBlockNumber() == blockNumber) {
			if (errorType == ErrorType.lose) {
				System.out.println("*****Packet is lost*****");
			} else if (errorType == ErrorType.delay) {
				System.out.println("*****Packet is delayed*****");
				new Thread(new DelayThread(sendReceiveSocket, sendPacket, delayTime), "Delay thread").start();
			} else if (errorType == ErrorType.duplicate) {
				System.out.println("*****Packet is duplicated*****");
				sendReceiveSocket.send(sendPacket);
				sendReceiveSocket.send(sendPacket);
			}
			errorSimulated = true;
		} else if (!errorSimulated && packet instanceof TFTPAckPacket && packetType == PacketType.ack &&
				((TFTPAckPacket) packet).getBlockNumber() == blockNumber) {
			if (errorType == ErrorType.lose) {
				System.out.println("*****Packet is lost*****");
			} else if (errorType == ErrorType.delay) {
				System.out.println("*****Packet is delayed*****");
				new Thread(new DelayThread(sendReceiveSocket, sendPacket, delayTime), "Delay thread").start();
			} else if (errorType == ErrorType.duplicate) {
				System.out.println("*****Packet is duplicated*****");
				sendReceiveSocket.send(sendPacket);
				sendReceiveSocket.send(sendPacket);
			}
			errorSimulated = true;
		} else {
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has sent the packet.");
		}
	}

	/**
	 * Clear the attributes so that all previous attributes are cleared
	 */
	private void initialize() {
		this.serverPort = -1;
		this.errorType = null;
		this.blockNumber = -1;
		this.packetType = null;
		this.delayTime = -1;
		this.errorSimulated = false;
	}

	/**
	 * Print the menu
	 */
	private static void printMenu() {
		System.out.println("Available commands:\n"
	            + "  menu         - display the menu\n"
				+ "  normal       - normal operation\n"
	            + "  lose         - lose a packet\n"
				+ "  delay <time> - delay a packet for a given time(in millisecond)\n"
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
			} else if (commands.equalsIgnoreCase("lose") || commands.split(" ")[0].equalsIgnoreCase("delay")
					|| commands.equalsIgnoreCase("duplicate")) {
				String[] splitCommand = commands.split(" ");
				if (splitCommand[0].equalsIgnoreCase("delay")) {
					if (splitCommand.length == 2)
						this.delayTime = Long.parseLong(commands.split(" ")[1]);
					else {
						System.out.println("Please enter a valid delay time");
						continue;
					}
				}
				
				this.errorType = ErrorType.getFromKeyword(splitCommand[0]);

				// this while loop chooces the type of packet to simulate the error
				while (true) {
					System.out.println("Plase select the type pf packet you want to simulate the error:\n"
							+ "request       - next WRQ or next RRQ received\n"
							+ "data <blknum> - simulate the error on the data packet with the given block number\n"
							+ "ack <blknum>  - simulate the error on the ack packet with the given block number\n");
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
							this.blockNumber = Integer.parseInt(choices[1]);
						} catch (Exception e) {
							continue; // parse int failed, need to try again
						}
						break;
					} else if (choices[0].equalsIgnoreCase("ack")) {
						if (choices.length != 2) // ack packet must specify which one to simulate the error
							continue;
						this.packetType = PacketType.ack;
						try {
							this.blockNumber = Integer.parseInt(choices[1]);
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
