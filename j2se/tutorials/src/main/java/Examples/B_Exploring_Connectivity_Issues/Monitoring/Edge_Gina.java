
package Examples.B_Exploring_Connectivity_Issues.Monitoring;

import Examples.Z_Tools_And_Others.Tools;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.swing.JOptionPane;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

/**
 * Simple EDGE peer connecting via the NetPeerGroup.
 */
public class Edge_Gina {

    // Static

    public static final String Name_EDGE = "EDGE GINA";
    public static final PeerID PID_EDGE = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, Name_EDGE.getBytes());
    public static final int TcpPort_EDGE = 9710;
    public static final int HttpPort_EDGE = 9900;
    public static final File ConfigurationFile_EDGE = new File("." + System.getProperty("file.separator") + Name_EDGE);

    public static final String ChildPeerGroupName = "Child peer group";
    public static final PeerGroupID ChildPeerGroupID = IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID, ChildPeerGroupName.getBytes());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {

            // Removing any existing configuration?
            NetworkManager.RecursiveDelete(ConfigurationFile_EDGE);

            System.out.println(PeerGroup.tcpProtoClassID);

            // Creation of the network manager
            final NetworkManager MyNetworkManager = new NetworkManager(
                    NetworkManager.ConfigMode.EDGE,
                    Name_EDGE, ConfigurationFile_EDGE.toURI());

            // Retrieving the network configurator
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();

            // Setting Configuration
            MyNetworkConfigurator.setUseMulticast(false);

            MyNetworkConfigurator.setTcpPort(TcpPort_EDGE);

            if ( Tools.PopYesNoQuestion(Name_EDGE, "Do you want to enable TCP?") == JOptionPane.YES_OPTION ) {

                MyNetworkConfigurator.setTcpEnabled(true);
                MyNetworkConfigurator.setTcpIncoming(true);
                MyNetworkConfigurator.setTcpOutgoing(true);

            } else {

                MyNetworkConfigurator.setTcpEnabled(false);
                MyNetworkConfigurator.setTcpIncoming(false);
                MyNetworkConfigurator.setTcpOutgoing(false);

            }

            MyNetworkConfigurator.setHttpPort(TcpPort_EDGE);

            if ( Tools.PopYesNoQuestion(Name_EDGE, "Do you want to enable HTTP?") == JOptionPane.YES_OPTION ) {

                MyNetworkConfigurator.setHttpEnabled(true);
                MyNetworkConfigurator.setHttpIncoming(false);
                MyNetworkConfigurator.setHttpOutgoing(true);

            } else {

                MyNetworkConfigurator.setHttpEnabled(false);
                MyNetworkConfigurator.setHttpIncoming(false);
                MyNetworkConfigurator.setHttpOutgoing(false);

            }

            // Setting the Peer ID
            MyNetworkConfigurator.setPeerID(PID_EDGE);

            // Adding RDV seed
            MyNetworkConfigurator.clearRendezvousSeeds();

            String TheRdvSeed = "tcp://" + InetAddress.getLocalHost().getHostAddress() + ":"
                    + RendezVous_Mya.TcpPort_RDV;
            URI RendezVousSeedURI = URI.create(TheRdvSeed);
            MyNetworkConfigurator.addSeedRendezvous(RendezVousSeedURI);

            // Adding Relay seed
            MyNetworkConfigurator.clearRelaySeeds();

            String TheRelaySeed = "http://" + InetAddress.getLocalHost().getHostAddress() + ":"
                    + Relay_Robert.HttpPort_RELAY;
            URI RelaySeedURI = URI.create(TheRelaySeed);
            MyNetworkConfigurator.addSeedRelay(RelaySeedURI);

            String TheRelaySeed2 = "tcp://" + InetAddress.getLocalHost().getHostAddress() + ":"
                    + Relay_Robert.TcpPort_RELAY;
            URI RelaySeedURI2 = URI.create(TheRelaySeed2);
            MyNetworkConfigurator.addSeedRelay(RelaySeedURI2);

            // Starting the JXTA network
            PeerGroup NetPeerGroup = MyNetworkManager.startNetwork();

            // Starting the connectivity monitor
            new ConnectivityMonitor(NetPeerGroup);

            // Disabling any rendezvous autostart
            NetPeerGroup.getRendezVousService().setAutoStart(false);

            // Stopping the network asynchronously
            ConnectivityMonitor.TheExecutor.schedule(
                new DelayedJxtaNetworkStopper(
                    MyNetworkManager,
                    "Click to stop " + Name_EDGE,
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
