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
 * $Id: who.java,v 1.13 2007/02/09 23:12:52 hamada Exp $
 */

package net.jxta.impl.shell.bin.who;

import net.jxta.credential.Credential;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;

import java.util.Enumeration;

/**
 * Display credential information
 */
public class who extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        ShellEnv myEnv = getEnv();
        boolean pretty = false;

        GetOpt options = new GetOpt(argv, 0, "p");

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
                case'p':
                    pretty = true;
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        MimeMediaType displayAs;

        if (pretty)
            displayAs = MimeMediaType.TEXT_DEFAULTENCODING;
        else
            displayAs = MimeMediaType.XMLUTF8;

        try {
            // extract the membership service
            ShellObject obj = myEnv.get("stdgroup");
            PeerGroup group = (PeerGroup) obj.getObject();

            MembershipService membership = group.getMembershipService();

            Enumeration eachCred = membership.getCurrentCredentials();

            clearEnvironment();

            int count = 1;

            while (eachCred.hasMoreElements()) {
                Credential aCred = (Credential) eachCred.nextElement();

                String envName;
                String objName;
                if (aCred == membership.getDefaultCredential()) {
                    envName = "cred0";
                    objName = "Default Credential";
                } else {
                    envName = "cred" + Integer.toString(count++);
                    objName = "Credential";
                }

                myEnv.add(envName, new ShellObject<Credential>(objName, aCred));

                StructuredTextDocument doc = (StructuredTextDocument) aCred.getDocument(displayAs);

                print(doc.toString());
            }
        } catch (Exception e) {
            printStackTrace("Failed due to exception", e);
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    private void clearEnvironment() {
        ShellEnv myEnv = getEnv();

        Enumeration eachEnv = myEnv.elements();

        while (eachEnv.hasMoreElements()) {
            try {
                String anEnv = (String) eachEnv.nextElement();

                if (anEnv.startsWith("cred") && (0 != Integer.parseInt(anEnv.substring(4)))) {
                    myEnv.remove(anEnv);
                }
            } catch (Exception ignore) {
            }
        }
    }

    public int syntaxError() {
        consoleMessage("who [-p]");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display credential information";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("    who - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("    who [-p]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'who' displays the set of credentials associated with this");
        println("peer for the current peer group.");
        println("");
        println("OPTIONS");
        println(" ");
        println("    [-p]  pretty print credential information");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>who -p");
        println(" ");
        println("This example displays pretty printed credentials ");
        println(" ");
        println("SEE ALSO");
        println("    join, leave, set, info");
    }
}
