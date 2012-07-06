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
 * $Id: whoami.java,v 1.26 2007/02/09 23:12:49 hamada Exp $
 */
package net.jxta.impl.shell.bin.whoami;

import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.RouteAdvertisement;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

/**
 * whoami command list the local peer information
 */

public class whoami extends ShellApp {
    
    public whoami() {
    }
    
    public int startApp(String[] args) {
        
        ShellEnv myEnv = getEnv();
        
        boolean full = false;
        boolean viewpg = false;
        
        ShellObject<PeerGroup> obj;
        PeerGroup pg;
        
        GetOpt getopt = new GetOpt(args, "gl");
        
        int c;
        try {
            while ((c = getopt.getNextOption()) != -1) {
                switch (c) {
                    case'g':
                        viewpg = true;
                        break;
                    case'l':
                        full = true;
                        break;
                    default:
                        consoleMessage("Illegal option");
                        syntaxError();
                        return ShellApp.appParamError;
                }
            }
            
            obj = (ShellObject<PeerGroup>) myEnv.get("stdgroup");
            
            // extract the advertisement
            pg = obj.getObject();
            
            if (viewpg) {
                PeerGroupAdvertisement pgAdv = pg.getPeerGroupAdvertisement();
                
                println( "PeerGroup : " + pg );
                
                if (full) {
                    String groupDoc = pgAdv.toString().trim();
                    print("Peer GroupAdvertisement : \n\t" + groupDoc.replace("\n", "\n\t") );
                } else {
                    println( "PeerGroup ID : " + pgAdv.getPeerGroupID() );
                    println( "PeerGroup MSID : " + pgAdv.getModuleSpecID() );
                    println( "PeerGroup Name : " + pgAdv.getName() );
                    XMLElement desc = (XMLElement) pgAdv.getDesc();
                    print( "PeerGroup Description : \n\t" + ((null == desc) ? "** NONE **\n" : desc.toString().trim().replace("\n", "\n\t")) );
                }
                
                println( "PeerGroup parent group : " + pg.getParentGroup() );
                println( "PeerGroup store home : " + pg.getStoreHome() );
                println( "PeerGroup thread group : " + pg.getHomeThreadGroup().getName() );
            } else {
                PeerAdvertisement peerAdv = pg.getPeerAdvertisement();
                Collection<EndpointAddress> endps = getEndpointAddresses(peerAdv);
                
                println( "Peer ID : " + peerAdv.getPeerID() );
                
                if (full) {
                    String peerDoc = peerAdv.toString().trim();
                    print("Peer Advertisement : \n\t" + peerDoc.replace("\n", "\n\t") );
                } else {
                    println( "PeerGroup ID : " + peerAdv.getPeerGroupID() );
                    println( "Peer Name : " + peerAdv.getName() );
                    XMLElement desc = (XMLElement) peerAdv.getDesc();
                    print( "Peer Description : \n\t" + ((null == desc) ? "** NONE **\n" : desc.toString().trim().replace("\n", "\n\t")) );
                    println( "Peer Endpoint Addresses : ");
                    if( endps.isEmpty() ) {
                        println("** NONE **");
                    } else {
                        for (EndpointAddress endp : endps) {
                            println("\t" + endp);
                        }
                    }
                }
            }
            
            return ShellApp.appNoError;
        } catch (Throwable ex) {
            printStackTrace("Exception in command.", ex);
            return ShellApp.appMiscError;
        }
    }
    
    private Collection<EndpointAddress> getEndpointAddresses(PeerAdvertisement peerAdv) {
        
        // Get its EndpointService advertisement
        XMLElement endpParam = (XMLElement) peerAdv.getServiceParam(PeerGroup.endpointClassID);
        
        if (endpParam == null) {
            return Collections.emptyList();
        }
        
        RouteAdvertisement route;
        try {
            Enumeration<XMLElement> paramChilds = endpParam.getChildren(RouteAdvertisement.getAdvertisementType());
            XMLElement param = null;
            
            if (paramChilds.hasMoreElements()) {
                param = paramChilds.nextElement();
            }
            route = (RouteAdvertisement) AdvertisementFactory.newAdvertisement(param);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
        
        if (route == null) {
            return Collections.emptyList();
        }
        
        List<EndpointAddress> addrs = new ArrayList<EndpointAddress>();
        for (Enumeration<String> e = route.getDest().getEndpointAddresses(); e.hasMoreElements();) {
            addrs.add(new EndpointAddress(e.nextElement()));
        }
        
        return addrs;
    }
    
    public int syntaxError() {
        consoleMessage("whoami [-g] [-l]");
        return ShellApp.appParamError;
    }
    
    @Override
    public String getDescription() {
        return "Display information about this peer or the current peergroup";
    }
    
    @Override
    public void help() {
        println("NAME");
        println("     whoami - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     whoami [-l] [-g]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("whoami displays information about a peer or a");
        println("peergroup. With no option, whoami returns");
        println("information about the local peer. The '-g' option returns");
        println("information about the current peer group joined.");
        println("");
        println("By default the Shell is brought up in the 'NetPeerGroup' group. ");
        println("Only the peerId and the peerGroup ID are guaranteed");
        println("to be unique.");
        println("");
        println("OPTIONS");
        println("");
        println("     [-g]  Return info about the current peergroup");
        println("     [-l]  Print the complete advertisement");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>whoami -l");
        println(" ");
        println("This example displays a long version of the peer information");
        println(" ");
        println("SEE ALSO");
        println("    peers");
    }
}
