/*
 * Copyright (c) 2010 DawningStreams, Inc.  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by DawningStreams, Inc." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The name "DawningStreams,Inc." must not be used to endorse or promote
 *     products derived from this software without prior written permission.
 *     For written permission, please contact DawningStreams,Inc. at 
 *     http://www.dawningstreams.com.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 *  DAWNINGSTREAMS, INC OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  DawningStreams is a registered trademark of DawningStreams, Inc. in the United 
 *  States and other countries.
 *  
 */

package Examples.D_Discovering_Resources;

import Examples.B_Exploring_Connectivity_Issues.RendezVous_Jack;
import Examples.Z_Tools_And_Others.Tools;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.impl.protocol.RdvAdv;
import net.jxta.impl.protocol.RouteAdv;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

public class _300_Retrieving_And_Flushing_Local_Advertisements_Example {
    
    public static final String Name = "Example 300";
    
    public static final int TcpPort = 9720;
    public static final PeerID PID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, Name.getBytes());
    public static final File ConfigurationFile = new File("." + System.getProperty("file.separator") + Name);
    
    public static void main(String[] args) {
        
        try {
            
            // Removing any existing configuration?
            Tools.CheckForExistingConfigurationDeletion(Name, ConfigurationFile);

            // Preparing context
            NetworkManager MyNetworkManager = new NetworkManager(NetworkManager.ConfigMode.EDGE,
                    Name);
            
            // Retrieving network configurator
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();
            
            // Checking if RendezVous_Jack should be a seed
            MyNetworkConfigurator.clearRendezvousSeeds();
            String TheSeed = "tcp://" + InetAddress.getLocalHost().getHostAddress() + ":" + RendezVous_Jack.TcpPort;
            Tools.CheckForRendezVousSeedAddition(Name, TheSeed, MyNetworkConfigurator);

            // Setting Configuration
            MyNetworkConfigurator.setTcpPort(TcpPort);
            MyNetworkConfigurator.setTcpEnabled(true);
            MyNetworkConfigurator.setTcpIncoming(true);
            MyNetworkConfigurator.setTcpOutgoing(true);
            
            // Setting the Peer ID
            Tools.PopInformationMessage(Name, "Setting the peer ID to :\n\n" + PID.toString());
            MyNetworkConfigurator.setPeerID(PID);

            // Starting the network and waiting for a rendezvous connection
            PeerGroup DefaultPeerGroup = MyNetworkManager.startNetwork();
            
            Tools.PopInformationMessage(Name, "Waiting for rendezvous connection for maximum 60 seconds");
            if (MyNetworkManager.waitForRendezvousConnection(60000)) {

                Tools.PopInformationMessage(Name, "Rendezvous connection successful!");

            } else {

                Tools.PopWarningMessage(Name, "Rendezvous connection NOT successful!");

            }
            
            // Retrieving local advertisements
            Tools.PopInformationMessage(Name, "Retrieving local advertisements");
            DiscoveryService TheDiscoveryService = DefaultPeerGroup.getDiscoveryService();

            Enumeration<Advertisement> TheAdvEnum = TheDiscoveryService.getLocalAdvertisements(DiscoveryService.ADV, null, null);
            
            while (TheAdvEnum.hasMoreElements()) { 
                
                Advertisement TheAdv = TheAdvEnum.nextElement();
                
                String ToDisplay = "Found " + TheAdv.getClass().getSimpleName();
                
                if (TheAdv.getClass().getName().compareTo(RouteAdv.class.getName())==0) {
                    
                    // We found a route advertisement
                    RouteAdv Temp = (RouteAdv) TheAdv;
                    ToDisplay = ToDisplay + "\n\nto " + Temp.getDestPeerID().toString();
                    
                } else if (TheAdv.getClass().getName().compareTo(RdvAdv.class.getName())==0) {
                    
                    // We found a rendezvous advertisement
                    RdvAdv Temp = (RdvAdv) TheAdv;
                    ToDisplay = ToDisplay + "\n\nof " + Temp.getPeerID().toString();
                    
                }
                
                // Displaying the advertisement
                Tools.PopInformationMessage(Name, ToDisplay);

                // Flushing advertisement
                TheDiscoveryService.flushAdvertisement(TheAdv);
                        
            }
            
            // Stopping JXTA
            Tools.PopInformationMessage(Name, "Stopping the network");
            MyNetworkManager.stopNetwork();
            
        } catch (IOException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        } catch (PeerGroupException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        }

    }
        
}
