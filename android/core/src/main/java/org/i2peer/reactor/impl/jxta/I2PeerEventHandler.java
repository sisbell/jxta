package org.i2peer.reactor.impl.jxta;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

import org.i2peer.reactor.EventHandler;

public class I2PeerEventHandler implements EventHandler {
	
	private SocketChannel channel;

	@Override
	public void handleEvent(int eventType) {

	}

	@Override
	public SelectableChannel getChannel() {
		// TODO Auto-generated method stub
		return null;
	}
}
