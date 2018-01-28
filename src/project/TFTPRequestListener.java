package project;

import java.net.DatagramSocket;
import java.net.SocketException;

public class TFTPRequestListener extends Thread {
	private TFTPServer server; // server that this listener is working for
	private int port; // port that it will be listening on
	// this boolean is used to check if this request listen should accept
	// new request, this can be set to false by the server, in which case
	// the request listen will not accept any new requests
	private boolean acceptNewConnection;
	private DatagramSocket socket;
	
	TFTPRequestListener(TFTPServer server, int port) {
		this.server = server;
		this.port = port;
		this.acceptNewConnection = true;
	}
	
	protected void refuseNewConnection() {
		this.acceptNewConnection = false;
		socket.close(); // close socket as it will not be used any more
	}
	
	@Override
	public void run() {
		server.incrementNumThread(); // request listen is one thread of the server
		
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException se) { // failed to bound the port
			se.printStackTrace();
			System.exit(1);
		}
		
		while (acceptNewConnection) { // keep waiting for new connection
			
		}
	}
	
}
