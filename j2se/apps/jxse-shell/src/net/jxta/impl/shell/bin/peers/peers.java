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
 * $Id: peers.java,v 1.53 2007/02/09 23:12:53 hamada Exp $
 */

package net.jxta.impl.shell.bin.peers;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.impl.util.TimeUtils;
import net.jxta.protocol.PeerAdvertisement;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;

/**
 * peers command lists peers known locally, and discovers peers
 */
public class peers extends ShellApp {
    private ShellEnv env;
    private DiscoveryService discovery = null;
    private int threshold = 10;
    private String pid = null;
    private String attr = null;
    private String val = null;
    private boolean nflag = false;
    private boolean rflag = false;
    private boolean lflag = false;
    private boolean pflag = false;

    public peers() {
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        boolean flush = false;
        env = getEnv();
        discovery = getGroup().getDiscoveryService();
        GetOpt getopt = new GetOpt(args, "rflp:n:a:v:");

        int c;
        try {
            while ((c = getopt.getNextOption()) != -1) {
                switch (c) {
                    case'p':
                        pid = getopt.getOptionArg();
                        pflag = true;
                        break;
                    case'r':
                        rflag = true;
                        break;
                    case'l':
                        lflag = true;
                        break;
                    case'f':
                        flush = true;
                        break;

                    case'n':
                        nflag = true;
                        threshold = Integer.parseInt(getopt.getOptionArg());
                        break;
                    case'a':
                        attr = getopt.getOptionArg();
                        break;
                    case'v':
                        val = getopt.getOptionArg();
                        if ((val.length() == 1) && (val.indexOf("*") != -1)) {
                            //let's stop here
                            consoleMessage("* is not allowed, you must specify at least one char");
                            return ShellApp.appNoError;
                        }
                        break;
                    default:
                        consoleMessage("Illegal option");
                        shortHelp();
                        return ShellApp.appParamError;
                }
            }

            if (flush) {
                try {
                    Enumeration eachAdv = discovery.getLocalAdvertisements(DiscoveryService.PEER, attr, val);

                    while (eachAdv.hasMoreElements()) {
                        Advertisement anAdv = (Advertisement) eachAdv.nextElement();

                        discovery.flushAdvertisement(anAdv);
                    }

                    discovery.publish(getGroup().getPeerAdvertisement());
                    removeEnv();
                } catch (IOException e) {
                    printStackTrace("Flush failed", e);
                    return ShellApp.appMiscError;
                }
                return ShellApp.appNoError;
            }

            if (rflag || pflag) {
                return (discover(pid, attr, val));
            } else {
                return (getLocal(attr, val));
            }
        } catch (Throwable ex) {
            printStackTrace("Exception in command.", ex);
            return ShellApp.appMiscError;
        }
    }

    private int discover(String address, String attr, String val) {

        discovery.getRemoteAdvertisements(address, DiscoveryService.PEER, attr, val, threshold, null);
        consoleMessage("peer discovery message sent");
        return ShellApp.appNoError;
    }

    private int getLocal(String attr, String val) {

        Enumeration res;

        try {
            res = discovery.getLocalAdvertisements(DiscoveryService.PEER, attr, val);
        } catch (Exception e) {
            printStackTrace("Discovery Failed", e);
            return ShellApp.appMiscError;
        }

        int j = 0;

        while (res.hasMoreElements()) {
            PeerAdvertisement peer = (PeerAdvertisement) res.nextElement();
            env.add("peer" + j, new ShellObject<PeerAdvertisement>("PeerAdvertisement", peer));

            String name = peer.getName();
            if ((null == name) || name.equals("")) {
                name = "(Anonymous Peer)";
            }

            if (lflag) {
                long life = discovery.getAdvLifeTime(peer);
                long exp = discovery.getAdvExpirationTime(peer);
                println("peer" + j + ": ID = " + peer.getPeerID() +
                        " name = " + name +
                        "\nExpires on : " + new Date(TimeUtils.toAbsoluteTimeMillis(life)) +
                        "\nFor Others : " + exp + " ms");
            } else {
                println("peer" + j + ": name = " + name);
            }

            j++;
            if (nflag && j == threshold) {
                break;
            }
        }
        return ShellApp.appNoError;
    }

    @Override
    public String getDescription() {
        return "Discover peers";
    }

    public void shortHelp() {
        println("NAME");
        println("     peers - discover peers ");
        println(" ");
        println("SYNOPSIS");
        println("     peers [-p peerid name attribute] ");
        println("           [-n n] limit the number of responses to n from a single peer");
        println("           [-r] discovers peers using propagate");
        println("           [-l] displays peer id as a hex string");
        println("           [-a] specify Attribute name to limit discovery to");
        println("           [-v] specify Attribute value to limit discovery to. wild card is allowed");
        println("           [-f] flush peer advertisements");
    }

    @Override
    public void help() {
        shortHelp();
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("use \"peers\" to discover other peers within a peer group or at a");
        println("specified peer location. Running \"peers\" command with no options lists");
        println("only the peers already known by the peer (cached). The '-r' option is used");
        println("to send a propagate request to find new peers.");
        println("peers stores results in the local cache, and inserts ");
        println("advertisement(s) into the environment, using the default");
        println("naming: peerX where X is a growing integer number.");
        println(" ");
        println("OPTIONS");
        println("-p peerid");
        println("     discovers peers at a given peer location");
        println("-r");
        println("     discovers peers using remote propagation");
        println("-l");
        println("     displays peer id as a hex string");
        println("-a");
        println("     specify Attribute name to limit discovery to");
        println("-v");
        println("     specify Attribute value to limit discovery to");
        println("-n");
        println("     limit the number of responses to n from a single peer");
        println("-f");
        println("     flush peer advertisements");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>peers -r");
        println("or");
        println("    JXTA>peers -r -aName -vluxor* ");
        println(" ");
        println("SEE ALSO");
        println("    whoami chpgrp join groups");
    }

    /* makes best attempt at removing env objects created this command
     */
    private void removeEnv() {
        String peerenv = "peer0";
        int i = 0;
        while (env.contains(peerenv)) {
            env.remove(peerenv);
            peerenv = "peer" + ++i;
        }
    }
}
