/**
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
package org.i2peer.android;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.SelectionKey;

import org.i2peer.android.jxta.JxtaMessenger;
import org.i2peer.android.jxta.JxtaModule;
import org.i2peer.android.messages.PingResponse;
import org.i2peer.android.network.Messenger;
import org.i2peer.android.network.PingMessageLoader;
import org.i2peer.reactor.InitiationDispatcher;
import org.i2peer.reactor.impl.DefaultDispatcher;
import org.i2peer.reactor.impl.jxta.JxtaAcceptor;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.TestCase;

public class HelloTest extends TestCase {
	
	//Network test
	public void testSayHello() throws Exception {
		Injector injector = Guice.createInjector(new JxtaModule());
		Messenger m = injector.getInstance(Messenger.class);
		
		m.ping(new InetSocketAddress("i2peer.net", 9701), new OnMessageLoadedListener() {

			@Override
			public void onMessageReceive(Message message) {
				System.out.println("Received message");
			}	
		});
	}
	
	public void testToMessageResponse() throws Exception {
		PingResponse response = new PingResponse(new URI("tcp://i2peer.net"), 
				new URI("tcp://i2peer.org"), "peerid", false, "3.0");
		
		assertEquals("JXTAHELLO tcp://i2peer.net tcp://i2peer.org peerid 0 3.0", response.toStringResponse());
	}
	
	public void testA() throws Exception {
		InitiationDispatcher jxtaDispatcher = new DefaultDispatcher();

		jxtaDispatcher.registerHandler(new JxtaAcceptor(new InetSocketAddress(
				9736), jxtaDispatcher), SelectionKey.OP_ACCEPT);

		jxtaDispatcher.handleEvents();		
		while(true) { }
	}
}
