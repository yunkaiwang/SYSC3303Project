package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * For iteration 2, the error simulator will not touch any of the packets
 * received, it will just forward all those packets.
 * 
 * @author yunkai wang Last modified on Feb 15, 2018
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

			// check if error packet is received from the server, if so, abort the connection
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

			// check if error packet is received from the client, if so, terminate the connection
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

	/**
	 * Wait for a new request
	 */
	public void waitForRequest() {
		for (;;) { // run forever
			byte data[] = new byte[TFTPPacket.MAX_LENGTH];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			System.out.println("Error simulator is waiting for new requests.");

			try {
				// received a new request
				receiveSocket.receive(packet);
				System.out.println("Error simulator received new requests.");
				// create the request packet from the packet received from the client
				TFTPRequestPacket requestPacket = TFTPRequestPacket.createFromPacket(packet);

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
				System.out
						.println("Connection is aborted as the following error message is received:\n" + e.getMessage());
			}
		}
	}

	public static void main(String[] args) {
		new TFTPErrorSimulator().waitForRequest();
	}
}
