package org.i2peer.android;

import java.io.IOException;

public interface MessageLoader<Params> {
	
	void registerListener(OnMessageLoadedListener listener);
	
	void unregisterListener(OnMessageLoadedListener listener);
	
	void execute(Params params) throws IOException;
}
