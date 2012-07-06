/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution,
 *  if any, must include the following acknowledgment:
 *  "This product includes software developed by the
 *  Sun Microsystems, Inc. for Project JXTA."
 *  Alternately, this acknowledgment may appear in the software itself,
 *  if and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *  not be used to endorse or promote products derived from this
 *  software without prior written permission. For written
 *  permission, please contact Project JXTA at http://www.jxta.org.
 *
 *  5. Products derived from this software may not be called "JXTA",
 *  nor may "JXTA" appear in their name, without prior written
 *  permission of Sun.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of Project JXTA.  For more
 *  information on Project JXTA, please see
 *  <http://www.jxta.org/>.
 *
 *  This license is based on the BSD license adopted by the Apache Foundation.
 *
 *  $Id: rdvstatus.java,v 1.27 2007/02/16 03:50:58 mcumings Exp $
 */
package net.jxta.impl.shell.bin.rdvstatus;

import net.jxta.discovery.DiscoveryService;
import net.jxta.id.ID;
import net.jxta.impl.rendezvous.RendezVousServiceInterface;
import net.jxta.impl.rendezvous.rpv.PeerView;
import net.jxta.impl.rendezvous.rpv.PeerViewElement;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.rendezvous.RendezVousService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Display information about the rendezvous service.
 */
public class rdvstatus extends ShellApp {
    DiscoveryService discovery;
    
    Map<ID,String> names = new HashMap<ID,String>();
    
    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        
        boolean verbose = false;
        
        GetOpt options = new GetOpt(args, 0, "v");
        
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
                
                case'v':
                    verbose = true;
                    break;
                    
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }
        
        if (null != options.getNextParameter()) {
            consoleMessage("Unexpected parameter.");
            return syntaxError();
        }
        
        RendezVousService rdv = getGroup().getRendezVousService();
        
        if (null == rdv) {
            consoleMessage("No Rendezvous Service in group ");
            return ShellApp.appMiscError;
        }
        
        discovery = getGroup().getDiscoveryService();
        
        println(" ");
        println("Rendezvous Status: ");
        println("__________________");
        println(" ");
        
        println("Current configuration : " + rdv.getRendezVousStatus());
        
        RendezVousServiceInterface stdRdv;
        net.jxta.impl.rendezvous.StdRendezVousService stdRdvProvider = null;
        if (rdv instanceof net.jxta.impl.rendezvous.RendezVousServiceInterface) {
            stdRdv = (RendezVousServiceInterface) rdv;
            PeerView rpv = null;
            
            if (null != stdRdv) {
                net.jxta.impl.rendezvous.RendezVousServiceProvider provider = stdRdv.getRendezvousProvider();
                
                if (provider instanceof net.jxta.impl.rendezvous.StdRendezVousService) {
                    stdRdvProvider = (net.jxta.impl.rendezvous.StdRendezVousService) provider;
                }
                
                rpv = stdRdv.getPeerView();
            }
            
            if (null != rpv) {
                PeerViewElement self = rpv.getSelf();
                if (verbose) {
                    print("\t" + self.getRdvAdvertisement().getPeerID());
                }
                println("\t" + self);
                
                List<PeerViewElement> pves = new ArrayList<PeerViewElement>(rpv.getView());
                Collections.reverse(pves);
                
                println(" ");
                println("Peer View : ");
                
                if (!pves.isEmpty()) {
                    for (PeerViewElement eachPVE : pves) {
                        if (verbose) {
                            print("\t" + eachPVE.getRdvAdvertisement().getPeerID());
                        }
                        print("\t" + eachPVE);
                        
                        if (rdv.isRendezVous()) {
                            if (eachPVE == rpv.getUpPeer()) {
                                println("\t(UP)");
                            } else if (eachPVE == rpv.getDownPeer()) {
                                println("\t(DOWN)");
                            } else {
                                println("");
                            }
                        } else {
                            println("");
                        }
                    }
                } else {
                    println("\t[None]");
                }
            }
        }
        println(" ");
        
        if (!rdv.isRendezVous()) {
            Enumeration rdvs = rdv.getConnectedRendezVous();
            
            println("Rendezvous Connections :");
            if (!rdvs.hasMoreElements()) {
                println("\t[None]");
                
            } else {
                while (rdvs.hasMoreElements()) {
                    try {
                        ID connection = (PeerID) rdvs.nextElement();
                        if (verbose) {
                            print("\t" + connection);
                        }
                        if (null != stdRdvProvider) {
                            println("\t" + stdRdvProvider.getPeerConnection(connection));
                        } else {
                            String peerName = idToName(connection);
                            println("\t" + peerName);
                        }
                    } catch (Exception e) {
                        printStackTrace("failed", e);
                    }
                }
            }
            println(" ");
            
            Enumeration rmRdvs = rdv.getDisconnectedRendezVous();
            
            println("Rendezvous Disconnections :");
            if (!rmRdvs.hasMoreElements()) {
                println("\t[None]");
            } else {
                while (rmRdvs.hasMoreElements()) {
                    try {
                        ID connection = (PeerID) rmRdvs.nextElement();
                        print("\t");
                        if (verbose) {
                            print("\t" + connection);
                        }
                        String peerName = idToName(connection);
                        println("\t" + peerName);
                    } catch (Exception e) {
                        printStackTrace("failed", e);
                    }
                }
                
            }
            println(" ");
        }
        
        // no need to display clients if the peer is not a rendezvous for
        // the group
        if (rdv.isRendezVous()) {
            
            Enumeration clients = rdv.getConnectedPeers();
            
            println("Rendezvous Client Connections :");
            if (!clients.hasMoreElements()) {
                println("\t[None]");
                
            } else {
                while (clients.hasMoreElements()) {
                    try {
                        ID connection = (PeerID) clients.nextElement();
                        if (verbose) {
                            print("\t" + connection);
                        }
                        if (null != stdRdvProvider) {
                            println("\t" + stdRdvProvider.getPeerConnection(connection));
                        } else {
                            String peerName = idToName(connection);
                            println("\t" + peerName);
                        }
                    } catch (Exception e) {
                        println("failed with " + e);
                    }
                }
            }
            println(" ");
        }
        
        return ShellApp.appNoError;
    }
    
    private int syntaxError() {
        consoleMessage("usage : rdvstatus [-v] ");
        return ShellApp.appParamError;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display information about the rendezvous service";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     rdvstatus - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     rdvstatus [-v]");
        println(" ");
        println("OPTIONS");
        println("     [-v]    print verbose information");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'rdvstatus' displays information about the rendezvous");
        println("service in the current group. The command shows the current");
        println("peerview and any rendezvous or client connections.");
        println("");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>rdvstatus");
        println(" ");
        println(" ");
        println("SEE ALSO");
        println("    whoami peers");
    }
    
    
    /**
     * Description of the Method
     *
     * @param id Description of Parameter
     * @return Description of the Returned Value
     */
    private String idToName(ID id) {
        
        String idstring = id.toString();
        String name = names.get(id);
        
        if (null != name) {
            return name;
        }
        
        try {
            Enumeration res;
            
            if (id instanceof PeerID) {
                res = discovery.getLocalAdvertisements(DiscoveryService.PEER, "PID", idstring);
                
                if (res.hasMoreElements()) {
                    name = ((PeerAdvertisement) res.nextElement()).getName();
                }
            } else if (id instanceof PeerGroupID) {
                res = discovery.getLocalAdvertisements(DiscoveryService.GROUP, "GID", idstring);
                
                if (res.hasMoreElements()) {
                    name = ((PeerGroupAdvertisement) res.nextElement()).getName();
                }
            }
        } catch (IOException failed) {
            println("failed with " + failed);
        }
        
        if (null != name) {
            names.put(id, name);
        } else {
            name = "[unknown]";
        }
        
        return name;
    }
}

