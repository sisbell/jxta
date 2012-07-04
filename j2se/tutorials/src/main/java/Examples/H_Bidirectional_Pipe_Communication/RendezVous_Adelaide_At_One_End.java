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

package Examples.H_Bidirectional_Pipe_Communication;

import Examples.Z_Tools_And_Others.Tools;
import java.io.File;
import java.io.IOException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;

public class RendezVous_Adelaide_At_One_End implements PipeMsgListener {
    
    // Static attributes
    public static final String Name = "RendezVous Adelaide, at one end";
    public static final int TcpPort = 9726;
    public static final PeerID PID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, Name.getBytes());
    public static final File ConfigurationFile = new File("." + System.getProperty("file.separator") + Name);
    
    public void pipeMsgEvent(PipeMsgEvent PME) {
        
        // We received a message
        Message ReceivedMessage = PME.getMessage();
        String TheText = ReceivedMessage.getMessageElement("DummyNameSpace", "HelloElement").toString();

        // Notifying the user
        Tools.PopInformationMessage(Name, "Received message:\n\n" + TheText);
        
    }

    public static PipeAdvertisement GetPipeAdvertisement() {
        
        // Creating a Pipe Advertisement
        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID MyPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, Name.getBytes());

        MyPipeAdvertisement.setPipeID(MyPipeID);
        MyPipeAdvertisement.setType(PipeService.UnicastType);
        MyPipeAdvertisement.setName("Test BidiPipe");
        MyPipeAdvertisement.setDescription("Created by " + Name);
        
        return MyPipeAdvertisement;
        
    }
    
    public static void main(String[] args) {
        
        try {
            
            // Removing any existing configuration?
            Tools.CheckForExistingConfigurationDeletion(Name, ConfigurationFile);
            
            // Creation of network manager
            NetworkManager MyNetworkManager = new NetworkManager(NetworkManager.ConfigMode.RENDEZVOUS,
                    Name, ConfigurationFile.toURI());
            
            // Retrieving the network configurator
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();
            
            // Setting more configuration
            MyNetworkConfigurator.setTcpPort(TcpPort);
            MyNetworkConfigurator.setTcpEnabled(true);
            MyNetworkConfigurator.setTcpIncoming(true);
            MyNetworkConfigurator.setTcpOutgoing(true);

            // Setting the Peer ID
            Tools.PopInformationMessage(Name, "Setting the peer ID to :\n\n" + PID.toString());
            MyNetworkConfigurator.setPeerID(PID);

            // Starting the JXTA network
            Tools.PopInformationMessage(Name, "Start the JXTA network");
            PeerGroup NetPeerGroup = MyNetworkManager.startNetwork();
            
            // Waiting for other peers to connect to JXTA
            Tools.PopInformationMessage(Name, "Waiting for other peers to connect to JXTA");

            // Preparing the listener and creating the BiDiPipe
            PipeMsgListener MyListener = new RendezVous_Adelaide_At_One_End();
            JxtaServerPipe MyBiDiPipeServer = new JxtaServerPipe(NetPeerGroup, GetPipeAdvertisement());
            Tools.PopInformationMessage(Name, "Bidirectional pipe server created!");
            MyBiDiPipeServer.setPipeTimeout(30000);
            
            JxtaBiDiPipe MyBiDiPipe = MyBiDiPipeServer.accept();
            
            if (MyBiDiPipe != null) {
            
                MyBiDiPipe.setMessageListener(MyListener);
                Tools.PopInformationMessage(Name, "Bidirectional pipe connection established!");

                // Sending a hello message !!!
                Message MyMessage = new Message();
                StringMessageElement MyStringMessageElement = new StringMessageElement("HelloElement", "Hello from " + Name, null);
                MyMessage.addMessageElement("DummyNameSpace", MyStringMessageElement);

                MyBiDiPipe.sendMessage(MyMessage);

                // Sleeping for 10 seconds
                Tools.GoToSleep(10000);

                // Sending a goodbye message !!!
                MyMessage = new Message();
                MyStringMessageElement = new StringMessageElement("HelloElement", "Goodbye from " + Name, null);
                MyMessage.addMessageElement("DummyNameSpace", MyStringMessageElement);

                MyBiDiPipe.sendMessage(MyMessage);

                // Sleeping for 10 seconds
                Tools.GoToSleep(10000);
                
                // Closing the bidi pipe
                MyBiDiPipe.close();

            }
            
            // Closing the bidi pipe server
            MyBiDiPipe.close();
            
            // Retrieving connected peers
            Tools.popConnectedPeers(NetPeerGroup.getRendezVousService(), Name);

            // Stopping the network
            Tools.PopInformationMessage(Name, "Stop the JXTA network");
            MyNetworkManager.stopNetwork();
            
        } catch (IOException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        } catch (PeerGroupException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        }

    }

}