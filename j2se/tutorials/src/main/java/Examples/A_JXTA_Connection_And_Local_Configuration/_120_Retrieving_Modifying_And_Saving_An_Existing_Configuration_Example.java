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
import java.io.File;
import java.io.IOException;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

public class _120_Retrieving_Modifying_And_Saving_An_Existing_Configuration_Example {

    public static final String Name = "Example 120";

    public static void main(String[] args) {

        try {
            
            // Preparing the configuration storage location
            String LocalPath = "." + System.getProperty("file.separator") + "MyPath";
            File ConfigurationFile = new File(LocalPath);

            // Creation of the network manager
            NetworkManager MyNetworkManager = new NetworkManager(
                    NetworkManager.ConfigMode.EDGE,
                    Name,
                    ConfigurationFile.toURI());
            
            // Checking for configuration existence
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();
            
            if (MyNetworkConfigurator.exists()) {
                
                // Found existing configuration
                Tools.PopInformationMessage(Name, "Found existing configuration");
                
            } else {

                // No configuration found
                Tools.PopInformationMessage(Name, "No configuration found at:\n\n"
                    + ConfigurationFile.getCanonicalPath());
                
            }
                
            // Modifying new name
            String NewName = "My new name @ " + System.currentTimeMillis();
            Tools.PopInformationMessage(Name, "Setting new name to: " + NewName);
            MyNetworkConfigurator.setName(NewName);
                
            // Saving modifications
            Tools.PopInformationMessage(Name, "Saving configuration at:\n\n"
                    + ConfigurationFile.getCanonicalPath());
            MyNetworkConfigurator.save();
            
        } catch (IOException Ex) {
            
            // Raised when access to local file and directories caused an error
            Tools.PopErrorMessage(Name, Ex.toString());            
            
        }

    }
        
}
