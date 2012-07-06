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
 * $Id: instjar.java,v 1.11 2007/02/09 23:12:52 hamada Exp $
 */
package net.jxta.impl.shell.bin.instjar;

import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellCmds;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 'instjar' Shell command to add user commands contained in the jar-file to be installed.
 */
public class instjar extends ShellApp {
    private List<Serializable> instJarList;
    protected ShellCmds cmds;

    /**
     * Returns a right aligned String of the specified number with the spcified length.
     *
     * @param num    the number to be returned right aligned.
     * @param strLen the total length of the returned String.
     * @return a right aligned String representation of the specified number.
     */
    private static String formatNumber(int num, int strLen) {
        StringBuilder buf = new StringBuilder();

        buf.append(num);
        while (buf.length() < strLen)
            buf.insert(0, ' ');
        return buf.toString();
    }

    /**
     * Lists all intalled jars.
     */
    protected void showInstalledJars() {
        int maxNumWidth = (instJarList.size() + "").length();
        for (int i = 0; i < instJarList.size(); i++)
            println(formatNumber(i, maxNumWidth) + " " + instJarList.get(i).toString());
    }

    /**
     * Installs a jar-file or a directory.
     *
     * @param file the jar-file to be installed.
     */
    protected void installJar(File file) {
        instJarList.add(file);
    }

    public int startApp(String[] args) {
        // Obtain a list containing all installed files.
        cmds = new ShellCmds(getEnv());
        instJarList = new ArrayList<Serializable>(Arrays.asList(cmds.getInstJars()));

        if (args.length == 0) {
            // No arguments were specified, so we have to list all installed jar-files.
            showInstalledJars();
            return ShellApp.appNoError;
        }

        // Install all correct files given as arguments to this method.
        for (String arg : args) {
            File file = new File(arg);
            if (file.exists()) {
                if (!instJarList.contains(file)) {
                    // We do not want dublicates.
                    installJar(file);
                }
            } else {
                println("Error: '" + file + "' does not exist");
            }
        }

        // Set the new value of environment variable INST_JARS.
        cmds.setInstJars(instJarList);

        return ShellApp.appNoError;
    }

    /**
     * Returns a short description for this command (used by the Shell command 'man').
     *
     * @return a short discription of this command.
     */
    @Override
    public String getDescription() {
        return "Installs jar-files containing additional Shell commands";
    }

    /**
     * Show help text.
     */
    @Override
    public void help() {
        println("NAME");
        println("     instjar - installs one or more jar-files containing");
        println("     additional Shell commands.");
        println(" ");
        println("SYNOPSIS");
        println("     instjar [<jar-file> [...]]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("This command installs jar-files containing additional Shell");
        println("commands. An arbirtary number of paths to jar-files");
        println("can be supplied. If this command is invoked with no arguments");
        println("it lists all installed jar-files. Use the given indices to");
        println("uninstall jar-files with the 'uninstjar' command.");
        println("The class-files of a command 'xxx' must be in the package");
        println("'net.jxta.impl.shell.bin.xxx' and the main-class must be named");
        println("'net.jxta.impl.shell.bin.xxx.xxx'. Accordingly the class-files must");
        println("be placed in a directory 'net/jxta/impl/shell/bin/xxx/' within the");
        println("jar-file, and must extend the ShellApp class.");
        println("The list of installed jar-files is stored in the environment");
        println("variable '" + ShellCmds.INST_JARS + "'.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("To install two jar-files 'c:/userlib/usrcmds.jar' and");
        println("'c:/userlib/toolscmds.jar:");
        println("    JXTA>instjar c:/userlib/usrcmds.jar c:/userlib/toolcmds.jar");
        println(" ");
        println("To list all installed jar-files with their indices:");
        println("    JXTA> instjar");
        println("    0 c:/userlib/usrcmds.jar");
        println("    1 c:/userlib/toolcmds.jar");
        println("These indeces can be used to uninstall some jar-files with the");
        println("'uninstjar' command.");
        println(" ");
        println("SEE ALSO");
        println("    uninstjar ");
    }
}
