package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TFTPErrorSimulator {
	public static final int TFTP_LISTEN_PORT = 23;
	
	DatagramSocket receiveSocket, sendReceiveSocket;

	public TFTPErrorSimulator() {
		try {
			receiveSocket = new DatagramSocket(TFTP_LISTEN_PORT);
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) { // Can't create the socket.
			se.printStackTrace();
			System.exit(1);
		}
	}

	public void waitForRequest() {
		DatagramPacket sendServerPacket = null, receiveServerPacket; //packet used to talk to server
		DatagramPacket sendClientPacket = null, receiveClientPacket;
		for (;;) { // run forever
			byte data[] = new byte[TFTPServer.MAX_LENGTH];
			receiveClientPacket = new DatagramPacket(data, data.length);

			try {
				receiveSocket.receive(receiveClientPacket);  //received something
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
				sendReceiveSocket.receive(receiveServerPacket);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			// create a new packet to send back to the host
			try {
				sendClientPacket = new DatagramPacket(receiveServerPacket.getData(), receiveServerPacket.getLength(),
						                              InetAddress.getLocalHost(), receiveClientPacket.getPort());
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			// send request back to the host
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
