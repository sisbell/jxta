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
 * $Id: get.java,v 1.17 2007/02/09 23:12:45 hamada Exp $
 */
package net.jxta.impl.shell.bin.get;

import net.jxta.document.BinaryDocument;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.NoSuchElementException;

/**
 * mkmsg: create a Message
 */
public class get extends ShellApp {
    ShellEnv env;

    public get() {
    }

    @Override
    public void stopApp() {
    }

    private int syntaxError() {
        println("usage: get <msg> <name>");
        return ShellApp.appParamError;
    }

    public int startApp(String[] args) {
        if ((args == null) || (args.length != 2)) {
            return syntaxError();
        }

        env = getEnv();

        ShellObject obj = env.get(args[0]);
        if (obj == null) {
            println("get: cannot access " + args[0]);
            return ShellApp.appMiscError;
        }

        Message msg;
        try {
            msg = (Message) obj.getObject();
        } catch (Exception e) {
            // This is not a Message
            println("get: " + args[0] + " is not a Message");
            return ShellApp.appMiscError;
        }

        if (msg == null) {
            println("get: " + args[0] + " is empty");
            return ShellApp.appMiscError;
        }

        String name = args[1];

        InputStream ip;
        StructuredDocument doc;

        try {
            MessageElement elem = msg.getMessageElement(name);
            msg.removeMessageElement(elem);
            ip = elem.getStream();

            try {
                doc = StructuredDocumentFactory.newStructuredDocument(elem.getMimeType(), ip);
                env.add(getReturnVariable(),
                        new ShellObject<StructuredDocument>("StructuredDocument", doc));

                return ShellApp.appNoError;
            } catch (NoSuchElementException notadoc) {
                ip = elem.getStream();
                ByteArrayOutputStream op = new ByteArrayOutputStream();

                int aChar;
                do {
                    aChar = ip.read();
                    if (-1 != aChar)
                        op.write(aChar);
                } while (aChar >= 0);

                BinaryDocument bdoc = new BinaryDocument(op.toByteArray());

                env.add(getReturnVariable(), new ShellObject<BinaryDocument>("BinaryDocument", bdoc));

                return ShellApp.appNoError;
            }

        } catch (Exception e) {
            println("get: failed with an exception ");
            StringWriter theStackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(theStackTrace));
            print(theStackTrace.toString());
            return ShellApp.appMiscError;
        }

    }

    @Override
    public String getDescription() {
        return "Get data from a pipe message";
    }

    @Override
    public void help() {
        println("NAME");
        println("     get - get data from a pipe message");
        println(" ");
        println("SYNOPSIS");
        println("     get <msg> <tag>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'get' retrieve the tag body of a message. JXTA messages are");
        println("composed a set of tag body, each identified with an unique tag name");
        println("A message tag name is supplied to command to specify which tag body");
        println("to extract.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> pipeadv = newpipe -n mypipe");
        println("    JXTA> inpipe = mkpipe -i pipeadv");
        println("    JXTA> msg = recv inpipe");
        println("    JXTA> data = get msg mytag");
        println(" ");
        println("This example creates a pipe advertisement 'pipeadv',");
        println("creates an input pipe 'inpipe', and receives a message 'msg'.");
        println("The tag body of the message associated with the tag 'mytag' is");
        println("retreived from the message via the 'get' command.");
        println(" ");
        println("SEE ALSO");
        println("    mkmsg put send recv mkadv newpipe mkpipe");
    }
}
