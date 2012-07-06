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
 * $Id: join.java,v 1.37 2007/02/09 23:12:44 hamada Exp $
 */
package net.jxta.impl.shell.bin.join;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;
import net.jxta.protocol.PeerGroupAdvertisement;

/**
 * join command list the local peer information
 */
public class join extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {

        ShellEnv env = getEnv();
        ShellObject obj;

        String advObj = null;
        String credDoc = null;

        boolean beRendezvous = false;
        boolean authenticate = true;
        boolean start = false;

        GetOpt options = new GetOpt(argv, 0, "d:c:rAs");

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
                case'd':
                    advObj = options.getOptionArg();
                    break;

                case'c':
                    credDoc = options.getOptionArg();
                    break;

                case'A':
                    authenticate = false;
                    break;

                case'r':
                    beRendezvous = true;
                    break;

                case's':
                    start = true;
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

        if (null == advObj) {
            return exec(null, "chpgrp", new String[0], env);
        }

        /*
        * create the Peergroup from the passed adv. This kind of make sense for now...
        */

        obj = env.get(advObj);
        if (obj == null) {
            consoleMessage("'" + advObj + "' not found.");
            return syntaxError();
        }

        if (!PeerGroupAdvertisement.class.isAssignableFrom(obj.getObjectClass())) {
            consoleMessage("'" + advObj + "' is not a Peer Group Advertisment.");
            return syntaxError();
        }

        PeerGroup pg;
        try {
            PeerGroupAdvertisement adv = (PeerGroupAdvertisement) obj.getObject();

            pg = getGroup().newGroup(adv); // does not startApp()

            if (beRendezvous) {
                // Start the rendezvous if requested.
                consoleMessage("Starting rdv");
                pg.getRendezVousService().startRendezVous();
            }
        } catch (Exception e) {
            printStackTrace("'" + advObj + "' could not be instantiated.", e);
            return ShellApp.appMiscError;
        }

        String returnvar = getReturnVariable();

        if (null == returnvar) {
            returnvar = env.createName();
        }

        env.add(returnvar, new PeerGroupShellObject("PeerGroup", pg));

        String chpgrpparams[] = {returnvar};
        int result = exec(null, "chpgrp", chpgrpparams, env);

        if (ShellApp.appNoError != result) {
            consoleMessage("Could not change to peer group.");
            return result;
        }

        if (start) {
            result = pg.startApp(new String[0]);
        }

        if (ShellApp.appNoError != result) {
            consoleMessage("Failed starting peer group.");
            return result;
        }

        // Authenticate if requested.
        if (authenticate) {
            String loginparams[] = {};

            if (null != credDoc) {
                loginparams = new String[]{"-c", credDoc};
            }

            result = exec(null, "login", loginparams, env);
        }

        if (ShellApp.appNoError != result) {
            consoleMessage("Could not login to peer group.");
            return result;
        }

        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("Usage: join [-r] [-A] [-s] [-c <credential>] [-d <adv>] ");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Instantiate and join peer group";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     join - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     join [-r] [-A] [-s] [-c <credential>] [-d <adv>] ");
        println("     ");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("The 'join' command is used to instantiate and join a peergroup ");
        println("that was created via the 'newpgrp' command or using an ");
        println(" advertisement that was previously discovered. ");
        println(" ");
        println("If no argument is given, join lists all the existing groups");
        println("and the current group on the local peer. When a group is ");
        println("joined successfully, an environment variable is created. This");
        println("variable holds the group object. ");
        println(" ");
        println("Upon joining the peer group, depending on the membership");
        println("authentication required, the user will be asked for the");
        println("identity he/she wants to have in the peer group. An identity");
        println("is used to assign credentials to users when accessing peer");
        println("group resources. Each peer group can define their own set of ");
        println("identities available in the peer group. ");
        println(" ");
        println("SAMPLE");
        println(" ");
        println("    JXTA> join mygroup");
        println("     identity:tra");
        println("     passwd:XXXXX");
        println("    JXTA>");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("    [-r]              Act as a rendezvous for the joined group.");
        println("    [-A]              Instantiate, but do not authenticate (join) this peer group.");
        println("    [-d <adv>]        Specify a shell variable holding a peergroup advertisement.");
        println("    [-c <credential>] Specify a credential to join the peer group.");
        println("    [-s]              Start the group. (calls \"startApp()\")");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("      JXTA>mygroupadv = newpgrp -n mygroup");
        println("      JXTA>grp = join -d mygroupadv");
        println(" ");
        println("This creates a new peer group which is cloning the services ");
        println("of the parent peer group. You can find the services of the ");
        println("current peergroup via the command 'whoami -g'. The new group ");
        println("is given the name 'mygroup'. ");
        println(" ");
        println("SEE ALSO");
        println("      newpgrp mkadv login leave chpgrp who groups");
    }

    /**
     * Unrefs the group.
     */
    public static class PeerGroupShellObject extends ShellObject<PeerGroup> {

        public PeerGroupShellObject(String name, PeerGroup group) {
            super(name, group);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * <p/>Closes the pipe if it is still open.
         */
        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            PeerGroup group = getObject();

            group.unref();
        }
    }
}
