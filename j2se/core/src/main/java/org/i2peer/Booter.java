package org.i2peer;

import java.io.IOException;

import net.jxta.exception.PeerGroupException;
import net.jxta.platform.NetworkManager;

public class Booter {

	public static void main(String[] args) {

		try {
			final NetworkManager manager = new NetworkManager(
					NetworkManager.ConfigMode.SUPER, "i2peer");
			Runtime.getRuntime().addShutdownHook(new Thread() {

				@Override
				public void run() {
					super.run();
					manager.stopNetwork();
				}
			});
			manager.startNetwork();
		} catch (PeerGroupException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
