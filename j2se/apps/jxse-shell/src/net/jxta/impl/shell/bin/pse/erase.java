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
 * $Id: erase.java,v 1.7 2007/02/09 23:12:41 hamada Exp $
 */

package net.jxta.impl.shell.bin.pse;

import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.membership.MembershipService;
import net.jxta.protocol.ModuleImplAdvertisement;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStoreException;

/**
 * PSE.erase
 */
public class erase extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        String eraseID;

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

        eraseID = options.getNextParameter();


        if (null == eraseID) {
            consoleMessage("Missing <id> parameter");
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
            consoleMessage("KeyStore has not been opened.");
            return ShellApp.appMiscError;
        }

        try {
            ID id;

            try {
                URI idURL = new URI(eraseID);
                id = IDFactory.fromURI(idURL);
            } catch (URISyntaxException badID) {
                printStackTrace("Bad ID", badID);
                return ShellApp.appMiscError;
            }

            pse.getPSEConfig().erase(id);
        } catch (KeyStoreException failure) {
            printStackTrace("KeyStore failure while printing keys", failure);
            return ShellApp.appMiscError;
        } catch (IOException failure) {
            printStackTrace("IO failure while printing keys", failure);
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("Usage: pse.erase <id>");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Erases a key or certificate from the PSE key store";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     pse.erase  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     pse.erase <id>");
        println(" ");
        println("     <id>    ID of the key or certificate to erase.");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("     (none)");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("     Erases a key or certificate from the PSE key store used");
        println("     by the membership service in this group.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> pse.erase urn:jxta:uuid-59616261646162614A78746150325033CACD56AE273E448CB25E8DA42C2BD46903");
        println(" ");
        println(" ");
        println("SEE ALSO");
        println("     pse.certs pse.keys");
    }
}
