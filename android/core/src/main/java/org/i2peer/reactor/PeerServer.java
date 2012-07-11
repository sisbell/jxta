package org.i2peer.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;

import org.i2peer.reactor.impl.DefaultDispatcher;

import org.i2peer.reactor.impl.jxta.JxtaAcceptor;

public class PeerServer {

	public static void main(String[] string) throws IOException {

		InitiationDispatcher jxtaDispatcher = new DefaultDispatcher();

		jxtaDispatcher.registerHandler(new JxtaAcceptor(new InetSocketAddress(
				9700), jxtaDispatcher), SelectionKey.OP_ACCEPT);

		jxtaDispatcher.handleEvents();
		
		/*
		 * InitiationDispatcher i2peerDispatcher = new DefaultDispatcher();
		 * TcpAcceptor i2peerAcceptor = new TcpAcceptor(new
		 * InetSocketAddress(9800), new DefaultDispatcher());
		 * i2peerDispatcher.handleEvents();
		 */
	}
}
