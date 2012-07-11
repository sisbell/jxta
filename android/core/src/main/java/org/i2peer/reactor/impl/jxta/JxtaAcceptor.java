package org.i2peer.reactor.impl.jxta;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import org.i2peer.android.messages.PingResponse;
import org.i2peer.android.network.ChannelUtils;
import org.i2peer.reactor.EventHandler;
import org.i2peer.reactor.InitiationDispatcher;

public class JxtaAcceptor implements EventHandler {

	private ServerSocketChannel channel;

	private InitiationDispatcher dispatcher;

	public JxtaAcceptor(InetSocketAddress address,
			InitiationDispatcher dispatcher) throws IOException {
		this.dispatcher = dispatcher;
		channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(address);
	}

	@Override
	public void handleEvent(int eventType) {
		System.out.println("Connected..........");
		try {
			SocketChannel socketChannel = channel.accept();
			socketChannel.configureBlocking(false);

			ChannelUtils.blockUntilConnected(socketChannel, 2000);
			
			writePingResponseTo(socketChannel);
			
			dispatcher.registerHandler(new JxtaEventHandler(socketChannel),
					SelectionKey.OP_READ);
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
		 * try { channel.socket().close(); } catch (IOException e) {
		 * e.printStackTrace(); }
		 */
	}

	@Override
	public SelectableChannel getChannel() {
		return channel;
	}

	private void writePingResponseTo(SocketChannel socketChannel)
			throws IOException, URISyntaxException {

		URI destination = new URI(socketChannel.socket()
				.getRemoteSocketAddress().toString());
		URI source = new URI(socketChannel.socket().getLocalSocketAddress()
				.toString());

		PingResponse r = new PingResponse(destination, source, "test-"
				+ UUID.randomUUID(), false, "4.0");
		String response = r.toStringResponse();

		ByteBuffer buffer = ByteBuffer.allocate(5000);
		buffer.clear();
		buffer.put(response.getBytes());
		buffer.flip();

		socketChannel.write(buffer);
	}
}
