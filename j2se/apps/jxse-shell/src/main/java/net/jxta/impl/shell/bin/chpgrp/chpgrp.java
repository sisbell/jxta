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
 * $Id: chpgrp.java,v 1.17 2007/02/09 23:12:53 hamada Exp $
 */
package net.jxta.impl.shell.bin.chpgrp;

import net.jxta.impl.shell.*;
import net.jxta.impl.shell.bin.Shell.Shell;
import net.jxta.peergroup.PeerGroup;

import java.util.Enumeration;

/**
 * This command changes the ShellObject associated with the 'stdgroup' Object.
 */
public class chpgrp extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {

        ShellEnv env = getEnv();
        boolean parent = false;

        GetOpt options = new GetOpt(argv, 0, "p");

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

                case'p':
                    parent = true;
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        String newgroup = null;
        if (!parent) {
            newgroup = options.getNextParameter();
        } else {
            ShellObject<PeerGroup> obj = (ShellObject<PeerGroup>) env.get("stdgroup");
            PeerGroup me = obj.getObject();

            PeerGroup myParent = me.getParentGroup();

            Enumeration<String> eachEnv = env.elements();
            while (eachEnv.hasMoreElements()) {
                String anEnv = eachEnv.nextElement();

                ShellObject<?> anObj = env.get(anEnv);

                if ((null != anObj) && (anObj.getObject() instanceof PeerGroup)) {
                    PeerGroup check = (PeerGroup) anObj.getObject();

                    if (myParent.getPeerGroupID().equals(check.getPeerGroupID())) {
                        newgroup = anEnv;
                        break;
                    }
                }
            }

            if (null == newgroup) {
                consoleMessage("Cannot change to parent group. No suitable environment variable");
                return syntaxError();
            }
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }

        if (null == newgroup) {
            // list all the active peerGroups

            PeerGroup current = (PeerGroup) env.get("stdgroup").getObject();

            Enumeration<String> eachEnv = env.elements();
            while (eachEnv.hasMoreElements()) {
                String anEnv =eachEnv.nextElement();

                ShellObject<?> anObj = env.get(anEnv);

                if ((null != anObj) && (anObj.getObject() instanceof PeerGroup)) {

                    print(anEnv);

                    PeerGroup check = (PeerGroup) anObj.getObject();

                    String name = check.getPeerGroupName();

                    if (null == name) {
                        name = check.getPeerGroupID().toString();
                    }

                    print("\t\"" + name + "\"");

                    if (current.getPeerGroupID().equals(check.getPeerGroupID())) {
                        print("\t(current)"); // mark this as the current group
                    }

                    println(" ");
                }
            }

            return ShellApp.appNoError;
        }

        // ok switch to the new group

        ShellObject<PeerGroup> obj = (ShellObject<PeerGroup>) env.get(newgroup);

        if (null == obj) {
            consoleMessage("The object '" + newgroup + "' does not exist.");
            return syntaxError();
        }

        if (!PeerGroup.class.isInstance(obj.getObject())) {
            consoleMessage("The object '" + newgroup + "' is not a PeerGroup object");
            return syntaxError();
        }

        env.add("stdgroup", obj);

        // if this is a root shell then change the status group.
        ShellObject<ShellConsole> consoleObject = (ShellObject<ShellConsole>) env.get("console");

        ShellObject<Shell> shellObject = (ShellObject<Shell>) env.get("shell");

        if ((null != consoleObject) && (null != shellObject)) {
            Shell shell = shellObject.getObject();

            ShellConsole console = consoleObject.getObject();

            if (shell.isRootShell()) {
                console.setStatusGroup(obj.getObject());
            }
        }

        return ShellApp.appNoError;
    }

    /**
     * Prints out an error message, if the input command line parameters are wrong
     */
    private int syntaxError() {
        consoleMessage("Usage: chpgrp [-p | <env>]");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Change the current peer group";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("    chpgrp  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("    chpgrp [-p | <env>]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("The 'chpgrp' command is used to switch the default Shell peer");
        println("group 'stdgroup' variable to another group that was previously");
        println("joined via a 'join' command. The 'join' command is used to join");
        println("a peergroup.");
        println(" ");
        println("After changing groups, the Shell 'stdgroup' variable is set to ");
        println("the value of the new peer group joined.");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("    [-p]        Change to this peer group's parent peer group.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>mygroupadv = newpgrp -n mygroup");
        println("    JXTA>join -d mygroupadv");
        println("    JXTA>chpgrp moi");
        println(" ");
        println(" This creates a new peergroup which is cloning the services");
        println(" of the parent peer group. You can find the services of the");
        println(" current peer group via the command 'whoami -g'. The new group");
        println(" is given the name 'mygroup'. Before you can do anything with");
        println(" the group you need to join the group via the 'join' command.");
        println(" The 'chpgrp' command is used to change the default group");
        println(" to the group 'moi'.");
        println(" ");
        println("SEE ALSO");
        println("     mkadv newpgrp join leave");
    }
}
