package project;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class TFTPRequestHandler extends Thread {
	private TFTPServer server; // server that this listener is working for
	private InetAddress address;
	private int port;
	private DatagramPacket packet;
	
	TFTPRequestHandler(TFTPServer server, DatagramPacket packet, InetAddress address, int port) {
		this.server = server;
		this.packet = packet;
		this.address = address;
		this.port = port;
	}
	
	// handle the request
	private void handleRequest() {
		
	}
	
	@Override
	public void run() {
		server.incrementNumThread(); // increase the thread count in server
		handleRequest();
		server.decrementNumThread(); // decrease the thread count in server
	}
	
}
