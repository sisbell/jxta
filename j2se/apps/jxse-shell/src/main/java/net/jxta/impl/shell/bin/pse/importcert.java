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
 * $Id: importcert.java,v 1.10 2007/02/09 23:12:41 hamada Exp $
 */

package net.jxta.impl.shell.bin.pse;

import net.jxta.document.Attributable;
import net.jxta.document.Element;
import net.jxta.document.StructuredDocument;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.protocol.Certificate;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerAdvertisement;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Imports a trusted certificate chain.
 */
public class importcert extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        ShellEnv env = getEnv();
        PeerGroup current = (PeerGroup) env.get("stdgroup").getObject();
        String createID;
        ID createid;
        boolean trimChain = false;

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

        GetOpt options = new GetOpt(argv, 0, "t");

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
                case't':
                    trimChain = true;
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        createID = options.getNextParameter();

        if (null == createID) {
            consoleMessage("Missing <id> or <peerAdv> parameter");
            return syntaxError();
        }

        Certificate cert_msg;
        ShellObject envObj = env.get(createID);

        if (null != envObj) {
            // we will assume its a peer adv
            if (PeerAdvertisement.class.isAssignableFrom(envObj.getObjectClass())) {
                PeerAdvertisement pa = (PeerAdvertisement) envObj.getObject();

                createid = pa.getPeerID();

                StructuredDocument rootParams = pa.getServiceParam(PeerGroup.peerGroupClassID);

                if (null == rootParams) {
                    consoleMessage("'" + createID + "' does not contain group parameters");
                    return ShellApp.appMiscError;
                }

                Enumeration eachRoot = rootParams.getChildren("RootCert");

                if (!eachRoot.hasMoreElements()) {
                    consoleMessage("'" + createID + "' does not contain group parameters");
                    return ShellApp.appMiscError;
                }

                Element root = (Element) eachRoot.nextElement();

                if (root instanceof Attributable) {
                    // XXX 20040719 bondolo Backwards compatibility hack. Adds type so cert chain is recognized.
                    ((Attributable) root).addAttribute("type", Certificate.getMessageType());
                }

                cert_msg = new Certificate(root);
            } else {
                consoleMessage("'" + createID + "' is not a PeerAdvertisement");
                return syntaxError();
            }
        } else {
            String certEnvName = options.getNextParameter();

            if (null == certEnvName) {
                consoleMessage("Missing <cert> parameter");
                return syntaxError();
            }

            try {
                URI idURI = new URI(createID);
                createid = IDFactory.fromURI(idURI);
            } catch (URISyntaxException badID) {
                printStackTrace("Bad ID", badID);
                return ShellApp.appMiscError;
            }

            ShellObject certEnv = env.get(certEnvName);

            if (null == certEnv) {
                consoleMessage("Issuer environment variable '" + certEnvName + "' not found.");
                return ShellApp.appMiscError;
            }

            if (!StructuredDocument.class.isAssignableFrom(certEnv.getObjectClass())) {
                consoleMessage("'" + certEnvName + "' is not a certificate.");
                return ShellApp.appMiscError;
            }

            cert_msg = new Certificate((Element) certEnv.getObject());
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        try {
            Iterator sourceChain = Arrays.asList(cert_msg.getCertificates()).iterator();

            int imported = 0;
            X509Certificate aCert = (X509Certificate) sourceChain.next();

            do {
                if (null != pse.getPSEConfig().getTrustedCertificateID(aCert)) {
                    break;
                }

                pse.getPSEConfig().erase(createid);
                pse.getPSEConfig().setTrustedCertificate(createid, aCert);
                imported++;

                // create a codat id for the next certificate in the chain.
                aCert = null;
                if (sourceChain.hasNext()) {
                    aCert = (X509Certificate) sourceChain.next();

                    if (trimChain) {
                        if (null != pse.getPSEConfig().getTrustedCertificateID(aCert)) {
                            // it's already in the pse, time to bail!
                            break;
                        }
                    }
                    byte[] der = aCert.getEncoded();
                    createid = IDFactory.newCodatID(current.getPeerGroupID(), new ByteArrayInputStream(der));
                }
            } while (null != aCert);

            consoleMessage("Imported " + imported + " certificates. ");
        } catch (CertificateEncodingException failure) {
            printStackTrace("Bad certifiacte.", failure);
            return ShellApp.appMiscError;
        } catch (KeyStoreException failure) {
            printStackTrace("KeyStore failure while importing certificate.", failure);
            return ShellApp.appMiscError;
        } catch (IOException failure) {
            printStackTrace("IO failure while importing certificate.", failure);
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("Usage: pse.importcert [-t] [<id> <cert> | <peerAdv>]");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Imports a trusted certificate chain.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     pse.importcert  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     pse.importcert [-t] [<id> <cert> | <peerAdv>]");
        println(" ");
        println("    <id>       ID under which the certificate chain will be ");
        println("               stored in the PSE key store.");
        println("    <cert>     The certificate chain to be imported. ");
        println("    <peerAdv>  The PeerAdvertisement who's certificate will be");
        println("               trusted.");
        println(" ");
        println("OPTIONS");
        println("    [-t]    Trim the certificate chain at the first certificate");
        println("            already present in the PSE.");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Import a trusted certificate or certificate chain into the PSE.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> pse.importcert urn:jxta:uuid-59616261646162614A78746150325033CACD56AE273E448CB25E8DA42C2BD46903 cert");
        println(" ");
        println("This example imports a certificate from 'cert'. The certificate ");
        println("chain in 'cert' will be stored under the id provided.");
        println(" ");
        println("SEE ALSO");
        println("     pse.certs pse.keys pse.erase pse.createkey pse.newcsr pse.signcsr");
    }
}
