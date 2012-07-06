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
 * $Id: flush.java,v 1.6 2007/02/09 23:12:45 hamada Exp $
 */
package net.jxta.impl.shell.bin.flush;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

import java.io.IOException;

/**
 * flush command to remove a document from cache
 */
public class flush extends ShellApp {
    private ShellEnv env;
    private DiscoveryService discovery = null;

    @Override
    public void stopApp() {
    }

    public int startApp(String[] args) {

        env = getEnv();
        discovery = getGroup().getDiscoveryService();

        if (args == null) {
            shortHelp();
            return ShellApp.appNoError;
        }

        GetOpt getopt = new GetOpt(args, "d:");

        int c;
        while ((c = getopt.getNextOption()) != -1) {
            switch (c) {
                case'd':
                    return flush(getopt.getOptionArg());
                default:
                    consoleMessage("this should have never happened");
            }
        }
        return ShellApp.appNoError;
    }

    private int flush(String name) {

        ShellObject obj = env.get(name);
        if (obj == null) {
            consoleMessage("'" + name + "' not found");
            return ShellApp.appMiscError;
        }

        Object adv = obj.getObject();

        if (adv instanceof Advertisement) {
            try {
                discovery.flushAdvertisement((Advertisement) adv);
                env.remove(name);
            } catch (IOException e) {
                printStackTrace("error removing Advertisement object. ", e);
                return ShellApp.appMiscError;
            }
        } else {
            shortHelp();
        }
        return ShellApp.appNoError;
    }

    @Override
    public String getDescription() {
        return "flush a jxta advertisement";
    }

    public void shortHelp() {
        println("NAME");
        println("     flush - flush a jxta advertisement");
        println(" ");
        println("SYNOPSIS");
        println("    flush  [-d document] advertisement to remove");
        println(" ");
    }

    @Override
    public void help() {
        shortHelp();
        println("DESCRIPTION");
        println(" ");
        println("use \"flush\" to flush an advertisement from cache ");
        println(" ");
        println("OPTIONS");
        println("-d document");
        println("     env advertisement to remove");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>flush -d peer10");
        println(" ");
        println("    removes the peer advertisement peer10");
        println(" ");
    }
}
