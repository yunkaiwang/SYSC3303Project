package project;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;

/**
 * Request listener class that will listen on the given port for new requests, and
 * create new request handler thread for handling the request
 * 
 * @author yunkai wang
 *
 */
public class TFTPRequestListener extends Thread {
	private TFTPServer server; // server that this listener is working for
	private int port; // port that it will be listening on
	// this boolean is used to check if this request listen should accept
	// new request, this can be set to false by the server, in which case
	// the request listen will not accept any new requests
	private boolean acceptNewConnection;
	private DatagramSocket socket;
	
	/**
	 * Constructor
	 * 
	 * @param server
	 * @param port
	 */
	TFTPRequestListener(TFTPServer server, int port) {
		this.server = server;
		this.port = port;
		this.acceptNewConnection = true;
	}
	
	/**
	 * Request the request listener to step
	 */
	protected void stopRequestListener() {
		this.acceptNewConnection = false;
		socket.close(); // close socket as it will not be used any more
	}
	
	/**
	 * Override run method
	 */
	@Override
	public void run() {
		server.incrementNumThread(); // request listen is one thread of the server
		ThreadLog.print("Request listener is waiting for new requests");
		try {
			socket = new DatagramSocket(port); // create the socket to listener to listen on the
			                                   // port given by the server
		} catch (SocketException se) { // failed to bound the port
			se.printStackTrace();
			System.exit(1);
		}
		
		while (acceptNewConnection) { // keep waiting for new connection
			// create new packet for receiving new requests
			DatagramPacket packet = new DatagramPacket(new byte[TFTPPacket.MAX_LENGTH], TFTPPacket.MAX_LENGTH);
			try {
				// a new packet is received
				socket.receive(packet);
				// only handle RRQ or WRQ at this moment, ignore all other requests received
				if (server.isReadRequest(packet.getData()) || server.isWriteRequest(packet.getData())) {
					server.createNewRequestHandler(packet, packet.getAddress(), packet.getPort()).start();
				}
			} catch(IOException e) {
				// IOException raised when the socket is closed while waiting
				// for new requests, which means new requests is received after
				// the server has told the request listener to stop listen to
				// new requests, so we should ignore handling this exception
				// since the server is stopped by the server operator, so it
				// should be safe to just ignore this exception
				continue;
			}
		}
		
		socket.disconnect(); // disconnect the socket
		server.decrementNumThread(); // decrease the thread count in server
	}
	
}
