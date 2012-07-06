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
 * $Id: relaystatus.java,v 1.14 2007/02/09 23:12:52 hamada Exp $
 */


package net.jxta.impl.shell.bin.relaystatus;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.MessageTransport;
import net.jxta.id.ID;
import net.jxta.impl.endpoint.relay.RelayClient;
import net.jxta.impl.endpoint.relay.RelayServer;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.AccessPointAdvertisement;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RdvAdvertisement;

import java.util.*;

/**
 * Display the list of relays and clients connected to this peer.
 */
public class relaystatus extends ShellApp {

    private boolean full = false;
    private boolean advs = false;

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {

        GetOpt options = new GetOpt(argv, 0, "la");

        while (true) {
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
                case'l':
                    full = true;
                    break;

                case'a':
                    advs = true;
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

        printStatus();

        if (advs)
            printAdvs();

        return ShellApp.appNoError;
    }

    private void printStatus() {

        EndpointService endpoint = getGroup().getEndpointService();

        Iterator it = endpoint.getAllMessageTransports();

        while (it.hasNext()) {
            MessageTransport mt = (MessageTransport) it.next();

            try {
                if (mt instanceof RelayClient) {
                    RelayClient er = (RelayClient) mt;
                    println("Active Relay Servers for '" + mt.getProtocolName() + "' : ");

                    List allRelays = er.getActiveRelays(null);

                    if ((null == allRelays) || allRelays.isEmpty()) {
                        println("\t(none)");
                    } else {
                        for (Object allRelay : allRelays) {
                            AccessPointAdvertisement ap = (AccessPointAdvertisement) allRelay;
                            if (full) {
                                println("\t" + getPeerName(mt, ap) + " [" + ap.getPeerID() + "]");
                            } else {
                                println("\t" + getPeerName(mt, ap));
                            }
                        }
                    }
                }

                if (mt instanceof RelayServer) {
                    RelayServer er = (RelayServer) mt;
                    println("Relayed Clients for '" + mt.getProtocolName() + "' : ");

                    Iterator e = er.getRelayedClients().iterator();

                    if (!e.hasNext()) {
                        println("\t(none)");
                    }

                    while (e.hasNext()) {
                        println("\t" + getPeerInfo(mt, (String) e.next()));
                    }
                }
            } catch (Exception ex) {
                printStackTrace("Error getting relay addresses from : " + mt, ex);
            }
        }
    }

    private String getPeerName(MessageTransport mt, AccessPointAdvertisement adv) {

        EndpointService endpoint = mt.getEndpointService();
        DiscoveryService discovery = endpoint.getGroup().getDiscoveryService();

        PeerID id = adv.getPeerID();
        String res = id.toString();

        Enumeration advs;

        try {
            advs = discovery.getLocalAdvertisements(DiscoveryService.PEER, "PID", id.toString());
        } catch (Exception ez) {
            return res;
        }

        while (advs.hasMoreElements()) {
            try {
                PeerAdvertisement padv = (PeerAdvertisement) advs.nextElement();
                return padv.getName();
            } catch (Exception ez) {
                println("failed with " + ez);

            }
        }

        return res;
    }

    private String getPeerInfo(MessageTransport mt, String str) {

        int idx = str.indexOf(',');

        String peerid = ID.URIEncodingName + ":" + ID.URNNamespace + ":" + str.substring(0, idx);

        EndpointService endpoint = mt.getEndpointService();
        DiscoveryService discovery = null;

        if (null != endpoint) {
            discovery = endpoint.getGroup().getDiscoveryService();
        }

        if (null == discovery) {
            return peerid;
        }

        Enumeration advs;

        try {
            advs = discovery.getLocalAdvertisements(DiscoveryService.PEER, "PID", peerid);
        } catch (Exception ez) {
            return peerid;
        }

        if (advs.hasMoreElements()) {
            PeerAdvertisement padv = (PeerAdvertisement) advs.nextElement();
            if (full)
                return padv.getName() + " [" + peerid + "]";
            else
                return padv.getName();
        }

        return peerid;
    }

    private void printAdvs() {

        Map<ID,RdvAdvertisement> advs = new HashMap<ID,RdvAdvertisement>();

        EndpointService endpoint = getGroup().getEndpointService();

        Iterator<MessageTransport> it = endpoint.getAllMessageTransports();

        while (it.hasNext()) {
            try {
                MessageTransport mt = it.next();

                if ((mt instanceof RelayClient) || (mt instanceof RelayServer)) {

                    endpoint = mt.getEndpointService();
                    DiscoveryService discovery = null;

                    if (null != endpoint) {
                        discovery = endpoint.getGroup().getDiscoveryService();
                    }

                    if (null != discovery) {
                        Enumeration each = discovery.getLocalAdvertisements(DiscoveryService.ADV,
                                RdvAdvertisement.ServiceNameTag,
                                PeerGroup.relayProtoClassID.getUniqueValue().toString());

                        while (each.hasMoreElements()) {
                            Advertisement adv = (Advertisement) each.nextElement();

                            if (adv instanceof RdvAdvertisement) {
                                RdvAdvertisement rdvadv = (RdvAdvertisement) adv;

                                advs.put(rdvadv.getPeerID(), rdvadv);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        println("Relay Advertisements :");

        Iterator eachAdv = advs.values().iterator();

        if (!eachAdv.hasNext()) {
            println("\t(none)");
        }

        while (eachAdv.hasNext()) {
            RdvAdvertisement rdvadv = (RdvAdvertisement) eachAdv.next();

            String name = rdvadv.getName();

            if (null == name) {
                name = "(none)";
            }

            if (full || (null == rdvadv.getName())) {
                name += " [" + rdvadv.getPeerID() + "]";
            }

            println("\t" + name);
        }
    }

    public int syntaxError() {
        consoleMessage("Error - relaystatus [-l] [-a]");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display the list of relays and clients connected to this peer.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     relaystatus - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     relaystatus [-l] [-a]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("   relaystatus displays all the clients and relays to which this peer is connected. ");
        println("");
        println("OPTIONS");
        println("   [-l]      Prints result in long format.");
        println("   [-a]      Prints the known relay advs.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>relaystatus");
        println(" ");
        println("This example displays all the clients and relays to which this client is connected. ");
        println(" ");
        println("SEE ALSO");
        println("    whoami peers route rdvstatus rdv");
    }
}
