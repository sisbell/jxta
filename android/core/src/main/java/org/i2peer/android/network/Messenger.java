package org.i2peer.android.network;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.i2peer.android.OnMessageLoadedListener;

public interface Messenger {
	void ping(InetSocketAddress destinationAddress, OnMessageLoadedListener listener)
			throws IOException;
	
}
