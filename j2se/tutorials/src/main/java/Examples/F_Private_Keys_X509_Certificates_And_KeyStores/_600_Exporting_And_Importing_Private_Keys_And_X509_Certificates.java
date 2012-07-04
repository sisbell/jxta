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
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import net.jxta.impl.membership.pse.PSEUtils;

public class _600_Exporting_And_Importing_Private_Keys_And_X509_Certificates {
    
    public static final String Name = "Example 600";

    public static void main(String[] args) {
        
        try {
        
            // Certificate and Private Key
            X509Certificate TheX509Certificate;
            PrivateKey ThePrivateKey;

            // Initialization
            PSEUtils.IssuerInfo ForPSE = PSEUtils.genCert(Name, null);
        
            TheX509Certificate = ForPSE.cert;
            ThePrivateKey = ForPSE.issuerPkey;
        
            // String encoded certificate & private key
            String Base64_X509Certificate = PSEUtils.base64Encode(TheX509Certificate.getEncoded());
            
            String Base64_ThePrivateKey = PSEUtils.base64Encode(ThePrivateKey.getEncoded());
                    
            // Printing Results
            System.out.println("------------------------------");
            System.out.println(Base64_X509Certificate);
            System.out.println("------------------------------");
            System.out.println(Base64_ThePrivateKey);
            System.out.println(ThePrivateKey.getFormat());
            System.out.println("------------------------------");
            
            // Recreating certificate & private key
            X509Certificate RecreateX509Certificate;
            PrivateKey RecreatePrivateKey;
            
            // Recreating the X509 certificate
            byte[] Temp = PSEUtils.base64Decode(new StringReader(Base64_X509Certificate));

            CertificateFactory TheCF = CertificateFactory.getInstance("X509");
            RecreateX509Certificate = (X509Certificate) TheCF.generateCertificate(new ByteArrayInputStream(Temp));
            
            System.out.println("-X509-Original-------------");
            System.out.println(TheX509Certificate.toString());
            System.out.println("-X509-Recreated------------");
            System.out.println(RecreateX509Certificate.toString());
            System.out.println("---------------------------");
            
            // Restoring the private key
            Temp = PSEUtils.base64Decode(new StringReader(Base64_ThePrivateKey));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec MyPKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(Temp);
            RecreatePrivateKey = keyFactory.generatePrivate(MyPKCS8EncodedKeySpec);
            
            System.out.println("-Private-Key-Original-------------");
            System.out.println(ThePrivateKey.toString());
            System.out.println("-Private-Key-Recreated------------");
            System.out.println(RecreatePrivateKey.toString());
            System.out.println("----------------------------------");

        } catch (Exception Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        }

    }

}