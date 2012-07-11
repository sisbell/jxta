package org.i2peer.reactor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

public interface InitiationDispatcher {

	void registerHandler(EventHandler handler, int eventType) throws ClosedChannelException;
	
	void deregisterHandler(EventHandler handler, int eventType);
	
	void cancel();
	
	void handleEvents() throws IOException;
}
