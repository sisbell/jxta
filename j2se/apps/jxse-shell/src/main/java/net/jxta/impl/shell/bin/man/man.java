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
 * $Id: man.java,v 1.25 2007/02/09 23:12:39 hamada Exp $
 */
package net.jxta.impl.shell.bin.man;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellCmds;
import net.jxta.impl.shell.ShellEnv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An on-line help command that displays information about a specific Shell
 * command
 */
public class man extends ShellApp {

    ShellCmds cmds;
    ShellEnv env;

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        env = getEnv();
        cmds = new ShellCmds(env);

        GetOpt options = new GetOpt(argv, 0, "");

        while (true) {
            //FIXME this does not loop
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
                    return ShellApp.appMiscError;
            }
        }

        String cmd = options.getNextParameter();

        if (null == cmd) {
            selfHelp();
        } else {
            if (null != options.getNextParameter()) {
                consoleMessage("Unexpected parameter");
                return ShellApp.appMiscError;
            }

            ShellApp app = loadApp(null, cmd, env);

            if (null != app) {
                app.help();
            } else {
                consoleMessage("Could not find a command named : " + cmd);
                return ShellApp.appMiscError;
            }
        }
        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("Usage: man [<command>]");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "An on-line help command that displays information about a specific Shell command";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     man - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     man  [<command>]");
        println(" ");
        println("     <command>   The name of the command to display help info.");
        println(" ");
        println("DESCRIPTION");
        println("'man' is an on-line help command that displays information ");
        println("about a specific Shell command.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("       JXTA> man mkpgrp ");
        println(" ");
        println("This will display information about the mkpgrp command");
        println(" ");
        println("SEE ALSO");
        println(" ");
    }

    private String formatString(String str, int strWidth) {
        StringBuilder buf = new StringBuilder(str);

        while (buf.length() < strWidth)
            buf.append(' ');
        return buf.toString();
    }

    private void selfHelp() {
        println("The 'man' command is the primary manual system for the JXTA Shell.");
        println("The usage of man is:");
        println(" ");
        println("   JXTA> man [<commandName>]");
        println(" ");
        println("  For instance typing");
        println("   JXTA> man Shell");
        println("         displays man page about the Shell");
        println(" ");
        println("The following commands are available:");
        println(" ");

        try {
            List<String> cmdlist = cmds.cmdlist();
            List<String> descriptions = new ArrayList<String>();

            int maxlen = 0;

            Iterator<String> eachCmd = cmdlist.iterator();

            while (eachCmd.hasNext()) {
                String aCommand = eachCmd.next();

                ShellApp app = loadApp(null, aCommand, env);

                if (null == app) {
                    eachCmd.remove();
                } else {
                    maxlen = Math.max(maxlen, aCommand.length());
                    descriptions.add(app.getDescription());
                }
            }

            eachCmd = cmdlist.iterator();
            Iterator<String> eachDesc = descriptions.iterator();
            maxlen += 4;

            while (eachCmd.hasNext()) {
                String aCommand = eachCmd.next();

                println(formatString(aCommand, maxlen) + eachDesc.next());
            }
        } catch (Exception e) {
            printStackTrace("Failure in printing descriptions.", e);
        }
        println(" ");
    }
}
