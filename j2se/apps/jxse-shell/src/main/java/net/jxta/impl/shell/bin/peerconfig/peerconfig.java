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
 * $Id: peerconfig.java,v 1.15 2007/02/09 23:12:43 hamada Exp $
 */
package net.jxta.impl.shell.bin.peerconfig;

import net.jxta.impl.shell.ShellApp;
import net.jxta.peergroup.PeerGroup;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Force Peer Reconfiguration
 */
public class peerconfig extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {

        PeerGroup group = getGroup();

        try {
            setReconfigure(group.getStoreHome(), true);
        } catch (Exception ex1) {
            printStackTrace("Error resetting configuration", ex1);
            return ShellApp.appMiscError;
        }

        consoleMessage("Please exit and restart the JXTA shell to reconfigure !!!!!");
        return ShellApp.appNoError;
    }

    void setReconfigure(URI jxtaHome, boolean reconfigure) {
        if (!"file".equalsIgnoreCase(jxtaHome.getScheme())) {
            return;
        }

        File jxtaHomeDir = new File(jxtaHome);
        File f = new File(jxtaHomeDir, "reconf");

        if (reconfigure) {
            try {
                f.createNewFile();
            } catch (IOException ex1) {
                printStackTrace("Could not create 'reconf' file", ex1);
                consoleMessage("Create the file 'reconf' by hand before retrying.");
            }
        } else {
            try {
                f.delete();
            } catch (Exception ex1) {
                printStackTrace("Could not remove 'reconf' file", ex1);
                consoleMessage("Delete the file 'reconf' by hand before retrying.");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Force Peer Reconfiguration";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     peerconfig - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     peerconfig");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("peerconfig is used to re-configure a peer. The command");
        println("forces the platform to show a configuration dialog the next");
        println("time it is started.");
        println("  ");
        println("After the command is run the shell needs to be restarted in ");
        println("order to display the configuration dialog.");
        println(" ");
        println("FILES");
        println(" ");
        println("   reconf          - File touched to force reconfiguration.");
        println("   PlatformConfig  - Platform Configuration file.");
    }
}
