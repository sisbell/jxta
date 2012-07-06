/*
 * Copyright (c) 2002 Sun Microsystems, Inc.  All rights reserved.
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
 * $Id: rshd.java,v 1.16 2007/02/09 23:12:42 hamada Exp $
 */

package net.jxta.impl.shell.bin.rshd;

import net.jxta.endpoint.*;
import net.jxta.impl.shell.ShellApp;

import java.util.*;

/**
 * Remote JXTA Shell Deamon
 */
public class rshd extends ShellApp implements EndpointListener {

    static public final String ThisShell = "RemoteShellDeamon";
    static public final long INACTIVITY_TIMEOUT = 5 * 60 * 1000; // 5 Minutes

    static public final String NAME = "JxtaRshd";

    static public final String SESSION_REQUEST = "SessionRequest";
    static public final String SESSION_CLOSE = "SessionClose";
    static public final String SESSION_GRANTED = "SessionGranted";
    static public final String SESSION_DENIED = "SessionDenied";
    static public final String DATA = "RshdData";

    static public final String APP_ARGS = "RshdAppArgs";

    EndpointService endpoint = null;
    Timer timer = new Timer(true);

    /**
     * noarg constructor
     */
    public rshd() {
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                Thread.currentThread().setName("Shell Session Inactivity Timer");
            }
        }, 0);
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {

        endpoint = getGroup().getEndpointService();

        String pParam = getGroup().getPeerGroupID().getUniqueValue().toString();
        try {
            endpoint.addIncomingMessageListener(this, NAME, pParam);

        } catch (Exception ez1) {
            // Not much we can do here.
            printStackTrace("Cannot attach endpoint listener", ez1);
            return ShellApp.appMiscError;
        }

        consoleMessage("daemon started");

        return ShellApp.appSpawned;
    }

    @Override
    public void stopApp() {
        // Remove itself from the parent ShellEnv (GC)

        super.stopApp();
        timer.cancel();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Remote JXTA Shell Deamon";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     rshd - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     rshd");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Runs the Remote Shell server. rshd runs in background, allowing");
        println("incoming connections using the command rsh.");
        println("The shell command login is used in order to authentified the user:");
        println("the user and password of the peer running rshd will have to be provided");
        println("by the user.");
        println(" ");
        println("SEE ALSO");
        println("    login Shell rsh");
    }

    private String[] processArgs(String str) {

        List<String> v = new ArrayList<String>();

        StringTokenizer st = new StringTokenizer(str);
        while (st.hasMoreTokens()) {
            v.add(st.nextToken());
        }

        return v.toArray(new String[v.size()]);
    }

    private boolean processConnectRequest(Message msg, EndpointAddress srcAddr) {

        String remoteAddr = null;

        MessageElement el = msg.getMessageElement(null, SESSION_REQUEST);
        if (el != null) {
            remoteAddr = el.toString();
        }

        // Check if there is arguments
        String[] appArgs;

        el = msg.getMessageElement(null, APP_ARGS);
        if (el != null) {
            appArgs = processArgs(el.toString());
        } else
            appArgs = new String[0];

        RemoteShellSession app;
        try {
            app = new RemoteShellSession(this, srcAddr, remoteAddr);
        } catch (Exception ez1) {
            return false;
        }

        app.init(getGroup(), null, null);

        exec(app, appArgs);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void processIncomingMessage(Message msg, EndpointAddress srcAddr, EndpointAddress dstAddr) {

        try {
            if (msg.getMessageElement(null, SESSION_REQUEST) != null) {
                processConnectRequest(msg, srcAddr);
            }
        } catch (Exception ez1) {
            printStackTrace("Failure in processing connect request", ez1);
        }
    }
}
