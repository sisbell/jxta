package org.i2peer.reactor.impl.jxta;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.i2peer.reactor.EventHandler;

public class JxtaEventHandler implements EventHandler {
	
	private SocketChannel channel;
	
	public JxtaEventHandler(SocketChannel channel) {
		if(channel == null) {
			throw new IllegalArgumentException("Channel is null");
		}
		try {
			channel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.channel = channel;
	}
	
	@Override
	public void handleEvent(int eventType) {
		switch (eventType) {
		case SelectionKey.OP_READ:
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			try {
				while ((channel.read(buffer)) > 0) {
					buffer.flip();
					//TODO: process messages
					System.out.print(new String(buffer.array()));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case SelectionKey.OP_WRITE:
			break;
		}
		/*
		try {
			channel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
	}

	@Override
	public SelectableChannel getChannel() {
		return channel;
	}

}
