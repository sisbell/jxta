/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc.  All rights reserved.
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
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
 * $Id: Launch.java,v 1.4 2007/01/11 18:55:09 bondolo Exp $
 */

import java.io.File;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.platform.ModuleSpecID;
import net.jxta.credential.Credential;
import net.jxta.membership.InteractiveAuthenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.util.AwtUtils;

import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.StringAuthenticator;

public class Launch {
    public static void main(String args[]) {
        System.out.println("Jxta is now taking off. Please fasten your seat belts and extinguish all smoking materials.");
        try {
            AwtUtils.initAsDaemon();
            Thread.currentThread().setName(Launch.class.getName() + ".main()");

            // Establish the default store location via long established hackery.
            String jxta_home = System.getProperty("JXTA_HOME", ".jxta");

            if (!jxta_home.endsWith(File.separator)) {
                jxta_home += File.separator;
            }

            File homedir = new File(jxta_home);
            if (!homedir.exists()) {
                homedir.mkdirs();
            }

            NetPeerGroupFactory netPeerGroupFactory = new NetPeerGroupFactory();
            PeerGroup netPeerGroup = netPeerGroupFactory.getInterface();
            netPeerGroup.startApp(null);

            MembershipService membership = netPeerGroup.getMembershipService();
            Credential cred = membership.getDefaultCredential();

            if (cred == null) {
                ModuleSpecID msid = ((ModuleImplAdvertisement) membership.getImplAdvertisement()).getModuleSpecID();
                if (msid.equals(PSEMembershipService.pseMembershipSpecID)) {
                    AuthenticationCredential authCred = new AuthenticationCredential(netPeerGroup, "StringAuthentication", null);
                    StringAuthenticator auth = (StringAuthenticator) membership.apply(authCred);
                    if (auth != null) {
                        auth.setAuth1_KeyStorePassword(System.getProperty("net.jxta.tls.password"));
                        auth.setAuth2Identity(netPeerGroup.getPeerID());
                        auth.setAuth3_IdentityPassword(System.getProperty("net.jxta.tls.password"));
                        if (auth.isReadyForJoin()) {
                            membership.join(auth);
                        }
                    }
                }
            }

            cred = membership.getDefaultCredential();
            if (null == cred) {
                AuthenticationCredential authCred = new AuthenticationCredential(netPeerGroup, "InteractiveAuthentication", null);
                InteractiveAuthenticator auth = (InteractiveAuthenticator) membership.apply(authCred);
                if ((null != auth) && auth.interact() && auth.isReadyForJoin()) {
                    membership.join(auth);
                }
            }
            netPeerGroup.unref();
        } catch (Throwable e) {
            // make sure output buffering doesn't wreck console display.
            System.out.flush();
            System.err.println("Uncaught Throwable caught by 'main':");
            e.printStackTrace();
            //indicate an error has occurred
            System.exit(1);
        }
    }
}
