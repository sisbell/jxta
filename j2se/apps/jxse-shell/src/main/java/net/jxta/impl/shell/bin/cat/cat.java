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
 * $Id: cat.java,v 1.28 2007/02/09 23:12:45 hamada Exp $
 */
package net.jxta.impl.shell.bin.cat;

import net.jxta.credential.Credential;
import net.jxta.document.Advertisement;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredTextDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Message.ElementIterator;
import net.jxta.endpoint.MessageElement;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.protocol.PeerInfoResponseMessage;

import java.io.ByteArrayOutputStream;

/**
 * Concatanate and display a Shell object
 */
public class cat extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        ShellEnv env = getEnv();
        boolean pretty = false;

        GetOpt options = new GetOpt(args, 0, "p");

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

        String name = options.getNextParameter();

        if ((null != name) && (null != options.getNextParameter())) {
            consoleMessage("Unsupported parameter.");
            return syntaxError();
        }

        if (null == name) {
            consoleMessage("Missing <env> parameter.");
            return syntaxError();
        }

        ShellObject obj = env.get(name);
        if (obj == null) {
            consoleMessage("Environment variable '" + name + "' not found.");
            return ShellApp.appMiscError;
        }

        Object catobj = obj.getObject();

        if (Advertisement.class.isInstance(catobj)) {
            try {
                catAdvertisement((Advertisement) obj.getObject(), pretty);
            } catch (Exception e) {
                printStackTrace("Error printing Advertisement object. ", e);
                return ShellApp.appMiscError;
            }
        } else if (StructuredDocument.class.isInstance(catobj)) {
            try {
                print(obj.getObject().toString());
                return ShellApp.appNoError;
            } catch (Exception e) {
                printStackTrace("Error printing StructuredDocument object. ", e);
                return ShellApp.appMiscError;
            }
        } else if (Document.class.isInstance(catobj)) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                Document doc = (Document) catobj;
                doc.sendToStream(out);
                println(out.toString());
            } catch (Exception e) {
                printStackTrace("Error printing Document object. ", e);
                return ShellApp.appMiscError;
            }
        } else if (Message.class.isInstance(catobj)) {
            try {
                catJxtaMessage((Message) obj.getObject());
            } catch (Exception e) {
                printStackTrace("Error printing Message object. ", e);
                return ShellApp.appMiscError;
            }
        } else if (PeerInfoResponseMessage.class.isInstance(catobj)) {
            try {
                PeerInfoResponseMessage resp = (PeerInfoResponseMessage) catobj;

                MimeMediaType displayAs;

                if (pretty)
                    displayAs = MimeMediaType.TEXTUTF8;
                else
                    displayAs = MimeMediaType.XMLUTF8;

                StructuredTextDocument doc = (StructuredTextDocument) resp.getDocument(displayAs);

                print(doc.toString());
            } catch (Exception e) {
                printStackTrace("Error printing PeerInfoResponseMessage object. ", e);
                return ShellApp.appMiscError;
            }
        } else if (Credential.class.isInstance(catobj)) {
            try {
                Credential resp = (Credential) catobj;

                MimeMediaType displayAs;

                if (pretty)
                    displayAs = MimeMediaType.TEXTUTF8;
                else
                    displayAs = MimeMediaType.XMLUTF8;

                StructuredTextDocument doc = (StructuredTextDocument) resp.getDocument(displayAs);

                print(doc.toString());
            } catch (Exception e) {
                printStackTrace("Error printing Credential object. ", e);
                return ShellApp.appMiscError;
            }
        } else {
            // if we get here we are basicaly giving up.
            println(catobj.toString());
        }

        return ShellApp.appNoError;
    }

    private void catAdvertisement(Advertisement adv, boolean pretty) throws Exception {

        MimeMediaType displayAs;

        if (pretty)
            displayAs = MimeMediaType.TEXT_DEFAULTENCODING;
        else
            displayAs = MimeMediaType.XMLUTF8;

        StructuredTextDocument doc = (StructuredTextDocument) adv.getDocument(displayAs);

        print(doc.toString());
    }

    private void catJxtaMessage(Message msg) {
        ElementIterator eachElement = msg.getMessageElements();

        int count = 1;
        while (eachElement.hasNext()) {
            MessageElement anElem = eachElement.next();
            println("Message Element # " + count++);
            println(" name : " + eachElement.getNamespace() + " / " + anElem.getElementName());
            println(" type : " + anElem.getMimeType());
            println(" body : (" + anElem.getByteLength() + " bytes)");
        }
    }

    private int syntaxError() {
        consoleMessage("cat [-p] <env>");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Concatenate and display a Shell object";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     cat  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     cat [-p] <objectName>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'cat' displays on stdout the contents of objects stored in");
        println("environment variables. 'cat' knows how to display a limited");
        println("(but growing) set of JXTA object types : ");
        println("  - Advertisement ");
        println("  - Credentials ");
        println("  - Document ");
        println("  - StructuredDocument ");
        println("  - Message ");
        println("  - PeerInfoResponseMessage ");
        println(" ");
        println("If you are not sure, try to cat the object anyway -- 'cat'");
        println("will try to display the object as best it can.");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("    [-p]   Pretty display");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> importfile -f /home/tra/myfile myfile");
        println("    JXTA> cat -p myfile");
        println(" ");
        println("This command imports the file '/home/tra/myfile' into the");
        println("'myfile' environment variable and displays it on stdout.");
        println(" ");
        println("SEE ALSO");
        println("    more env");
    }
}
