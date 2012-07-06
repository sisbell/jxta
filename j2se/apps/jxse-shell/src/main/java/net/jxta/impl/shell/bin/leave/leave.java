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
 * $Id: leave.java,v 1.22 2007/02/09 23:12:54 hamada Exp $
 */
package net.jxta.impl.shell.bin.leave;

import net.jxta.exception.PeerGroupException;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;

/**
 * Leave a peer group
 */
public class leave extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        ShellEnv env = getEnv();

        ShellObject obj;
        boolean stopGroup = false;

        GetOpt options = new GetOpt(argv, 0, "k");

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
                case'k':
                    stopGroup = true;
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

        obj = env.get("stdgroup");
        PeerGroup pg = (PeerGroup) obj.getObject();

        MembershipService membership = pg.getMembershipService();

        try {
            // Resign our identity.
            membership.resign();
        } catch (PeerGroupException failed) {
            printStackTrace("Peer Group resign failed.", failed);
        }

        if (stopGroup) {
            exec(null, "chpgrp", new String[]{"-p"}, env);

            pg.stopApp();
        }

        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("Usage: leave [-k] ");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Resign from and optionally stop a peer group";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     leave - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     leave [-k]");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("     [-k]   The peer group will be stopped and deleted.");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println(" The 'leave' command is used to leave a group that was ");
        println(" previously joined via a 'join' command. The 'join' command is");
        println(" used to join a peergroup. If the '-k' option was used, then this");
        println(" instance of the group is stopped and deleted and the current");
        println(" group is changed to the peer group's parent. Before a user ");
        println(" can use the group  again, the user will have to rejoin the ");
        println(" group via the 'join' command.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("      JXTA>mygroupadv = newpgrp -n mygroup");
        println("      JXTA>join -d mygroupadv");
        println("      JXTA>leave -k");
        println("      JXTA>");
        println(" ");
        println(" This creates a new peer group which is cloning the services");
        println(" of the parent peer group. You can find the services of the");
        println(" current peergroup via the command 'whoami -g'. The new group");
        println(" is given the name 'mygroup'. Finally, the group is stopped ");
        println(" using the 'leave' command. ");
        println(" ");
        println("SEE ALSO");
        println("    newpgrp join login who chpgrp ");
    }
}
