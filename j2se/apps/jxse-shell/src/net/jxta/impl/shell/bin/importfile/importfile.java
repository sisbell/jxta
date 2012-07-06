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
 * $Id: importfile.java,v 1.14 2007/02/09 23:12:42 hamada Exp $
 */
package net.jxta.impl.shell.bin.importfile;

import net.jxta.document.*;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

import java.io.*;

/**
 * 'importfile' Shell command to load a new file object into a
 * Structured document content environment variable
 */
public class importfile extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        String filename = null;
        String envName = null;

        ShellEnv env = getEnv();

        GetOpt options = new GetOpt(args, 0, "f:");

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
                case'f':
                    filename = options.getOptionArg();
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        if (null == filename) {
            filename = options.getNextParameter();

            if (null == filename) {
                consoleMessage("Missing <filename> parameter");
                return syntaxError();
            }
        }

        if (null == envName) {
            envName = options.getNextParameter();
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        if (null == envName) {
            envName = getReturnVariable();
        }

        try {
            File file = new File(filename);

            long length = file.length();

            InputStream ip = new FileInputStream(file);

            if (length > 32) {
                try {
                    XMLDocument doc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, ip);

                    env.add(envName, new ShellObject<XMLDocument>("XMLDocument", doc));

                    return ShellApp.appNoError;
                } finally {
                    ip.close();
                }
            }

            try {
                byte[] buffer = new byte[(int) length];

                DataInput dis = new DataInputStream(ip);
                dis.readFully(buffer);

                Document doc = new BinaryDocument(buffer);

                env.add(envName, new ShellObject<Document>("Document", doc));

                return ShellApp.appNoError;
            } finally {
                ip.close();
            }
        } catch (IOException failure) {
            printStackTrace(" ", failure);
            return ShellApp.appMiscError;
        }
    }

    private int syntaxError() {
        consoleMessage("usage : importfile <filename>");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Import an external file";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     importfile - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     importfile <filename>");
        println(" ");
        println("DESCRIPTION");
        println("'importfile' imports an external file into a Document");
        println("object stored in a Shell environment variable. The name of the");
        println("environment variable is specified as an argument.");
        println("'importfile' is the reverse operation of 'exportfile'");
        println(" ");
        println("EXAMPLE");
        println("    JXTA> myfile = importfile /home/tra/myfile ");
        println("    JXTA> cat myfile");
        println(" ");
        println("This command imports the file '/home/tra/myfile' into the");
        println("'myfile' environment variable");
        println(" ");
        println("SEE ALSO");
        println("    exportfile mkadv");
    }
}
