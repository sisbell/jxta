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
 * $Id: env.java,v 1.13 2007/02/09 23:12:46 hamada Exp $
 */

package net.jxta.impl.shell.bin.env;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

import java.util.Enumeration;

/**
 * Display environment variables
 */
public class env extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        ShellEnv myEnv = getEnv();

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


        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        Enumeration each = myEnv.elements();

        while (each.hasMoreElements()) {
            try {
                String objName = (String) each.nextElement();

                ShellObject obj = myEnv.get(objName);

                if (obj == null)
                    continue;

                println(objName + " = " + obj);
            } catch (Exception e) {
                printStackTrace("Failure printing environment variable", e);
            }
        }

        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("Usage: env ");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display environment variables";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     env  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     env");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Displays all of the environment variables defined in the Shell.");
        println(" ");
        println("The following environment variables are defined by default:");
        println("    console     = Console for this Shell.");
        println("    consin      = Console InputPipe");
        println("    consout     = Console OutputPipe");
        println("    stdin       = Default InputPipe");
        println("    stdout      = Default OutputPipe");
        println("    shell       = This Shell object");
        println("    stdgroup    = current active PeerGroup");
        println("    rootgroup   = Default Infrastructure PeerGroup");
        println("    worldgroup  = World PeerGroup");
        println("    echo        = (if defined) Shell will echo all commands before execution");
        println("    parentShell = (if defined) Parent Shell of this Shell");
        println(" ");
        println("Shell environment variables are defined as a result of executing ");
        println("Shell commands or using the 'set' command. The '=' operator can ");
        println("be used to assign the result value of a command to a particular ");
        println("variable. For example `myenv = mkmsg` will assign a new message ");
        println("object to the 'myenv' environment variable.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> env");
        println("    History = \"History\" [net.jxta.impl.shell.bin.history.HistoryQueue]");
        println("    rootgroup = \"Default Group\" [net.jxta.impl.peergroup.RefCountPeerGroupInterface]");
        println("    stdgroup = \"Default Group\" [net.jxta.impl.peergroup.RefCountPeerGroupInterface]");
        println("    console = \"console\" [net.jxta.impl.shell.SwingShellConsole]");
        println("    consout = \"Default Console OutputPipe\" [net.jxta.impl.shell.ShellOutputPipe]");
        println("    worldgroup = \"World Peer Group\" [net.jxta.impl.peergroup.PeerGroupInterface]");
        println("    stdout = \"Default OutputPipe\" [net.jxta.impl.shell.ShellOutputPipe]");
        println("    stdin = \"Default InputPipe\" [net.jxta.impl.shell.ShellInputPipe]");
        println("    shell = \"Shell\" [net.jxta.impl.shell.bin.Shell.Shell]");
        println("    consin = \"Default Console InputPipe\" [net.jxta.impl.shell.ShellInputPipe]");
        println("    JXTA> ");
        println(" ");
        println("This command will display all defined environment variables.");
        println(" ");
        println("SEE ALSO");
        println("    set unset Shell");
    }
}
