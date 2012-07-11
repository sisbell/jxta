package org.i2peer.android;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class Peer {

	public void startServer() throws IOException {
		ServerSocketChannel c = ServerSocketChannel.open();
		c.configureBlocking(false);
		int port = 9701;
		// Allocate an unbound server socket channel 
		ServerSocketChannel serverChannel = ServerSocketChannel.open( ); 
		// Get the associated ServerSocket to bind it with 
		ServerSocket serverSocket = serverChannel.socket( ); 
		// Create a new Selector for use below 
		Selector selector = Selector.open( );
		// Set the port the server channel will listen to 
		serverSocket.bind (new InetSocketAddress (port));
		// Set nonblocking mode for the listening socket 
		serverChannel.configureBlocking (false);
		// Register the ServerSocketChannel with the Selector 
		serverChannel.register (selector, SelectionKey.OP_ACCEPT);
	}
}
