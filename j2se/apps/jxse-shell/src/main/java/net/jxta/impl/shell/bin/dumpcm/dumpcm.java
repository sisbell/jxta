/*
 * Copyright (c) 2003 Sun Microsystems, Inc.  All rights
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
 * $Id: dumpcm.java,v 1.5 2007/02/09 23:12:44 hamada Exp $
 */
package net.jxta.impl.shell.bin.dumpcm;

import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.util.cm.DumpCm;

import java.io.File;
import java.io.IOException;

/**
 * Dump the CM content
 */
public class dumpcm extends ShellApp {


    public dumpcm() {
    }

    public int startApp(String[] args) {

        String type = null;
        String dir = null;
        String file = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-type") && i + 1 < args.length) {
                type = args[++i];
            } else if (args[i].equals("-dir") && i + 1 < args.length) {
                dir = args[++i];
            } else if (args[i].equals("-file") && i + 1 < args.length) {
                file = args[++i];
            } else if (args[i].equals("-help")) {
                syntaxError();
                return ShellApp.appNoError;
            }
        }

        if (dir == null) {
            String DEFAULT_ROOT = ".jxta" + File.separator + "cm";

            dir = DEFAULT_ROOT +
                    File.separator +
                    getGroup().getPeerGroupID().getUniqueValue().toString();
        }


        String[] newArgs = new String[6];

        newArgs[0] = "-type";
        newArgs[1] = type;
        newArgs[2] = "-dir";
        newArgs[3] = dir;
        newArgs[4] = "-file";
        newArgs[5] = file;

        try {

            DumpCm.dump(newArgs, new DisplayDump(this));

        } catch (IOException ez) {
            println("Cannot dump");
            return ShellApp.appMiscError;
        } catch (IllegalArgumentException ez1) {
            return syntaxError();
        }
        return ShellApp.appNoError;
    }

    @Override
    public void stopApp() {
    }

    @Override
    public String getDescription() {
        return "Dump the content of the local cache (CM)";
    }


    private class DisplayDump implements DumpCm.DumpCmCallback {

        dumpcm app = null;

        public DisplayDump(dumpcm app) {
            this.app = app;
        }

        public void println(String str) {
            app.println(str);
        }
    }

    private int syntaxError() {

        println("Usage: dumpcm");
        println("  -type    type of file being dumped (index|offsets|db)");
        println("  -dir     the directory to dump");
        println("  -file    the file to dump");

        return ShellApp.appParamError;
    }

    @Override
    public void help() {
        println("NAME");
        println(" ");
        println("    dumpcm - Dump the content of the local cache (CM)");
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("    dumpcm");
        println("          -type    type of file being dumped (index|offsets|db)");
        println("          -dir     the directory to dump");
        println("          -file    the file to dump");

        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("dumpcm allows to dump onto the screen the content");
        println("of the local cache (CM).");

    }
}
