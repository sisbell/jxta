/*
 * Copyright (C) 2012 i2peer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.i2peer.android.jxta;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.i2peer.android.OnMessageLoadedListener;
import org.i2peer.android.network.Messenger;
import org.i2peer.android.network.PingMessageLoader;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class JxtaMessenger implements Messenger {
	
	private PingMessageLoader loader;
	
	@Inject
	public JxtaMessenger(@Named("jxta") PingMessageLoader loader) {
		this.loader = loader;
	}

	public void ping(InetSocketAddress destinationAddress, OnMessageLoadedListener listener)
			throws IOException {
		loader.registerListener(listener);
		loader.execute(destinationAddress);
	}


}
