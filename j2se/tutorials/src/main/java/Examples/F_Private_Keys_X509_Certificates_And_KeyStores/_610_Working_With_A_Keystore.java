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

package Examples.F_Private_Keys_X509_Certificates_And_KeyStores;

import Examples.Z_Tools_And_Others.Tools;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.FileKeyStoreManager;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;

public class _610_Working_With_A_Keystore {
    
    public static final String Name = "Example 610";
    public static final PeerID PID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, Name.getBytes());
    public static final File ConfigurationFile = new File("." + System.getProperty("file.separator") + Name);
    
    public static final String MyPrincipalName = "Principal - " + Name;
    public static final String MyPrivateKeyPassword = "PrivateKey Password - " + Name;

    public static final String MyKeyStoreFileName = "MyKeyStoreFile";
    public static final String MyKeyStoreLocation = "." + System.getProperty("file.separator") + Name + File.separator + "MyKeyStoreLocation";
    public static final String MyKeyStorePassword = "KeyStore Password - " + Name;
    public static final String MyKeyStoreProvider = "KeyStore Provider - " + Name;

    public static final File MyKeyStoreDirectory = new File(MyKeyStoreLocation);
    public static final File MyKeyStoreFile = new File(MyKeyStoreLocation + File.separator + MyKeyStoreFileName);

    public static final X509Certificate TheX509Certificate;
    public static final PrivateKey ThePrivateKey;
    
    static {
        
        // Static initialization
        PSEUtils.IssuerInfo ForPSE = PSEUtils.genCert(Name, null);
        
        TheX509Certificate = ForPSE.cert;
        ThePrivateKey = ForPSE.issuerPkey;
        
    }

    public static void main(String[] args) {
        
        try {
            
            // Removing any existing configuration?
            Tools.CheckForExistingConfigurationDeletion(Name, ConfigurationFile);
            
            // Preparing data
            MyKeyStoreDirectory.mkdirs();
            
            // Creating the key store
            FileKeyStoreManager MyFileKeyStoreManager = new FileKeyStoreManager(
                (String)null, MyKeyStoreProvider, MyKeyStoreFile);
            
            MyFileKeyStoreManager.createKeyStore(MyKeyStorePassword.toCharArray());
            
            if (!MyFileKeyStoreManager.isInitialized()) {
                Tools.PopInformationMessage(Name, "Keystore is NOT initialized");
            } else {
                Tools.PopInformationMessage(Name, "Keystore is initialized");
            }
            
            // Loading the (empty) keystore 
            KeyStore MyKeyStore = MyFileKeyStoreManager.loadKeyStore(MyKeyStorePassword.toCharArray());
            
            // Setting data
            X509Certificate[] Temp = { TheX509Certificate };
            MyKeyStore.setKeyEntry(PID.toString(), ThePrivateKey, MyPrivateKeyPassword.toCharArray(), Temp);
            
            // Saving the data
            MyFileKeyStoreManager.saveKeyStore(MyKeyStore, MyKeyStorePassword.toCharArray());
            
            // Reloading the KeyStore
            MyKeyStore = MyFileKeyStoreManager.loadKeyStore(MyKeyStorePassword.toCharArray());
            
            // Retrieving Certificate
            X509Certificate MyCertificate = (X509Certificate) MyKeyStore.getCertificate(PID.toString());
            
            if (MyCertificate==null) {
                Tools.PopInformationMessage(Name, "X509 Certificate CANNOT be retrieved");
            } else {
                Tools.PopInformationMessage(Name, "X509 Certificate can be retrieved");
                System.out.println(MyCertificate.toString());
            }

            // Retrieving private key 
            PrivateKey MyPrivateKey = (PrivateKey) MyKeyStore.getKey(PID.toString(), MyPrivateKeyPassword.toCharArray());
            
            if (MyPrivateKey==null) {
                Tools.PopInformationMessage(Name, "Private key CANNOT be retrieved");
            } else {
                Tools.PopInformationMessage(Name, "Private key can be retrieved");
                System.out.println(MyPrivateKey.toString());
            }

        } catch (NoSuchAlgorithmException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        } catch (UnrecoverableKeyException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        } catch (NoSuchProviderException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        } catch (KeyStoreException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        } catch (IOException Ex) {
            
            // Raised when access to local file and directories caused an error
            Tools.PopErrorMessage(Name, Ex.toString());
            
        }

    }

}