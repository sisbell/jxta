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
 * $Id: put.java,v 1.13 2007/05/29 15:51:23 bondolo Exp $
 */
package net.jxta.impl.shell.bin.put;

import net.jxta.document.Document;
import net.jxta.endpoint.InputStreamMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

/**
 * mkmsg: Add an element to a message.
 */
public class put extends ShellApp {

    private int syntaxError() {
        consoleMessage("usage: put <msg> <elementname> <document>");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        ShellEnv env;

        if ((args == null) || (args.length != 3)) {
            return syntaxError();
        }

        env = getEnv();

        ShellObject obj = env.get(args[0]);
        if (obj == null) {
            consoleMessage("cannot access " + args[0]);
            return ShellApp.appMiscError;
        }

        Message msg;
        try {
            msg = (Message) obj.getObject();
        } catch (Exception e) {
            // This is not a Message
            consoleMessage(args[0] + " is not a Message");
            return ShellApp.appMiscError;
        }

        if (msg == null) {
            consoleMessage(args[0] + " is empty.");
            return ShellApp.appMiscError;
        }

        String name = args[1];

        obj = env.get(args[2]);
        if (obj == null) {
            consoleMessage("cannot access " + args[2]);
            return ShellApp.appMiscError;
        }

        Document doc;
        try {
            doc = (Document) obj.getObject();
        } catch (Exception e) {
            // This is not a Document
            consoleMessage(args[2] + " is not a Document.");
            return ShellApp.appMiscError;
        }

        if (doc == null) {
            consoleMessage(args[2] + " is empty");
            return ShellApp.appMiscError;
        }

        try {
            MessageElement elem = new InputStreamMessageElement( name, doc.getMimeType(), doc.getStream(), null);

            msg.addMessageElement(elem);
        } catch (Exception e) {
            printStackTrace("Failed to add message element", e);
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Put data into a message";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     put - put data into a message");
        println(" ");
        println("SYNOPSIS");
        println("     put <msg> <elementname> <document>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'put' store a document into the body of message. JXTA messages ");
        println("are composed a set of elements, each identified with a tag name ");
        println("A message tag name is supplied to specify which tag name is ");
        println("used to store the document. On the receiving end the document ");
        println("can be retrieved via the 'get' command.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> importfile -f /home/tra/myfile mydata");
        println("    JXTA> msg = mkmsg");
        println("    JXTA> put msg mytag mydata");
        println(" ");
        println("This example creates a document 'mydata' by importing data from the file");
        println("'/home/tra/myfile'. Then, we create a message 'msg' and store the document");
        println("'mydata' into the message 'msg' with the associated tag name 'mytag'.");
        println(" ");
        println("SEE ALSO");
        println("    mkmsg get send recv mkadv mkpipe");
    }
}
