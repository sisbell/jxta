/*
 * Copyright (c) 2001-2007 Sun Microsystems, Inc. All rights reserved.
 *  
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *  
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *  
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *  
 *  ====================================================================
 *  
 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */
package net.jxta.test.util;


import java.io.InputStream;
import java.util.Iterator;

import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.endpoint.WireFormatMessage;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.RouteAdvertisement;
import net.jxta.util.CountingOutputStream;
import net.jxta.util.DevNullOutputStream;

import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.impl.endpoint.router.EndpointRouterMessage;
import net.jxta.impl.rendezvous.StdRendezVousService;


/**
 *  Utility class to create the various types of JXTA mesasges
 */
public class MessageUtil {

    static String incarnationTagName = "RdvIncarn" + PeerGroupID.defaultNetPeerGroupID.getUniqueValue().toString();

    /**
     *  Adds source and destination message elements as well as endpoint route
     *  message
     *
     *@param  message       The message to add source and param to
     *@param  srcPeer       source peer to base source address on
     *@param  dstAddress    destination address
     *@param  service       destination service id
     *@param  serviceParam  destination service Param
     */
    public static void addServiceParam(Message message, PeerAdvertisement padv, String myaddress, PeerID dstAddress, String service, String serviceParam) {

        PeerID srcPeer = padv.getPeerID();
        EndpointAddress srcAddr = new EndpointAddress("jxta", srcPeer.toString(), null, null);
        MessageElement srcAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NAME, myaddress, null);

        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_SOURCE_NS, srcAddressElement);

        MessageElement dstAddressElement = new StringMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NAME
                ,
                "jxta://" + dstAddress.getUniqueValue().toString() + "/EndpointService:jxta-NetGroup/EndpointRouter"
                ,
                (MessageElement) null);

        message.replaceMessageElement(EndpointServiceImpl.MESSAGE_DESTINATION_NS, dstAddressElement);

        EndpointRouterMessage erm = new EndpointRouterMessage(message, true);

        erm.setSrcAddress(new EndpointAddress("jxta://" + srcPeer.getUniqueValue().toString()));
        erm.setDestAddress(
                new EndpointAddress("jxta://" + dstAddress.getUniqueValue().toString() + "/" + service + "/" + serviceParam));
        erm.setLastHop(new EndpointAddress("jxta://" + srcPeer.getUniqueValue().toString()));
        erm.setRouteAdv(getRouteAdv(padv, myaddress));
        erm.updateMessage();

    }

    /**
     *  Print  message element names, and sizes to stdout
     *
     *@param  msg  the Message
     */
    public static void printMessageStats(Message message) {
        printMessageStats(message, false);
    }

    /**
     *  Print  message element names, and sizes to stdout
     *  if verbose print element content.
     *
     *@param  msg  the Message
     *@param  verbose  print element content if true
     */
    public static void printMessageStats(Message msg, boolean verbose) {
        try {
            CountingOutputStream cnt;
            Iterator en = msg.getMessageElements();

            System.out.println("------------------Begin Message---------------------");
            WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, new MimeMediaType("application/x-jxta-msg")
                    ,
                    (MimeMediaType[]) null);

            System.out.println("Message Size :" + serialed.getByteLength());
            while (en.hasNext()) {
                MessageElement el = (MessageElement) en.next();
                String eName = el.getElementName();

                cnt = new CountingOutputStream(new DevNullOutputStream());
                el.sendToStream(cnt);
                long size = cnt.getBytesWritten();

                System.out.println("Element " + eName + " : " + size);
                if (verbose) {
                    System.out.println("[" + el + "]");
                }
            }
            System.out.println("-------------------End Message----------------------");
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    /**
     *  Create a Rendezvous Connect Message
     *
     *@param  incarnation  a pre-created incarnation number
     *@param  peeradv      Description of the Parameter
     *@return              The rdv connect message
     */
    public static Message rdvConnectMessage(PeerAdvertisement peeradv, String incarnation) {
        Message msg = new Message();
        PeerID peerid = peeradv.getPeerID();

        try {
            String incStr = peerid.toString() + "#" + incarnation;
            MessageElement el = new StringMessageElement(incarnationTagName, incStr, null);

            msg.addMessageElement("jxta", el);

            XMLDocument doc = (XMLDocument) peeradv.getDocument(MimeMediaType.XMLUTF8);

            msg.replaceMessageElement("jxta", new TextDocumentMessageElement(StdRendezVousService.ConnectRequest, doc, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public static RouteAdvertisement getRouteAdv(PeerAdvertisement padv, String address) {
        RouteAdvertisement route = null;
        
        try {
            route = (RouteAdvertisement)
                    AdvertisementFactory.newAdvertisement(RouteAdvertisement.getAdvertisementType());
            route.setDestPeerID(padv.getPeerID());
            route.addDestEndpointAddress(new EndpointAddress(address));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return route;
    }

}

