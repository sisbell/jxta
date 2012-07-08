package org.i2peer.android.messages;

import java.net.URI;
import java.net.URISyntaxException;

import org.i2peer.android.Message;

public final class HelloResponse implements Message {

	private URI destinationUri;

	private URI sourceUri;

	private boolean isPropagated;

	private String protocolVersion;
	
	private String peerId;

	public HelloResponse() {
	}

	public HelloResponse(URI destinationUri, URI sourceUri, String peerId,
			boolean isPropagated, String protocolVersion) {
		super();	
		this.destinationUri = destinationUri;
		this.sourceUri = sourceUri;
		this.peerId = peerId;
		this.isPropagated = isPropagated;
		this.protocolVersion = protocolVersion;
	}

	public URI getDestinationUri() {
		return destinationUri;
	}

	public void setDestinationUri(URI destinationUri) {
		this.destinationUri = destinationUri;
	}

	public URI getSourceUri() {
		return sourceUri;
	}

	public void setSourceUri(URI sourceUri) {
		this.sourceUri = sourceUri;
	}

	public String getPeerId() {
		return peerId;
	}

	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}

	public boolean isPropagated() {
		return isPropagated;
	}

	public void setPropagated(boolean isPropagated) {
		this.isPropagated = isPropagated;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public static HelloResponse fromStringResponse(String... response)
			throws URISyntaxException {
		if (response.length != 5) {
			throw new IllegalArgumentException(
					"Wrong number of arguments: expected 5, found: " + response.length);
		}

		return new HelloResponse(new URI(response[0]), new URI(response[1]), response[2],
				Boolean.parseBoolean(response[3]), response[4]);
	}

	@Override
	public boolean persistMessage() {
		//Implementation specific storage
		//Just print out for now
		System.out.println("Dest = " + destinationUri + ", Src = " + sourceUri + ", ID = " + peerId);
		return true;
	}

}
