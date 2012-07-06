/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 * $Id: mkadv.java,v 1.26 2007/02/09 23:12:49 hamada Exp $
 */
package net.jxta.impl.shell.bin.mkadv;

import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.TextDocument;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

/**
 * mkadv Shell command
 */
public class mkadv extends ShellApp {
    private String advDoc = null;

    private int syntaxError() {
        consoleMessage("Usage : mkadv <doc>");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {

        ShellEnv env = getEnv();

        GetOpt options = new GetOpt(argv, 0, "d:");

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
                // support the "legacy" varient of this command.
                case'd':
                    advDoc = options.getOptionArg();
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        if (null == advDoc) {
            advDoc = options.getNextParameter();

            if (null == advDoc) {
                consoleMessage("Missing <advDoc> parameter");
                return syntaxError();
            }
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        if (null == advDoc) {
            consoleMessage("Advertisement Document name not specified.");
            return syntaxError();
        }

        try {
            ShellObject obj = env.get(advDoc);

            Advertisement adv;
            Object theRealObj = obj.getObject();
            if (theRealObj instanceof TextDocument) {
                TextDocument doc = (TextDocument) theRealObj;
                adv = AdvertisementFactory.newAdvertisement(doc.getMimeType(), doc.getReader());
            } else if (theRealObj instanceof Document) {
                Document doc = (Document) theRealObj;
                adv = AdvertisementFactory.newAdvertisement(doc.getMimeType(), doc.getStream());
            } else {
                consoleMessage(obj.getClass().getName() + "is not usable as an advertisement");
                return ShellApp.appMiscError;
            }

            ShellObject<Advertisement> newAdv = new ShellObject<Advertisement>("Advertisement", adv);
            env.add(getReturnVariable(), newAdv);

            return ShellApp.appNoError;
        } catch (Throwable all) {
            printStackTrace("Failure creating advertisment", all);
            return ShellApp.appMiscError;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Make an advertisement from a document";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     mkadv - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     mkadv <doc>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println(" 'mkadv' creates an advertisement from a document stored in a shell ");
        println(" environment variable. A symbolic name can be associated with ");
        println(" the advertisement. This name can be used to search for the ");
        println(" advertisement.");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("      JXTA>pipeadv = importfile -f saveadv");
        println("      JXTA>mypipeadv = mkadv pipeadv");
        println("      JXTA>inpipe = mkpipe -i mypipeadv");
        println(" ");
        println(" This creates a pipe using an advertisement stored in a file. ");
        println(" The file is imported as a document using the 'importfile' ");
        println(" command. An advertisement is created from the document using ");
        println(" 'mkadv' and finally an input pipe is instantiated using ");
        println(" 'mkpipe'. ");
        println(" ");
        println("SEE ALSO");
        println("     newpgrp mkpipe join whoami");
    }
}
