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
 * $Id: mem.java,v 1.6 2007/02/09 23:12:42 hamada Exp $
 */
package net.jxta.impl.shell.bin.mem;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;

/**
 * Display memory information
 */
public class mem extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {

        boolean forceGC = false;

        GetOpt options = new GetOpt(argv, "g");
        int c;
        while ((c = options.getNextOption()) != -1) {
            switch (c) {
                case'g':
                    forceGC = true;
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        if (forceGC) {
            Runtime.getRuntime().runFinalization();
            Runtime.getRuntime().gc();
            Runtime.getRuntime().gc();
        }

        consoleMessage(" Free: " + Runtime.getRuntime().freeMemory() / 1024 + "K " +
                " Total : " + Runtime.getRuntime().totalMemory() / 1024 + "K " +
                " Max : " + Runtime.getRuntime().maxMemory() / 1024 + "K ");

        //println(" Cpu(s): " + Runtime.getRuntime().availableProcessors());
        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("Usage: mem [-g] ");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display memory information";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     mem - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("    mem [-g]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("use \"mem\" to display memory information.");
        println("OPTIONS");
        println("-g");
        println("     Garbage collect prior to displaying memory");
        println(" ");
    }
}
