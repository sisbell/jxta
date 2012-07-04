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

package Examples.Z_Tools_And_Others;

import java.io.File;
import net.jxta.impl.membership.pse.FileKeyStoreManager;
import net.jxta.platform.NetworkManager;

public class Keystore_Creation_Example {
    
    public static void main(String args[]) throws Throwable {
        
        try {
            
            // Preparing data
            String MyKeyStoreFileName = "MyKeyStoreFile";
            String MyKeyStoreLocation = "." + File.separator + "MyKeyStoreLocation";
            String MyKeyStorePassword = "My Key Store Password";
            String MyKeyStoreProvider = "Me Myself And I";
            
            File MyKeyStoreDirectory = new File(MyKeyStoreLocation);
            File MyKeyStoreFile = new File(MyKeyStoreLocation + File.separator
                    + MyKeyStoreFileName);
            
            // Deleting any existing key store and content
            NetworkManager.RecursiveDelete(MyKeyStoreDirectory);
            MyKeyStoreDirectory.mkdirs();
            
            // Creating the key store
            FileKeyStoreManager MyFileKeyStoreManager = new FileKeyStoreManager(
                    (String)null, MyKeyStoreProvider, MyKeyStoreFile);
            
            MyFileKeyStoreManager.createKeyStore(MyKeyStorePassword.toCharArray());
            
            // Checking initialization
            if (MyFileKeyStoreManager.isInitialized()) {
                
                System.out.println("Keystore initialized successfully");
                
            } else {
                
                System.out.println("Keystore NOT initialized successfully");
                
            }
            
        } catch (Exception Ex) {
            
            Ex.printStackTrace();
            
        }
        
    }

    public Keystore_Creation_Example() {

    }

}
