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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.jxse.configuration.JxseHttpTransportConfiguration;
import net.jxse.configuration.JxsePeerConfiguration;
import net.jxse.configuration.JxseTcpTransportConfiguration;

public class _150_Configuration_Objects {
    
    public static final String Name = "Example 150";
    
    public static void main(String[] args) {

        try {
            
            // Creating a default configuration object
            JxsePeerConfiguration MyConfig = new JxsePeerConfiguration();
            JxseHttpTransportConfiguration MyHttpConfig = MyConfig.getHttpTransportConfiguration();
            JxseTcpTransportConfiguration MyTcpConfig = MyConfig.getTcpTransportConfiguration();

            // Setting some configuration information
            MyConfig.setPeerInstanceName("Config Object Test");
            MyHttpConfig.setHttpIncoming(true);
            MyTcpConfig.setTcpIncoming(true);

            // DON'T FORGET TO SET BACK THE TRANSPORT CONFIGURATION
            MyConfig.setHttpTransportConfiguration(MyHttpConfig);
            MyConfig.setTcpTransportConfiguration(MyTcpConfig);

            // Saving the configuration
            ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
            MyConfig.storeToXML(BAOS, "My saving");
            System.out.println(BAOS.toString());

            // Reading the configuration
            ByteArrayInputStream BAIS = new ByteArrayInputStream(BAOS.toByteArray());
            JxsePeerConfiguration MyReadConfig = new JxsePeerConfiguration();
            MyReadConfig.loadFromXML(BAIS);

            // Checking
            Tools.PopInformationMessage(Name, "The configuration name is: "
                    + MyReadConfig.getPeerInstanceName());

            JxseHttpTransportConfiguration MyReadHttpConfig = MyReadConfig.getHttpTransportConfiguration();
            JxseTcpTransportConfiguration MyReadTcpConfig = MyReadConfig.getTcpTransportConfiguration();

            Tools.PopInformationMessage(Name, "HTTP Incoming is: "
                + Boolean.valueOf(MyReadHttpConfig.getHttpIncoming()));

            Tools.PopInformationMessage(Name, "TCP Incoming is: "
                + Boolean.valueOf(MyReadTcpConfig.getTcpIncoming()));

        } catch (IOException ex) {

            // Unexpected error
            Tools.PopErrorMessage(Name, ex.toString());

        }

    }
        
}
