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
 * $Id: exportfile.java,v 1.15 2007/02/09 23:12:46 hamada Exp $
 */
package net.jxta.impl.shell.bin.exportfile;

import net.jxta.document.Document;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 'exportfile' Shell command to save an environment variable or stream to a
 * file.
 */
public class exportfile extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        String filename = null;
        String envName;

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

        envName = options.getNextParameter();

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        File file = new File(filename);

        OutputStream op = null;

        try {
            op = new FileOutputStream(file);

            if (null != envName) {
                ShellObject obj = env.get(envName);

                if (!Document.class.isAssignableFrom(obj.getObjectClass())) {
                    consoleMessage("Cannot export object of type '" + obj.getObjectClass().getName() + "'");
                    return ShellApp.appMiscError;
                }

                Document doc = (Document) obj.getObject();

                doc.sendToStream(op);
            } else {
                String line;
                do {
                    try {
                        line = waitForInput();
                    } catch (IOException failed) {
                        line = null;
                    }

                    if (line != null) {
                        op.write(line.getBytes());
                    }
                } while (null != line);
            }
        } catch (IOException failure) {
            printStackTrace(" ", failure);
            return ShellApp.appMiscError;
        } finally {
            if (null != op) {
                try {
                    op.close();
                } catch (IOException ioe) {
                    //ignored
                }
            }
        }

        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("usage : exportfile <filename> <env>");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Export enviroment variable to an external file";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     exportfile - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     exportfile <filename> <env>");
        println(" ");
        println("DESCRIPTION");
        println("'exportfile' exports the content of a Shell environment variable");
        println("into an external file. The exported object is stored in the");
        println("supplied filename argument.");
        println("If no variable name is given, the 'stdin' pipe is used to read");
        println("data and saved it into the file.");
        println("'exportfile' is the reverse operation of 'importfile'");
        println(" ");
        println("EXAMPLE");
        println("    JXTA> exportfile /home/tra/myfile myfile");
        println(" ");
        println("This command saves into the file '/home/tra/myfile' the content");
        println("of the 'myfile' environment variable");
        println(" ");
        println("SEE ALSO");
        println("        importfile ");
    }
}
