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
 * $Id: RemoteShellSession.java,v 1.4 2007/02/09 23:12:42 hamada Exp $
 */
package net.jxta.impl.shell.bin.rshd;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.impl.shell.*;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;

import java.io.IOException;
import java.util.TimerTask;
import net.jxta.peergroup.PeerGroup;

/**
 * Remote JXTA Shell Deamon
 */
class RemoteShellSession extends ShellApp implements Runnable {

    static final String INITIAL_APP = "login";
    static final String LOGIN_INITIAL_APP = "Shell";

    private rshd server = null;
    private ShellConsole cons = null;
    private InputPipe inputPipe = null;
    private OutputPipe outputPipe = null;

    private String remoteAddress = null;
    private String localAddress = null;

    private long lastActivity;
    private TimerTask timerTask = null;

    private String[] appArgs = null;

    public RemoteShellSession(rshd server, EndpointAddress addr, String remoteAddr) throws IOException {

        this.server = server;
        try {
            this.remoteAddress =
                    addr.getProtocolName()
                            + "://" + addr.getProtocolAddress() + "/"
                            + rshd.NAME + "/" +
                            remoteAddr;

        } catch (Exception ez1) {
            throw new IOException();
        }
        long currentTime = System.currentTimeMillis();
        this.localAddress = Long.toHexString(currentTime);

        lastActivity = currentTime;
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] appArgs) {

        this.appArgs = appArgs;

        try {
            cons = new EndpointRemoteShellConsole(this, this,
                    localAddress,
                    remoteAddress,
                    server.endpoint);
        } catch (IOException failed) {
            return ShellApp.appMiscError;
        }

        ShellEnv env = new ShellEnv();

        setEnv(env);

        env.add("console", new ShellObject<ShellConsole>("console", cons));
        env.add("stdgroup", new ShellObject<PeerGroup>("Default Group", getGroup()));

        // Create the default InputPipe
        inputPipe = new ShellInputPipe(getGroup(), cons);
        outputPipe = new ShellOutputPipe(getGroup(), cons);

        setInputPipe(inputPipe);
        setOutputPipe(outputPipe);

        setInputConsPipe(inputPipe);
        setOutputConsPipe(outputPipe);

        env.add("stdin", new ShellObject<InputPipe>("stdin", inputPipe));
        env.add("stdout", new ShellObject<OutputPipe>("stdout", outputPipe));
        env.add("consin", new ShellObject<InputPipe>("consin", inputPipe));
        env.add("consout", new ShellObject<OutputPipe>("consout", outputPipe));

        timerTask = new TimerTask() {

            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                if ((currentTime - lastActivity) > rshd.INACTIVITY_TIMEOUT) {
                    consprintln(" ");
                    consoleMessage("Inactivity timeout, your session has automatically shutdown.");
                    stopApp();
                }
            }
        };

        try {
            server.timer.schedule(timerTask, rshd.INACTIVITY_TIMEOUT, rshd.INACTIVITY_TIMEOUT);
        } catch (Exception ez1) {
            printStackTrace("Cannot set inactivity timer: ", ez1);
        }

        Thread t = new Thread(getGroup().getHomeThreadGroup(), this, "Remote Shell Session");
        t.setDaemon(true); // FIXME 20040421 jice  Questionable
        t.start();

        // As far as the invoker is concerned, we're finished here.
        return ShellApp.appSpawned;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Spawn a sperate thread and let the rshd server's thread go. It is still the incoming
     * message thread !
     */
    public void run() {
        try {
            String[] intialArgs = new String[appArgs.length + 3];

            intialArgs[0] = "-s";
            intialArgs[1] = LOGIN_INITIAL_APP;
            intialArgs[2] = "--";

            System.arraycopy(appArgs, 0, intialArgs, 3, appArgs.length);

            exec(null, INITIAL_APP, intialArgs, getEnv());

            // exec is always in the foreground. When it returns, the app is finished. Tear down.
            // The app does not know it is top-level because if we told it that, it would also
            // try and create its own window and would actually use the remote peer's one
            // if can't make one. We do not have a middle-ground concept.
            // So, it is us that do all the tear-down.
        } finally {
            stopApp();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stopApp() {
        super.stopApp();

        println(" ");
        println("Remote shell session terminated.");
        println("type ~. to exit rsh");
        println(" ");
        // Give a couple of seconds to help ensure that the message will be sent.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
        }
        getInputPipe().close();
        getOutputPipe().close();

        if (timerTask != null) {
            timerTask.cancel();
        }

        if (cons != null) {
            cons.destroy();
        }
    }

    synchronized void updateActivity() {
        lastActivity = System.currentTimeMillis();
    }
}
