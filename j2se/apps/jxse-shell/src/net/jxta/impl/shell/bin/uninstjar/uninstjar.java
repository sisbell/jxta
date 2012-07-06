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
 * $Id: uninstjar.java,v 1.9 2007/02/09 23:12:47 hamada Exp $
 */


package net.jxta.impl.shell.bin.uninstjar;

import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellCmds;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 'uninstjar' Shell command to remove installed commands contained in the jar-file to be uninstalled.
 */
public class uninstjar extends ShellApp {

    public int startApp(String[] args) {
        List<URL> instJarList;
        ShellCmds cmds;

        if (args.length == 0) {
            return syntaxError();
        }

        cmds = new ShellCmds(getEnv());

        // Construct a list containing all installed files (if any)
        instJarList = new ArrayList<URL>(Arrays.asList(cmds.getInstJars()));

        // Check whether the environment variable contains files
        if (instJarList.isEmpty()) {
            return noJarsInstalledError();
        }

        // Check if we must uninstall all files
        if (args[0].equals("-all")) {
            instJarList.clear();
        } else {
            // Construct list containing all File objects to be removed.

            for (int i = 0; i < args.length; i++) {
                // Check for file parameter.
                if (args[i].equals("-f")) {
                    // Test whether the neccessary file argument exists.
                    if (++i >= args.length) {
                        consoleMessage("Error: Missing file argument!");
                        break;
                    }
                    // Add corresponding File object to toRemoveList.
                    try {
                    URL file = new File(args[i]).toURI().toURL();
                    if (instJarList.contains(file)) {
                        instJarList.remove(file);
                    } else {
                        consoleMessage("Error: '" + file + "' not found!");
                    }
                    } catch( Exception failed ) {
                        printStackTrace( "Failed processing file : " + args[i], failed );
                    }
                } else {
                    consoleMessage("Error: Unknown parameter '" + args[i] + "'!");
                }
            }
        }

        // Set the new value of environment variable INST_JARS.
        cmds.setInstJars(instJarList);

        return ShellApp.appNoError;
    }

    private int syntaxError() {
        println("Usage: uninstjar <-all | -f jar-file | -i index> [<-f jar-file | -i index> [...]]");
        return ShellApp.appParamError;
    }

    private int noJarsInstalledError() {
        consoleMessage("Error: No jar-files installed!");
        return ShellApp.appMiscError;
    }

    /**
     * Returns a short description for this command (used by the Shell command 'man').
     */
    @Override
    public String getDescription() {
        return "Uninstalls jar-files previously installed with 'instjar'";
    }

    /**
     * Prints the help text.
     */
    @Override
    public void help() {
        println("NAME");
        println("     uninstjar - uninstalls one or more jar-files previously");
        println("     installed with the command 'instjar'.");
        println(" ");
        println("SYNOPSIS");
        println("     uninstjar <-all | -f file [-f file]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("This command uninstalls previously installed jar-files containing");
        println("additional Shell commands. Once uninstalled, the commands placed");
        println("in that jar-file are no longer available to the Shell.");
        println("An arbirtary number of jar-files to uninstall can be supplied.");
        println("By supplying the path to the jar-file itself" );
        println("So uninstalling means removing the corresponding entry from this");
        println("environment variable.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("To uninstall a jar-file by supplying the path:");
        println("    JXTA>uninstjar -f c:/userlib/usrcmds.jar");
        println(" ");
       println("To uninstall all jar-files:");
        println("    JXTA>uninstjar -all");
        println(" ");
        println("SEE ALSO");
        println("    instjar ");
    }
}