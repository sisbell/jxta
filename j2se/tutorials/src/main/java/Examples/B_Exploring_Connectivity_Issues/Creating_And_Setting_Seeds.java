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

package Examples.B_Exploring_Connectivity_Issues;

import Examples.Z_Tools_And_Others.Tools;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.RouteAdvertisement;

public class Creating_And_Setting_Seeds {
    
    public static final String Name = "Creating and setting seeds";
    public static final File ConfigurationFile = new File("." + System.getProperty("file.separator") + Name);
    
    public static void main(String[] args) {
        
        try {
            
            // Creation of the network manager
            NetworkManager MyNetworkManager = new NetworkManager(NetworkManager.ConfigMode.EDGE,
                    Name, ConfigurationFile.toURI());

            // Retrieving the network configurator
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();
            
            // Checking if RendezVous_Jack should be a seed
            MyNetworkConfigurator.clearRendezvousSeeds();

            // Creating an endpoint seed and setting it
            URI TheSeed = URI.create("tcp://33.44.55.66:9202");
            MyNetworkConfigurator.addSeedRendezvous(TheSeed);

            // Creating a document read by seeding URIs
            XMLDocument MyDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XML_DEFAULTENCODING, "jxta:seeds");
            MyDoc.addAttribute("ordered", "false");
            MyDoc.addAttribute("xmlns:jxta", "http://www.jxta.org");

            // First seed
            RouteAdvertisement MyRouteAdv = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());
            PeerID MyRDV = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, "My first RDV".getBytes());

            AccessPointAdvertisement MyAPA = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());
            MyAPA.addEndpointAddress(new EndpointAddress("tcp://18.22.1.69:3333"));

            MyRouteAdv.setDestPeerID(MyRDV);
            MyRouteAdv.setDest(MyAPA);

            XMLDocument MyRouteAdvDoc = (XMLDocument) MyRouteAdv.getDocument(MimeMediaType.XMLUTF8);
            Tools.copyElements(MyDoc, MyDoc.getRoot(), MyRouteAdvDoc.getRoot(), true, false);

            // Second seed
            RouteAdvertisement MyRouteAdv2 = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());
            PeerID MyRDV2 = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, "My second RDV".getBytes());

            AccessPointAdvertisement MyAPA2 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());
            MyAPA2.addEndpointAddress(new EndpointAddress("tcp://171.17.22.4:2876"));

            MyRouteAdv2.setDestPeerID(MyRDV2);
            MyRouteAdv2.setDest(MyAPA2);

            XMLDocument MyRouteAdvDoc2 = (XMLDocument) MyRouteAdv2.getDocument(MimeMediaType.APPLICATION_XML_DEFAULTENCODING);
            Tools.copyElements(MyDoc, MyDoc.getRoot(), MyRouteAdvDoc2.getRoot(), true, false);

            // Third seed
            RouteAdvertisement MyRouteAdv3 = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());
            PeerID MyRDV3 = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, "My third RDV".getBytes());

            AccessPointAdvertisement MyAPA3 = (AccessPointAdvertisement)
                AdvertisementFactory.newAdvertisement(AccessPointAdvertisement.getAdvertisementType());
            MyAPA3.addEndpointAddress(new EndpointAddress("tcp://240.28.16.57:4210"));

            MyRouteAdv3.setDestPeerID(MyRDV3);
            MyRouteAdv3.setDest(MyAPA3);

            XMLDocument MyRouteAdvDoc3 = (XMLDocument) MyRouteAdv3.getDocument(MimeMediaType.XMLUTF8);
            Tools.copyElements(MyDoc, MyDoc.getRoot(), MyRouteAdvDoc3.getRoot(), true, false);

            // Printing the result
            MyDoc.sendToStream(System.out);

        } catch (IOException Ex) {
            
            // Raised when access to local file and directories caused an error
            Tools.PopErrorMessage(Name, Ex.toString());
            
        }

    }

}