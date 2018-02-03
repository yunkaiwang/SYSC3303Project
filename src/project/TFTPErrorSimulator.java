package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Please note that for project iteration 1, the error simulator will not do any actual work,
 * it will just receive an request from client, and forward the request to the server, receive
 * the response and return the response back to the client. This class currently only contains
 * one function which does the request-forward, and it should be sufficient for iteration 1,
 * more functions will be added in future development.
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
	 * Wait for an request and forward the request without processing the request
	 */
	public void waitForRequest() {
		DatagramPacket sendServerPacket = null, receiveServerPacket; //packet used to talk to server
		DatagramPacket sendClientPacket = null, receiveClientPacket;
		for (;;) { // run forever
			byte data[] = new byte[TFTPServer.MAX_LENGTH];
			receiveClientPacket = new DatagramPacket(data, data.length);

			try {
				// received a new request
				receiveSocket.receive(receiveClientPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// create datagram packet to forward the request to server
			try {
				sendServerPacket = new DatagramPacket(receiveClientPacket.getData(), receiveClientPacket.getLength(),
						InetAddress.getLocalHost(),TFTPServer.TFTP_LISTEN_PORT);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// send request to server
			try {
				sendReceiveSocket.send(sendServerPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// clean old bytes
			data = new byte[TFTPServer.MAX_LENGTH];
			// wait for server to response
			receiveServerPacket = new DatagramPacket(data, data.length);
			try {
				// received response from server
				sendReceiveSocket.receive(receiveServerPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// create a new packet to send back to the client
			try {
				sendClientPacket = new DatagramPacket(receiveServerPacket.getData(), receiveServerPacket.getLength(),
						                              InetAddress.getLocalHost(), receiveClientPacket.getPort());
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// send request back to the client
			try {
				sendReceiveSocket.send(sendClientPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public static void main(String[] args) {
		new TFTPErrorSimulator().waitForRequest();
	}
}
