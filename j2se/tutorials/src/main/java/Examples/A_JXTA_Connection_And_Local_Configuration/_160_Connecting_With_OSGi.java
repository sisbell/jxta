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

package Examples.A_JXTA_Connection_And_Local_Configuration;

import Examples.Z_Tools_And_Others.Tools;
import java.io.IOException;
import net.jxse.OSGi.JxseOSGiFramework;
import net.jxse.OSGi.Services.JxseOSGiNetworkManagerService;
import net.jxse.configuration.JxsePeerConfiguration;
import net.jxse.configuration.JxsePeerConfiguration.ConnectionMode;
import net.jxta.configuration.JxtaConfigurationException;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkManager;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.util.tracker.ServiceTracker;

public class _160_Connecting_With_OSGi {
    
    public static final String Name = "Example 160";
    
    // The NetworkManager service instance
    private static JxseOSGiNetworkManagerService TheNMS;

    // The OSGi service tracker for the NetworkManager Service
    private static ServiceTracker ST;

    public static void main(String[] args) {

        try {

            // Starting the OSGi framwork (Felix)
            JxseOSGiFramework.INSTANCE.start();

            // Retrieving the NetworkManager service
            ST = JxseOSGiFramework.getServiceTracker(JxseOSGiNetworkManagerService.class);

            // Starting to track the service
            ST.open();

            // Retrieving the NetworkManager service for at most 5 seconds
            TheNMS = (JxseOSGiNetworkManagerService) ST.waitForService(5000);

            if (TheNMS==null) {
                Tools.PopErrorMessage(Name, "Could not retrieve the NetworkManager "
                    + "service within 5 seconds");
                System.exit(-1);
            }

            // Creating a peer configuration
            JxsePeerConfiguration MyConfig = new JxsePeerConfiguration();
            MyConfig.setConnectionMode(ConnectionMode.ADHOC);
            MyConfig.setPeerID(IDFactory.newPeerID(PeerGroupID.worldPeerGroupID));
            MyConfig.setPeerInstanceName("Poupoudipou");
            
            // Setting the configuration in the NetworkManager OSGi service
            TheNMS.setPeerConfiguration(MyConfig);

            // Retrieving a configured network manager
            NetworkManager MyNM = TheNMS.getConfiguredNetworkManager();

            // Starting and stopping the network
            MyNM.startNetwork();
            MyNM.stopNetwork();

            // Stopping the service tracking
            ST.close();
            TheNMS = null;

            // Stopping the OSGI framework
            JxseOSGiFramework.INSTANCE.stop();

            // Waiting for stop for maximum 20 seconds
            FrameworkEvent FE = JxseOSGiFramework.INSTANCE.waitForStop(20000);

            // Checking whether we stopped properly
            if ( FE.getType() != FrameworkEvent.STOPPED ) {
                Tools.PopErrorMessage(Name, "OSGi framework failed to stop after 20 seconds, event type: " + FE.getType() );
            } 

        } catch (PeerGroupException ex) {

            Tools.PopErrorMessage(Name, ex.toString());

        } catch (JxtaConfigurationException ex) {

            Tools.PopErrorMessage(Name, ex.toString());

        } catch (InterruptedException ex) {

            Tools.PopErrorMessage(Name, ex.toString());

        } catch (BundleException ex) {

            Tools.PopErrorMessage(Name, ex.toString());

        } catch (IOException ex) {

            Tools.PopErrorMessage(Name, ex.toString());

        } catch (Exception ex) {

            Tools.PopErrorMessage(Name, ex.toString());

        }
    }
        
}
