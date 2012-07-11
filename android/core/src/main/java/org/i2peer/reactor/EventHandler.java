package org.i2peer.reactor;

import java.nio.channels.SelectableChannel;

public interface EventHandler {
	
	void handleEvent(int eventType);
	
	SelectableChannel getChannel();
	
}
