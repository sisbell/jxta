
package Examples.B_Exploring_Connectivity_Issues.Monitoring;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

/**
 * Simple RELAY peer.
 */
public class Relay_Robert {

    // Static

    public static final String Name_RELAY = "RELAY";
    public static final PeerID PID_RELAY = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, Name_RELAY.getBytes());
    public static final int HttpPort_RELAY = 9900;
    public static final int TcpPort_RELAY = 9715;
    public static final File ConfigurationFile_RELAY = new File("." + System.getProperty("file.separator") + Name_RELAY);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {

            // Removing any existing configuration?
            NetworkManager.RecursiveDelete(ConfigurationFile_RELAY);

            // Creation of the network manager
            final NetworkManager MyNetworkManager = new NetworkManager(
                    NetworkManager.ConfigMode.RELAY,
                    Name_RELAY, ConfigurationFile_RELAY.toURI());

            // Retrieving the network configurator
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();

            // Setting Configuration
            MyNetworkConfigurator.setUseMulticast(false);

            MyNetworkConfigurator.setTcpPort(TcpPort_RELAY);
            MyNetworkConfigurator.setTcpEnabled(true);
            MyNetworkConfigurator.setTcpIncoming(true);
            MyNetworkConfigurator.setTcpOutgoing(true);

            MyNetworkConfigurator.setHttpPort(HttpPort_RELAY);
            MyNetworkConfigurator.setHttpEnabled(true);
            MyNetworkConfigurator.setHttpIncoming(true);
            MyNetworkConfigurator.setHttpOutgoing(true);

            // Setting the Peer ID
            MyNetworkConfigurator.setPeerID(PID_RELAY);

            // Starting the JXTA network
            PeerGroup NetPeerGroup = MyNetworkManager.startNetwork();

            // Starting the connectivity monitor
            new ConnectivityMonitor(NetPeerGroup);

            // Stopping the network asynchronously
            ConnectivityMonitor.TheExecutor.schedule(
                new DelayedJxtaNetworkStopper(
                    MyNetworkManager,
                    "Click to stop " + Name_RELAY,
                    "Stop"),
                0,
                TimeUnit.SECONDS);

        } catch (IOException Ex) {

            System.err.println(Ex.toString());

        } catch (PeerGroupException Ex) {

            System.err.println(Ex.toString());

        }

    }

}
