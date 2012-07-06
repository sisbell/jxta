package org.i2peer;

import java.io.IOException;
import java.net.URI;

import net.jxta.exception.PeerGroupException;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.NetworkManager;

public class Booter {
	 protected static final transient URI publicSeedingRdvURI = URI.create("http://ec2-107-21-173-139.compute-1.amazonaws.com:9700");
	    protected static final transient URI publicSeedingRelayURI = URI.create("http://ec2-107-21-173-139.compute-1.amazonaws.com:9700");

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
			manager.getConfigurator().addRdvSeedingURI(publicSeedingRdvURI);
			manager.getConfigurator().addRelaySeedingURI(publicSeedingRelayURI);
			PeerGroup pg = manager.startNetwork();
			pg.getRendezVousService().setAutoStart(false);
			 if (manager.waitForRendezvousConnection(120000)) {
				 System.out.println("Found server");
	                
	            } else {
	            	System.out.println("Not Found server");
	               
	            }
		} catch (PeerGroupException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
