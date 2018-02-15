package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Please note that for project iteration 1, the error simulator will not do any
 * actual work, it will just receive an request from client, and forward the
 * request to the server, receive the response and return the response back to
 * the client. This class currently only contains one function which does the
 * request-forward, and it should be sufficient for iteration 1, more functions
 * will be added in future development.
 * 
 * @author yunkai wang
 * Last modified on Feb 3, 2018
 */
public class TFTPErrorSimulator {
	public static final int TFTP_LISTEN_PORT = 23; // default error simulator port
	DatagramSocket receiveSocket, sendReceiveSocket;

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
	 * Check if the given packet is a ACK data packet
	 * 
	 * @param packet
	 * @return true if it is a ACK data packet, false otherwise
	 */
	private boolean isAckPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		return data[0] == 0 && data[1] == 4; // OPCODE is 4
	}

	/**
	 * Handle read request
	 * 
	 * @param packet
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void handleRRQ(TFTPRequestPacket packet) throws UnknownHostException, IOException {
		// form the RRQ packet
		DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(), packet.getAddress(),
				TFTPServer.TFTP_LISTEN_PORT);
		// send the RRQ packet
		sendReceiveSocket.send(sendPacket);
		System.out.println("Error simulator has forward the RRQ to the server.");
		byte data[];
		
		TFTPDataPacket dataPacket;
		do {
			data = new byte[TFTPServer.MAX_LENGTH]; // clean old byte

			// form the packet for receiving
			DatagramPacket receivePacket = new DatagramPacket(data, data.length);

			// receive data packet from server
			sendReceiveSocket.receive(receivePacket);
			int serverResponsePort = receivePacket.getPort();
			System.out.println("Error simulator has received data packet from server.");

			dataPacket = TFTPDataPacket.createFromPacket(receivePacket);
			
			// form the data packet that will be sent to the client
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
					packet.getPort());

			// send the data packet
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has forward the data packet to the client.");

			data = new byte[TFTPAckPacket.PACKET_LENGTH]; // clean old byte

			// prepare packet for receiving
			receivePacket = new DatagramPacket(data, data.length);

			// receive the ack packet from client
			sendReceiveSocket.receive(receivePacket);
			System.out.println("Error simulator has received the ack packet from the client.");

			// prepare the ack packet for sending to the server
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
					serverResponsePort);

			// send the ack packet to the server
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has sent the ack packet to the server.");
		} while (!dataPacket.isLastDataPacket());
	}

	/**
	 * Handle write request
	 * 
	 * @param packet
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void handleWRQ(TFTPRequestPacket packet) throws IOException {
		// form the WRQ packet
		DatagramPacket sendPacket = new DatagramPacket(packet.getData(), packet.getLength(), packet.getAddress(),
				TFTPServer.TFTP_LISTEN_PORT);
		// send the WRQ packet
		sendReceiveSocket.send(sendPacket);
		System.out.println("Error simulator has forward the WRQ to the server.");

		byte data[] = new byte[TFTPAckPacket.PACKET_LENGTH];
		
		// prepare the packet for receiving
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);
		
		// receive the ack packet
		sendReceiveSocket.receive(receivePacket);
		int serverResponsePort = receivePacket.getPort();
		System.out.println("Error simulator has received ack packet from server.");
		TFTPDataPacket dataPacket;

		if (isAckPacket(receivePacket)) {
			do {
				// prepare the ack packet to send
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
						packet.getPort());
				
				// send the ack packet
				sendReceiveSocket.send(sendPacket);
				System.out.println("Error simulator has forward the ack packet to the client.");

				data = new byte[TFTPServer.MAX_LENGTH]; // clean old byte
				
				// prepare the packet for receiving
				receivePacket = new DatagramPacket(data, data.length);
				// receive the data packet from client
				sendReceiveSocket.receive(receivePacket);
				System.out.println("Error simulator has received the data packet from the client.");

				dataPacket = TFTPDataPacket.createFromPacket(receivePacket);
				
				// prepare the data packet for sending
				sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
						serverResponsePort);
				
				// send the data packet to the server
				sendReceiveSocket.send(sendPacket);
				System.out.println("Error simulator has sent the data packet to the server.");

				data = new byte[TFTPAckPacket.PACKET_LENGTH]; // clean old byte
				
				// prepare the packet for receiving
				receivePacket = new DatagramPacket(data, data.length);
				
				// receive the packet from server
				sendReceiveSocket.receive(receivePacket);
				System.out.println("Error simulator has received new ack packet.");
			} while (!dataPacket.isLastDataPacket());
			sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), packet.getAddress(),
					packet.getPort());
			sendReceiveSocket.send(sendPacket);
			System.out.println("Error simulator has forward the ack packet to the client.");
		} else {
			System.out.println("Error simulator received invalid response, RRQ failed.");
			return;
		}
	}

	/**
	 * Wait for a new request
	 */
	public void waitForRequest() {
		for (;;) { // run forever
			byte data[] = new byte[TFTPServer.MAX_LENGTH];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			System.out.println("Error simulator is waiting for new requests.");

			try {
				// received a new request
				receiveSocket.receive(packet);
				System.out.println("Error simulator received new requests.");
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// create the request packet from the packet received from the client
			TFTPRequestPacket requestPacket = TFTPRequestPacket.createFromPacket(packet);

			try {
				if (requestPacket.isReadRequest()) {
					handleRRQ(requestPacket);
				} else if (requestPacket.isWriteRequest()) {
					handleWRQ(requestPacket);
				} else {
					System.out.println("Error simulator have received an invalid request.");
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		new TFTPErrorSimulator().waitForRequest();
	}
}
