package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * For iteration 4, the error simulator will simulate the following errors
 *  - lose any packet (WRQ, RRQ, DATA, ACK)
 *  - delay DATA or ACK packet with a specified delay time
 *  - duplicate DATA or ACK packet
 *  - Corrupt a packet(including change opcode, append data, etc.)
 *  - unknown tid error
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
		duplicate("Duplicate a packet"),
		corrupt("Corrupt a packet"),
		tid("Sending packet using an unknown tid");

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
			else if (commands.equalsIgnoreCase("corrupt"))
				return ErrorType.corrupt;
			else if (commands.equalsIgnoreCase("tid"))
				return ErrorType.tid;
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
	
	/**
	 * This enum class represents all types of corruption that
	 * an user can perform on the packets
	 */
	private enum CorruptionType {
		CHANGE_OPCODE("Change opcode in packet"),
		REMOVE_FILENAME_DELIMITER("Remove '0' byte after file name"),
		REMOVE_MODE_DELIMITER("Remove '0' byte after mode"),
		REMOVE_FILENAME("Remove file name"),
		MODIFY_MODE("Change mode in packet"),
		APPEND_PACKET("Add extra byte to the packet"),
		SHRINK_PACKET("Shrink the packet"),
		CHANGE_BLOCKNUMBER("Change block number");
		
		String representation;
		
		CorruptionType(String repr) {
			this.representation = repr;
		}
		
		public static CorruptionType getFromCode(int code) {
			switch(code) {
			case (0):
				return CorruptionType.CHANGE_OPCODE;
			case (1):
				return CorruptionType.REMOVE_FILENAME_DELIMITER;
			case (2):
				return CorruptionType.REMOVE_MODE_DELIMITER;
			case (3):
				return CorruptionType.REMOVE_FILENAME;
			case (4):
				return CorruptionType.MODIFY_MODE;
			case (5):
				return CorruptionType.APPEND_PACKET;
			case (6):
				return CorruptionType.SHRINK_PACKET;
			case (7):
				return CorruptionType.CHANGE_BLOCKNUMBER;
			default:
				return null;
			}
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
	private InetAddress clientAddress; // address to send
	private int clientPort; // client port
	private InetAddress serverAddress; // request handler address
	private int serverPort; // request handler port
	private long delayTime; // the delay time that the user specified
	private boolean errorSimulated; // if the error has been simulated, this is important
								    // since if the user choose to delay the data packet
									// with block number 5, that packet will be received
									// twice, so if we don't keep this variable, the error
									// will be simulated twice which is unexpected
	private CorruptionType corruptionType; // type of corruption to simulate
	private int newBlockNumber; // block number to simulate the error
	
	/**
	 * Constructor
	 */
	public TFTPErrorSimulator() {
		try {
			receiveSocket = new DatagramSocket(TFTP_LISTEN_PORT);
			sendReceiveSocket = new DatagramSocket();
			sendReceiveSocket.setSoTimeout(10000); // wait for 10 seconds
			serverAddress = InetAddress.getLocalHost(); // default server address is localhost
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Print the error that will be simulated
	 */
	private void printError() {
		System.out.println("This is the type of error that will be simulated:\n" + "Error Type: "
				+ this.errorType + "\n"
				+ (this.errorType == ErrorType.corrupt ? "Corruption type: " + this.corruptionType + "\n" : "")
				+ "Error Packet: " + this.packetType + "\n"
				// print count only if it's not simulating an error on request packet
				+ (this.packetType == PacketType.request ? "" :
						"Block number: " + this.blockNumber + "\n"));
	}

	/**
	 * Wait for a new request
	 */
	private void waitForRequest() {
		DatagramPacket packet = TFTPPacket.createDatagramPacketForReceive();
		System.out.println("Error simulator is waiting for new requests.");

		try {
			// wait forever for the next request packet
			while (true) {
				try {
					// received a new request
					receiveSocket.receive(packet);
					System.out.println("Error simulator received new requests.");
					
					clientAddress = packet.getAddress();
					clientPort = packet.getPort();
					// forward the request packet to server
					DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(),
										serverAddress, TFTPServer.TFTP_LISTEN_PORT);
					
					// check if the user wants to simulate the error on TFTP request packet
					// as a request packet is received
					if (!errorSimulated && this.packetType == PacketType.request) {
						// for iteration 3, duplicate or delay TFTP request packet will not be handled,
						// so even if you specify duplicate a TFTP request packet, the error will
						// not be simulated
						errorSimulated = true;
						if (this.errorType == ErrorType.lose) { // lose request packet
							System.out.println("*****Lose packet*****");
							continue;
						} else if (this.errorType == ErrorType.delay) {
							System.out.println("*****Packet is delayed*****");
							new Thread(new DelayThread(sendReceiveSocket, sendPacket, delayTime), "Delay thread").start();
							if (delayTime >= 2000)
								continue;
						} else if (this.errorType == ErrorType.duplicate) {
							System.out.println("*****Packet is duplicated*****");
							sendReceiveSocket.send(sendPacket);
							sendReceiveSocket.send(sendPacket);
						} else if (this.errorType == ErrorType.corrupt) {
							System.out.println("*****Packet is corrupted*****");
							sendPacket = corruptPacket(sendPacket);
							sendReceiveSocket.send(sendPacket);
						}
						break;
					} else {
						sendReceiveSocket.send(sendPacket);
						break;
					}
				} catch (SocketTimeoutException e) {
					continue;
				}
			}
			// handle the file transfer
			while (true)
				receiveAndSend();
		} catch (SocketTimeoutException e) {
			System.out.println("Error simulator doesn't received any packet within 10 seconds,\n"
		        + "the connection should have finished");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Corrupt the packet based on the type of corruption that user selected
	 * 
	 * @param packet
	 * @return corrupted packet
	 */
	private DatagramPacket corruptPacket(DatagramPacket packet) {
		int length = packet.getLength();
		byte[] data = packet.getData();

		if (this.corruptionType == CorruptionType.CHANGE_OPCODE) {
			// change opcode to be 22 (just a randomly selected number)
			data[0] = 2;
			data[1] = 2;
		} else if (this.corruptionType == CorruptionType.REMOVE_FILENAME_DELIMITER) {
			// remove 0 byte after the file name
			int i = 1;
			while (data[++i] != 0 && i < length)
				;
			data[i] = (byte)0xff; // change the delimiter from 0 to 0xff
		} else if (this.corruptionType == CorruptionType.REMOVE_MODE_DELIMITER) {
			// remove 0 byte after the mode
			data[length - 1] = (byte)0xff; // change the delimiter from 0 to 0xff
		} else if (this.corruptionType == CorruptionType.REMOVE_FILENAME) {
			// remove file name from the packet
			int i = 1;
			while (data[++i] != 0 && i < length)
				;
			byte[] newData = new byte[length - i + 2];
			
			// copy the bytes from old byte array
			System.arraycopy(data, 0, newData, 0, 2);
			System.arraycopy(data, i, newData, 2, newData.length - 2);
			length = length - i + 2;
			data = newData;
		} else if (this.corruptionType == CorruptionType.MODIFY_MODE) {
			// modify the mode to be a new mode other than 'netascii' and 'octet'
			int i = 1;
			while (data[++i] != 0 && i < length)
				;
			byte[] invalidMode = "invalidMode".getBytes();
			length = i + invalidMode.length + 1;
			byte[] newData = new byte[length];
			// copy the bytes from old byte array
			System.arraycopy(data, 0, newData, 0, i);
			// copy the invalid mode into the newData
			System.arraycopy(data, i, invalidMode, 0, invalidMode.length);
			newData[length - 1] = 0;
			data = newData;
		} else if (this.corruptionType == CorruptionType.APPEND_PACKET) {
			// append 4 more bytes to the current packet
			byte[] newData = new byte[length + 4];
			// copy the bytes from old byte array
			System.arraycopy(data, 0, newData, 0, length);
			for (int i = length; i < length + 4; ++i)
				newData[i] = (byte)0xff;
			length += 4;
			data = newData;
		} else if (this.corruptionType == CorruptionType.SHRINK_PACKET) {
			// shrink the packet to be less than 4 bytes
			byte[] newData = new byte[2];
			newData[0] = data[0];
			newData[1] = data[1];
			length = 2;
			data = newData;
		} else if (this.corruptionType == CorruptionType.CHANGE_BLOCKNUMBER) {
			// change the block number to be the user specified block number
			data[2] = (byte) ((newBlockNumber >> 8) & 0xFF);
			data[3] = (byte) (newBlockNumber & 0xFF);
		}
		
		return new DatagramPacket(data, length, packet.getAddress(), packet.getPort());
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
		sendReceiveSocket.receive(receivePacket); // receive new packet
		System.out.println("Error simulator has received the packet.");

		// remember the server port if the server port is still unknown
		if (serverPort == -1)
			serverPort = receivePacket.getPort();
		
		// if the request is from client, we send it to server
		if (receivePacket.getPort() == clientPort)
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					serverAddress, serverPort);
		// if the request if from server, we send it to client
		else
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(),
					clientAddress, clientPort);
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
			} else if (errorType == ErrorType.corrupt) {
				System.out.println("*****Packet is corrupted*****");
				sendPacket = corruptPacket(sendPacket);
				sendReceiveSocket.send(sendPacket);
			} else if (errorType == ErrorType.tid) {
				System.out.println("*****Packet is sent from unknown tid*****");
				new DatagramSocket().send(sendPacket);
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
			} else if (errorType == ErrorType.corrupt) {
				System.out.println("*****Packet is corrupted*****");
				sendPacket = corruptPacket(sendPacket);
				sendReceiveSocket.send(sendPacket);
			} else if (errorType == ErrorType.tid) {
				System.out.println("*****Packet is sent from unknown tid*****");
				new DatagramSocket().send(sendPacket);
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
		this.corruptionType = null;
		this.newBlockNumber = -1;
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
				+ "  corrupt      - corrupt the packet\n"
				+ "  tid          - create an unknown tid\n"
				+ "  exit         - exit the error simulator\n"
				+ "  ip           - print current server ip\n"
		        + "  connect <ip> - change server ip to the given address(i.e., connect localhost)\n");
	}
	
	/**
	 * Print current server ip address
	 */
	private void printServerIP() {
		System.out.println("Current server ip is : " + this.serverAddress.toString());
	}
	
	/**
	 * Set server ip address to the given new address
	 * 
	 * @param newAddress
	 */
	private void setServerIP(String newAddress) {
		try {
			if (newAddress.equalsIgnoreCase("localhost"))
				this.serverAddress = InetAddress.getLocalHost();
			else
				this.serverAddress = InetAddress.getByName(newAddress);
		} catch (Exception e) {
			System.out.println("Failed to change server ip to " + newAddress + ". Please try again.");
		}
	}
	
	/**
	 * Choose type of corruption
	 * 
	 * @param s
	 */
	private void chooseCorruptType(Scanner s) {
		while (true) {
			System.out.println("Plase select the type of corruption:\n"
				+ "0 - Change opcode in packet\n"
				+ "1 - Remove '0' byte after file name\n"
				+ "2 - Remove '0' byte after mode\n"
				+ "3 - Remove file name\n"
				+ "4 - Change mode in packet\n"
				+ "5 - Add extra byte to the packet\n"
				+ "6 - Shrink the packet\n"
				+ "7 - Change block number\n");
			System.out.print("Choice: ");
			String choice = s.nextLine();
			try {
				corruptionType = CorruptionType.getFromCode(Integer.parseInt(choice));
				if (corruptionType == CorruptionType.CHANGE_BLOCKNUMBER) {
					System.out.print("Please enter the new block number you want: ");
					this.newBlockNumber = Integer.parseInt(s.nextLine());
					return;
				} else if (corruptionType != null)
					return;
			} catch (Exception e) {
				System.out.println("Please enter a valid choiec.");
				continue;
			}
		}
	}
	
	/**
	 * Chooce the type of packet to simulate the error.
	 * 
	 * @param s
	 */
	private void choocePacketType(Scanner s) {
		// this while loop chooces the type of packet to simulate the error
		while (true) {
			System.out.println("Plase select the type of packet you want to simulate the error:\n"
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
			} else if (commands.equalsIgnoreCase("lose") ||
					   commands.split(" ")[0].equalsIgnoreCase("delay") ||
					   commands.equalsIgnoreCase("duplicate") ||
					   commands.equalsIgnoreCase("corrupt") ||
					   commands.equalsIgnoreCase("tid")) {
				String[] splitCommand = commands.split(" ");
				if (splitCommand[0].equalsIgnoreCase("delay")) {
					if (splitCommand.length == 2) {
						try {
							this.delayTime = Long.parseLong(commands.split(" ")[1]);
						} catch (Exception e) { // parse failed
							System.out.println("Please enter a valid delay time");
							continue;
						}
					} else {
						System.out.println("Please enter a valid delay time");
						continue;
					}
				} else if (splitCommand[0].equalsIgnoreCase("corrupt"))
					this.chooseCorruptType(s);
				
				this.errorType = ErrorType.getFromKeyword(splitCommand[0]);
				this.choocePacketType(s);
				this.printError();
				this.waitForRequest();
			} else if (commands.equalsIgnoreCase("exit")) {
				s.close();
				return;
			} else if (commands.equalsIgnoreCase("ip")) {
				printServerIP();
				continue;
			} else if (commands.split(" ")[0].equalsIgnoreCase("connect")) {
				setServerIP(commands.split(" ")[1]);
				continue;
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
