/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: dumpcred.java,v 1.4 2007/02/09 23:12:41 hamada Exp $
 */

package net.jxta.impl.shell.bin.pse;

import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.impl.util.BASE64OutputStream;
import net.jxta.membership.MembershipService;
import net.jxta.protocol.ModuleImplAdvertisement;

import javax.crypto.EncryptedPrivateKeyInfo;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * PSE.dumpcred
 */
public class dumpcred extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        ShellEnv env = getEnv();
        String credEnvName;
        boolean showBASE64 = false;
        boolean showPrivate = false;
        char[] privatePassword = null;

        GetOpt options = new GetOpt(argv, 0, "apx:");

        while (true) {
            int option;
            try {
                option = options.getNextOption();
            } catch (IllegalArgumentException badopt) {
                consoleMessage("Illegal argument :" + badopt);
                return syntaxError();
            }

            if (-1 == option) {
                break;
            }

            switch (option) {
                case'a':
                    showBASE64 = true;
                    break;

                case'p':
                    showPrivate = true;
                    break;

                case'x':
                    privatePassword = options.getOptionArg().toCharArray();
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        credEnvName = options.getNextParameter();

        if (null == credEnvName) {
            consoleMessage("Missing <cred> parameter");
            return syntaxError();
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        MembershipService membership = getGroup().getMembershipService();

        if (!(membership instanceof PSEMembershipService)) {
            ModuleImplAdvertisement mia = (ModuleImplAdvertisement) membership.getImplAdvertisement();

            consoleMessage("Group membership service is not PSE. (" + mia.getDescription() + ")");
            return ShellApp.appMiscError;
        }

        PSEMembershipService pse = (PSEMembershipService) membership;

        if (null == pse.getDefaultCredential()) {
            consoleMessage("Key store has not been opened.");
            return ShellApp.appMiscError;
        }

        try {
            ShellObject credEnv = env.get(credEnvName);

            if (null == credEnv) {
                consoleMessage("Environment variable '" + credEnvName + "' not found.");
                return ShellApp.appMiscError;
            }

            if (!PSECredential.class.isAssignableFrom(credEnv.getObjectClass())) {
                consoleMessage("'" + credEnvName + "' is not a  is not a PSE credential.");
                return ShellApp.appMiscError;
            }

            println("Certificate:");

            PSECredential cred = (PSECredential) credEnv.getObject();

            X509Certificate cert = cred.getCertificate();

            byte encoded[] = cert.getEncoded();

            if (showBASE64) {
                StringWriter base64 = new StringWriter();
                OutputStream out = new BASE64OutputStream(base64, 72);

                out.write(encoded);
                out.close();
                base64.close();

                println(base64.toString());
            } else {
                String hex = toHex(encoded, 0, encoded.length);

                println(hex);
            }

            if (showPrivate) {
                PrivateKey key = null;

                println("Private Key:");

                try {
                    key = cred.getPrivateKey();
                } catch (IllegalStateException notLocal) {
                    //ignored
                }

                if (null == key) {
                    consoleMessage("Credential is not a local login credential.");
                    return ShellApp.appMiscError;
                }

                if (null == privatePassword) {
                    encoded = key.getEncoded();
                } else {
                    EncryptedPrivateKeyInfo encrypted = PSEUtils.pkcs5_Encrypt_pbePrivateKey(privatePassword, key, 10000);

                    encoded = encrypted.getEncoded();
                }

                if (showBASE64) {
                    StringWriter base64 = new StringWriter();
                    OutputStream out = new BASE64OutputStream(base64, 72);

                    out.write(encoded);
                    out.close();
                    base64.close();

                    println(base64.toString());
                } else {
                    String hex = toHex(encoded, 0, encoded.length);

                    println(hex);
                }
            }
        } catch (Exception failure) {
            printStackTrace("Failure while recovering certificate", failure);
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    private static final char hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Do a hex dump of a byte array
     */
    private static String toHex(byte[] arr, int offset, int len) {
        StringBuilder str = new StringBuilder((3 * len) + (9 * (len / 16)));

        for (int i = offset; i < len; i++) {
            if ((i % 16) == 0) {
                String address = "00000000" + Integer.toHexString(i);

                str.append(address.substring(address.length() - 8)).append(": ");
            }
            str.append(hex[(arr[i] >> 4) & 15]);
            str.append(hex[arr[i] & 15]);
            if (15 != (i % 16)) {
                str.append(' ');
            } else {
                str.append('\n');
            }
        }
        return str.toString();
    }

    private int syntaxError() {
        consoleMessage("Usage: pse.dumpcred <cred>");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Dumps a credential.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     pse.dumpcred  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     pse.dumpcred [-a] [-p [-x <password>]] <cred>");
        println(" ");
        println("     <cred>    The credential to be printed.");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("      [-a]              Print in BASE64 format.");
        println("      [-p]              Print the private key.");
        println("      [-x <password>]]  Encrpyt the private key using <password>. ");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Dumps a credential object.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> pse.dumpcred cred0");
        println(" ");
        println(" ");
        println("SEE ALSO");
        println("     pse.certs pse.keys pse.erase pse.createkey login");
    }
}
