package org.i2peer.android.jxta;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.i2peer.android.Message;
import org.i2peer.android.impl.DefaultMessageLoader;
import org.i2peer.android.messages.PingResponse;
import org.i2peer.android.network.ChannelUtils;
import org.i2peer.android.network.PingMessageLoader;

public class JxtaPingMessageLoader extends DefaultMessageLoader<InetSocketAddress>
		implements PingMessageLoader {
	private static ByteBuffer HELLO = ByteBuffer.wrap("JXTAHELLO ".getBytes());

	@Override
	public Message load(InetSocketAddress destinationAddress)
			throws IOException {

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
			PingResponse response = PingResponse.fromStringResponse(message
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
