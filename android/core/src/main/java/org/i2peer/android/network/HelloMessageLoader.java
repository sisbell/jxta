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
package org.i2peer.android.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.i2peer.android.Message;
import org.i2peer.android.MessageLoader;
import org.i2peer.android.messages.HelloResponse;

public class HelloMessageLoader extends MessageLoader<InetSocketAddress> {

	private static ByteBuffer HELLO = ByteBuffer.wrap("JXTAHELLO ".getBytes());
	
	@Override
	public Message load(InetSocketAddress destinationAddress) throws IOException {

		SocketChannel socketChannel = SocketChannel.open(destinationAddress);
		if (!isJxtaPeer(socketChannel)) {
			throw new IOException("Not jxta peer");
		}

		ChannelUtils.blockUntilConnected(socketChannel, 2000);

		ByteBuffer buffer = ByteBuffer.allocate(512);
		StringBuilder message = new StringBuilder();

		while (socketChannel.read(buffer) != -1) {
			buffer.flip();
			message.append(new String(buffer.array(), "UTF-8"));
			buffer.clear();
		}

		try {
			HelloResponse response = HelloResponse.fromStringResponse(message
					.toString().trim().split("[ ]"));
			if (!response.persistMessage()) {
				throw new IOException("Unable to persist message ");
			}
			return response;
		} catch (URISyntaxException e) {
			throw new IOException("Invalid response: " + e.getMessage());
		} finally {
			socketChannel.close();
		}
	}

	private static boolean isJxtaPeer(SocketChannel socketChannel) {
		ByteBuffer c = ByteBuffer.allocate(10);
		try {
			socketChannel.read(c);
		} catch (IOException e) {
			return false;
		}
		c.flip();
		return c.equals(HELLO.duplicate());
	}
}
