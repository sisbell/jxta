/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 * $Id: transports.java,v 1.12 2007/02/09 23:12:48 hamada Exp $
 */

package net.jxta.impl.shell.bin.transports;

import net.jxta.endpoint.*;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;

import java.util.Iterator;

/**
 * Display information about the message transports available in the current group
 */
public class transports extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {

        ShellObject obj;

        ShellEnv myEnv = getEnv();

        obj = myEnv.get("stdgroup");

        PeerGroup pg = (PeerGroup) obj.getObject();
        EndpointService ep = pg.getEndpointService();

        Iterator eachTransport = ep.getAllMessageTransports();

        println("Message Transports for group : " + pg.getPeerGroupName());

        while (eachTransport.hasNext()) {
            MessageTransport aTransport = (MessageTransport) eachTransport.next();

            println("\nMessage Transport : '" + aTransport.getProtocolName()
                    + "' in " + aTransport.getEndpointService().getGroup().getPeerGroupName() + " [" + aTransport.getEndpointService().getGroup().getPeerGroupID() + "]");

            if (aTransport instanceof MessageSender) {
                MessageSender sender = (MessageSender) aTransport;

                println("\tMessage Sender : " + sender.getPublicAddress());
                println("\t\tAllow Routing : " + sender.allowsRouting());
                println("\t\tConnection Oriented : " + sender.isConnectionOriented());
            }

            if (aTransport instanceof MessagePropagater) {
                MessagePropagater propagater = (MessagePropagater) aTransport;
                println("\tMessage Propagater : " + propagater.getPublicAddress());
            }

            if (aTransport instanceof MessageReceiver) {
                println("\tMessage Receiver : ");

                Iterator eachPublicAddress = ((MessageReceiver) aTransport).getPublicAddresses();

                while (eachPublicAddress.hasNext()) {
                    EndpointAddress anEndpointAddress = (EndpointAddress) eachPublicAddress.next();

                    println("\t\tAddress : " + anEndpointAddress);
                }
            }
        }


        return ShellApp.appNoError;
    }

    public int syntaxError() {
        consoleMessage("Error: transports");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display information about the message transports available in the current group";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     transports - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     transports ");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Display information about the messge transports available in");
        println("the current group");
        println("");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>transports");
        println(" ");
        println("This example displays the current message transports in this group.");
        println(" ");
    }

}
