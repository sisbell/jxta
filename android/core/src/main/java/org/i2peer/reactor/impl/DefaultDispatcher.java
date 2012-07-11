package org.i2peer.reactor.impl;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;

import org.i2peer.reactor.InitiationDispatcher;
import org.i2peer.reactor.EventHandler;

public class DefaultDispatcher implements InitiationDispatcher, Runnable {

	private Selector selector;

	private boolean isCancelled;

	public DefaultDispatcher() throws IOException {
		selector = Selector.open();
	}

	public void handleEvents() throws IOException {
		new Thread(this).start();
	}

	@Override
	public void run() {
		isCancelled = false;
		while (!isCancelled) {
			try {
				dispatchRequest();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void cancel() {
		isCancelled = true;
		selector.wakeup();
	}

	@Override
	public void registerHandler(EventHandler handler, int eventType)
			throws ClosedChannelException {
		if(handler == null) {
			throw new IllegalArgumentException("Handler is null");
		}
		handler.getChannel().register(selector, eventType, handler);
		
	}

	@Override
	public void deregisterHandler(EventHandler handler, int eventType) {
		Set<SelectionKey> keys = new HashSet<SelectionKey>(
				selector.keys());
		for(SelectionKey key : keys) {
			EventHandler mh = (EventHandler) key.attachment();
			if(mh.equals(handler)) {
				key.cancel();
			}
		}
	}
	
	private void dispatchRequest() throws IOException {
		selector.select();

		Set<SelectionKey> keys = new HashSet<SelectionKey>(
				selector.selectedKeys());

		for (SelectionKey key : keys) {
			if (key.isValid() && key.attachment() != null) {
				EventHandler mh = (EventHandler) key.attachment();	
				if(key.isWritable()) {
					mh.handleEvent(SelectionKey.OP_WRITE);
				}
				
				if(key.isReadable()) {
					mh.handleEvent(SelectionKey.OP_READ);
				}
				
				if(key.isAcceptable()) {
					mh.handleEvent(SelectionKey.OP_CONNECT);
				}	
			}
		}
		
		selector.selectedKeys().clear();
	}
}
