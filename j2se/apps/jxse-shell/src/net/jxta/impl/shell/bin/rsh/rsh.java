/*
 * Copyright (c) 2002 Sun Microsystems, Inc.  All rights
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
 * $Id: rsh.java,v 1.12 2007/02/09 23:12:48 hamada Exp $
 */
package net.jxta.impl.shell.bin.rsh;

import net.jxta.endpoint.*;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.impl.shell.bin.rshd.rshd;
import net.jxta.protocol.PeerAdvertisement;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * connects to a remote JXTA Shell
 */
public class rsh extends ShellApp implements EndpointListener {

    private String myAddress = null;
    private String hostAddress = null;
    private EndpointService endpoint = null;
    private Messenger messengerCmd = null;
    private Messenger messengerData = null;
    private String appArgs = "";

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {

        ShellEnv env = getEnv();
        PeerAdvertisement hostAdv = null;
        String fileName = null;
        boolean secure = true;
        String scriptFile;

        endpoint = getGroup().getEndpointService();

        if ((args != null) && (args.length > 0)) {

            try {
                for (int i = 0; i < args.length; ++i) {
                    if (args[i].equals("-r")) {
                        if (hostAdv != null) {
                            return syntaxError();
                        }
                        hostAddress = args[++i];
                        continue;
                    }

                    if (args[i].equals("-f")) {
                        if (fileName != null) {
                            return syntaxError();
                        }
                        fileName = args[++i];
                        scriptFile = processScriptFile(fileName);
                        appArgs += ("-e");
                        appArgs += (scriptFile);
                        continue;
                    }

                    if (args[i].equals("-unsecure")) {
                        if (hostAddress != null) {
                            return syntaxError();
                        }
                        secure = false;
                        continue;
                    }

                    if (args[i].equals("-p")) {

                        if (hostAddress != null) {
                            return syntaxError();
                        }
                        String hostAdvName = args[++i];

                        ShellObject obj = env.get(hostAdvName);
                        if (obj == null) {
                            consoleMessage("cannot access " + hostAdvName);
                            return ShellApp.appMiscError;
                        }

                        try {
                            hostAdv = (PeerAdvertisement) obj.getObject();
                        } catch (Exception e) {
                            consoleMessage(hostAdvName + " is not an Peer Advertisement");
                            return ShellApp.appMiscError;
                        }
                    }
                }
            } catch (Exception e) {
                return syntaxError();
            }
        }

        if (hostAddress == null) {
            if (hostAdv == null) {
                return syntaxError();
            }

            String hostID = hostAdv.getPeerID().getUniqueValue().toString();
            if (secure) {
                hostAddress = "jxtatls://" + hostID;
            } else {
                hostAddress = "jxta://" + hostID;
            }
        }

        if (!createLocalEndpoint()) {
            consoleMessage("Could not register local Endpoint listener");
            return ShellApp.appMiscError;
        } else {
            consoleMessage("Local listener installed.");
        }

        try {
            if (!connectToHost()) {
                consoleMessage("Could not connect to remote host " + hostAddress);

                return ShellApp.appMiscError;
            }

            if (fileName == null) {
                processUserCmds();
            }

            consoleMessage("rsh [Disconnected]");
            return ShellApp.appNoError;
        } finally {
            deleteLocalEndpoint();
        }
    }


    private void deleteLocalEndpoint() {
        endpoint.removeIncomingMessageListener(rshd.NAME, myAddress);
    }

    private boolean createLocalEndpoint() {

        myAddress = Long.toHexString(System.currentTimeMillis());
        try {
            endpoint.addIncomingMessageListener(this, rshd.NAME, myAddress);
        } catch (Exception ez1) {
            return false;
        }
        return true;
    }

    private boolean connectToHost() {

        String completeHostAddress = hostAddress + "/" +
                rshd.NAME + "/" +
                getGroup().getPeerGroupID().getUniqueValue().toString();

        consoleMessage("Connecting to " + hostAddress);

        try {
            EndpointAddress addr = new EndpointAddress(completeHostAddress);
            messengerCmd = endpoint.getMessenger(addr);

            if (null == messengerCmd) {
                consoleMessage("Could not get messenger for " + completeHostAddress);
                return false;
            }

            return sendConnectRequest();
        } catch (Exception ez1) {
            printStackTrace("Could not get messenger for " + completeHostAddress, ez1);
            return false;
        }
    }

    private boolean sendConnectRequest() {

        // Check that the EndpointService can talk to that peer

        try {
            Message msg = new Message();

            msg.addMessageElement(null,
                    new StringMessageElement(rshd.SESSION_REQUEST, myAddress, null));

            // Add optional arguments
            if (!appArgs.equals("")) {

                msg.addMessageElement(null,
                        new StringMessageElement(rshd.APP_ARGS, appArgs, null));
            }

            messengerCmd.sendMessage(msg);

            return true;
        } catch (Exception ez1) {
            printStackTrace("Could not send connect request", ez1);
            return false;
        }
    }

    private void processSessionGranted(String remoteAddress) {

        consoleMessage("Connected to " + remoteAddress);

        String completeHostAddress = hostAddress + "/" + remoteAddress;

        try {
            EndpointAddress addr = new EndpointAddress(completeHostAddress);

            messengerData = endpoint.getMessenger(addr);
        } catch (Exception ez1) {
            printStackTrace("Could not get messenger for " + completeHostAddress, ez1);
        }
    }

    public void send(String data) {
        try {
            Message msg = new Message();
            MessageElement input = new StringMessageElement(rshd.DATA, data, null);
            msg.addMessageElement(null, input);

            messengerData.sendMessage(msg);
        } catch (Exception ez1) {
            //ignored
        }
    }

    private void processUserCmds() {

        while (true) {
            try {
                String line = waitForInput();
                if (line.equals("~.")) {
                    send("\u0004");
                    break;
                } else {
                    send(line);
                }
            } catch (Exception ez1) {
                printStackTrace("Failure processing commands", ez1);
            }
        }
    }

    private String processScriptFile(String fn) {

        BufferedReader scriptReader;
        String res = "";

        try {
            scriptReader = new BufferedReader(new FileReader(fn));
            String cmd;

            while ((cmd = scriptReader.readLine()) != null) {
                res += cmd + ";";
            }

            scriptReader.close();
            return res;

        } catch (Exception e) {
            printStackTrace("Cannot process " + fn, e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void processIncomingMessage(Message msg, EndpointAddress srcAddr, EndpointAddress dstAddr) {

        try {
            MessageElement el = msg.getMessageElement(null, rshd.SESSION_GRANTED);
            if (el != null) {
                processSessionGranted(el.toString());
            }

            el = msg.getMessageElement(null, rshd.DATA);
            if (el != null) {
                print(el.toString());
            }
        } catch (Throwable ez1) {
            printStackTrace("Failure processing message from remote", ez1);
        }
    }

    private int syntaxError() {

        consoleMessage("Usage: rsh [-p <peeradv> [-unsecure] | -r <addr>] [-f <file>]");

        return ShellApp.appParamError;
    }

    @Override
    public String getDescription() {
        return "Connects to a remote JXTA Shell";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     rsh - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     rsh [-p <peeradv> [-unsecure] | -r <addr>] [-f <file>]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Connects to a JXTA peer that is running the 'rshd' daemon.");
        println("The remote peer can be specified by its peer advertisement ");
        println("by one of its endpoint address. If the peer is specified by its");
        println("peer advertisement then communication will default to the ");
        println("secure TLS Message transport. If the peer is specified via a ");
        println("transport address then that addresss will be used.");
        println(" ");
        println("Security: the default normal mode to use rsh is to use the option -p.");
        println("Since the connection then uses the JXTA TLS Endpoint Transport,");
        println("the connection is guaranteed to be secure.");
        println("Otherwise, text will go clear onto the wire, which is not secure.");
        println(" ");
        println("The normal way to exit rsh is to type at the begining of a line '~.'");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("  -p <peeradv>   Set the peer advertismeent of the remote host");
        println("  -unsecure      Force the connection to NOT be secure");
        println("  -r <addr>      Set the endpoint address (URI) of the remote host");
        println("  -f <file>      Specifies a shell script to run on the remote host");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("      JXTA> peers");
        println("      peer0: remote RSHD peer");
        println(" ");
        println("      JXTA> rsh -p peer0");
        println(" or");
        println("      JXTA> rsh -r tcp://192.168.1.10:9701");
        println(" ");
        println("SEE ALSO");
        println("     rshd Shell login");
    }
}
