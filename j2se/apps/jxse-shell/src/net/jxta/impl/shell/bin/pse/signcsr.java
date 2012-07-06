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
 * $Id: signcsr.java,v 1.6 2007/02/09 23:12:41 hamada Exp $
 */

package net.jxta.impl.shell.bin.pse;

import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLDocument;
import net.jxta.impl.membership.pse.PSECredential;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.membership.MembershipService;
import net.jxta.protocol.ModuleImplAdvertisement;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.X509V3CertificateGenerator;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * PSE.erase
 */
public class signcsr extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        ShellEnv env = getEnv();
        String issuerEnvName;
        String duration;
        String csrEnvName;

        GetOpt options = new GetOpt(argv, 0, "");

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

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        issuerEnvName = options.getNextParameter();

        if (null == issuerEnvName) {
            consoleMessage("Missing <issuer> parameter");
            return syntaxError();
        }

        duration = options.getNextParameter();

        if (null == duration) {
            consoleMessage("Missing <duration> parameter");
            return syntaxError();
        }

        csrEnvName = options.getNextParameter();

        if (null == csrEnvName) {
            consoleMessage("Missing <csr> parameter");
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

        PSEUtils.IssuerInfo issuer;
        X509Certificate[] issuerChain;

        ShellObject issuerEnv = env.get(issuerEnvName);

        if (null == issuerEnv) {
            consoleMessage("Issuer environment variable '" + issuerEnvName + "' not found.");
            return ShellApp.appMiscError;
        }

        if (!PSECredential.class.isAssignableFrom(issuerEnv.getObjectClass())) {
            consoleMessage("'" + issuerEnvName + "' is not a  is not a PSE credential.");
            return ShellApp.appMiscError;
        }

        PSECredential cred = (PSECredential) issuerEnv.getObject();

        issuerChain = cred.getCertificateChain();

        PrivateKey issuerKey = null;

        try {
            issuerKey = cred.getPrivateKey();
        } catch (IllegalStateException notLocal) {
            //ignored            
        }

        if (null == issuerKey) {
            consoleMessage("Credential is not a local login credential.");
            return ShellApp.appMiscError;
        }

        issuer = new PSEUtils.IssuerInfo();

        issuer.cert = issuerChain[0];
        issuer.subjectPkey = issuerKey;

        ShellObject csrEnv = env.get(csrEnvName);

        if (null == csrEnv) {
            consoleMessage("CSR environment variable not found.");
            return ShellApp.appMiscError;
        }

        if (!StructuredDocument.class.isAssignableFrom(csrEnv.getObjectClass())) {
            consoleMessage("'" + csrEnvName + "' is not a Certificate Signing Request.");
            return ShellApp.appMiscError;
        }

        net.jxta.impl.protocol.CertificateSigningRequest csr_msg = new net.jxta.impl.protocol.CertificateSigningRequest((Element) csrEnv.getObject());

        org.bouncycastle.jce.PKCS10CertificationRequest csr = csr_msg.getCSR();

        // set validity 10 years from today
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.DATE, Integer.parseInt(duration));
        Date until = cal.getTime();

        // generate cert
        try {
            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

            certGen.setIssuerDN(new X509Principal(true, issuer.cert.getSubjectX500Principal().getName()));
            certGen.setSubjectDN(csr.getCertificationRequestInfo().getSubject());
            certGen.setNotBefore(today);
            certGen.setNotAfter(until);
            certGen.setPublicKey(csr.getPublicKey());
            //certGen.setSignatureAlgorithm("SHA1withDSA");
            certGen.setSignatureAlgorithm("SHA1withRSA");
            // FIXME bondolo 20040317 needs fixing.
            certGen.setSerialNumber(BigInteger.valueOf(1));

            // return issuer info for generating service cert

            // the cert
            X509Certificate newCert = certGen.generateX509Certificate(issuer.subjectPkey);

            net.jxta.impl.protocol.Certificate cert_msg = new net.jxta.impl.protocol.Certificate();

            List<X509Certificate> newChain = new ArrayList<X509Certificate>(Arrays.asList(issuerChain));

            newChain.add(0, newCert);

            cert_msg.setCertificates(newChain);

            XMLDocument asXML = (XMLDocument) cert_msg.getDocument(MimeMediaType.XMLUTF8);

            ShellObject<XMLDocument> newObj = new ShellObject<XMLDocument>("Certificate", asXML);
            env.add(getReturnVariable(), newObj);
        } catch (Exception failed) {
            printStackTrace("Failed to generate certificate", failed);
        }

        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("Usage: pse.signcsr <issuer> <duration> <csr>");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Signs a certificate signing request";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     pse.signcsr  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     pse.signcsr <issuer> <duration> <csr>");
        println(" ");
        println("     <issuer>    The credential which will be the issuer of ");
        println("                 the certificate.");
        println("     <duration>  The duration of the certificate to be issued ");
        println("                 measured in relative days from today.");
        println("     <csr>       The certificate signing request.");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Signs a public key.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> newcert = pse.signcsr cred0 120 mycsr");
        println(" ");
        println(" ");
        println("SEE ALSO");
        println("     pse.certs pse.keys pse.erase pse.createkey pse.newcsr pse.importcert");
    }
}
