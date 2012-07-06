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
 * $Id: newpgrp.java,v 1.9 2007/02/09 23:12:47 hamada Exp $
 */
package net.jxta.impl.shell.bin.newpgrp;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;

/**
 * newpgrp command creates a peer group adv
 */
public class newpgrp extends ShellApp {

    private String name = null;

    public newpgrp() {
    }

    public int startApp(String[] argv) {
        boolean parentInID = false;
        boolean ownModuleSpecID = false;
        String idFormat = null;

        ShellEnv env = getEnv();

        GetOpt options = new GetOpt(argv, 0, "n:psi:");

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
                case'n':
                    name = options.getOptionArg();
                    break;

                case'p':
                    parentInID = true;
                    break;

                case's':
                    ownModuleSpecID = true;
                    break;

                case'i':
                    idFormat = options.getOptionArg();
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

        if (parentInID && (null != idFormat)) {
            consoleMessage("Cannot simultaneously specify ID Format and require parent ID.");
            return syntaxError();
        }

        try {
            PeerGroup parent = (PeerGroup) env.get("stdgroup").getObject();
            ModuleImplAdvertisement groupImplAdv = parent.getAllPurposePeerGroupImplAdvertisement();

            if (ownModuleSpecID) {
                groupImplAdv.setModuleSpecID(IDFactory.newModuleSpecID(groupImplAdv.getModuleSpecID().getBaseClass()));
            }

            PeerGroupAdvertisement adv = (PeerGroupAdvertisement)
                    AdvertisementFactory.newAdvertisement(
                            PeerGroupAdvertisement.getAdvertisementType());

            adv.setName(name);
            adv.setModuleSpecID(groupImplAdv.getModuleSpecID());

            PeerGroupID gid;

            if (parentInID) {
                gid = IDFactory.newPeerGroupID(parent.getPeerGroupID());
            } else {
                if (null != idFormat)
                    gid = IDFactory.newPeerGroupID(idFormat);
                else
                    gid = IDFactory.newPeerGroupID();

            }
            adv.setPeerGroupID(gid);
            adv.setDescription("created by newpgrp");

            ShellObject<PeerGroupAdvertisement> newAdv = new ShellObject<PeerGroupAdvertisement>("PeerGroup Advertisement", adv);
            env.add(getReturnVariable(), newAdv);

            if (ownModuleSpecID) {
                if (null != name) {
                    groupImplAdv.setDescription("For PeerGroup " + gid + " (" + name + ")");
                } else {
                    groupImplAdv.setDescription("For PeerGroup " + gid);
                }
            }

            // It is necessary to publish the impl adv in order to
            // be able to later instantiate the group. If we don't
            // do it now, the impl adv may be lost and never
            // recoverable.
            DiscoveryService disco = parent.getDiscoveryService();

            disco.publish(groupImplAdv);

            return ShellApp.appNoError;
        } catch (Throwable all) {
            printStackTrace("Failure creating peer group advertisment", all);
            return ShellApp.appMiscError;
        }
    }

    @Override
    public void stopApp() {
    }

    public int syntaxError() {
        consoleMessage("Usage: newpgrp [-p | -i <idformat>] [-n <name>] [-s]");
        return ShellApp.appParamError;
    }

    @Override
    public String getDescription() {
        return "Create a new peer group advertisement";
    }

    @Override
    public void help() {
        println("NAME");
        println("     newpgrp - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     newpgrp [-p | -i <idformat>] [-n <name>]");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("      [-i <idformat>]  The ID Format to use for the group and default for all IDs created in group.");
        println("      [-n <name>]      Optional name for the peer group");
        println("      [-p]             The parent group should be explicit in the new peer group's ID.");
        println("      [-s]             The created group will use a new unique ModuleSpecID.");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println(" newpgrp creates a new peer group advertisement with a random group ");
        println(" id which uses the same implementataion as the current peer group. You ");
        println(" can find the services of the current peergroup via the command ");
        println(" 'whoami -g'.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("          JXTA>mygroupadv = newpgrp mygroup");
        println(" ");
        println(" This creates a new peergroup advertisement with the name 'mygroup'. ");
        println(" Before you can do anything with the peergroup you need to join the ");
        println(" group via the 'join' command.");
        println(" ");
        println("SEE ALSO");
        println("    whoami join leave peers chpgrp");
    }
}
