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
 *  $Id: groups.java,v 1.45 2007/02/09 23:12:51 hamada Exp $
 */
package net.jxta.impl.shell.bin.groups;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.impl.util.TimeUtils;
import net.jxta.protocol.PeerGroupAdvertisement;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;

/**
 * groups command lists groups known locally, and discovers groups
 */
public class groups extends ShellApp {
    private String attr = null;
    private DiscoveryService discovery = null;
    private ShellEnv env;
    private String pid = null;
    int threshold = 10;
    private String val = null;
    private boolean lflag = false;
    private boolean nflag = false;
    private boolean rflag = false;

    /**
     * Description of the Method
     *
     * @param address Peer id or null.
     * @param attr    The search attribute.
     * @param val     The search value.
     * @return error result
     */
    private int discover(String address, String attr, String val) {
        discovery.getRemoteAdvertisements(address, DiscoveryService.GROUP, attr, val, threshold, null);
        consoleMessage("Discovery message sent.");
        return ShellApp.appNoError;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Discover peer groups";
    }

    /**
     * Gets the local attribute of the groups object
     *
     * @param attr attribute value
     * @param val attribute value
     * @return The local value
     */
    private int getLocal(String attr, String val) {
        Enumeration res;
        try {
            res = discovery.getLocalAdvertisements(DiscoveryService.GROUP, attr, val);
        } catch (Exception e) {
            consoleMessage("nothing stored");
            return ShellApp.appNoError;
        }

        int j = 0;
        while (res.hasMoreElements()) {
            Object obj = res.nextElement();
            if (obj instanceof PeerGroupAdvertisement) {
                PeerGroupAdvertisement group = (PeerGroupAdvertisement) obj;
                env.add("group" + j, new ShellObject<PeerGroupAdvertisement>("PeerGroupAdvertisement", group));
                if (lflag) {
                    long life = discovery.getAdvLifeTime(group);
                    long exp = discovery.getAdvExpirationTime(group);
                    println("group" + j + ":"
                            + "\n   Name = " + group.getName()
                            + "\n    GID = " + group.getPeerGroupID()
                            + "\n SpecID = " + group.getModuleSpecID()
                            + "\n  Descr = " + group.getDescription()
                            + "\nExpires = " + new Date(TimeUtils.toAbsoluteTimeMillis(life))
                            + "\n4others = " + exp + " ms");
                } else {
                    println("group" + j + ": name = " + group.getName());
                }
                j++;
                if (nflag && j == threshold) {
                    break;
                }
            }
        }
        return ShellApp.appNoError;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        shortHelp();
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("use to discover peer groups by propagation or at a");
        println("specified peer location. By default \"groups\" lists all the peer");
        println("groups known (cached) by the local peer. The '-r' option is used to send");
        println("a propagate request to find new peer groups.");
        println("\"groups\" stores results in the local cache, and inserts ");
        println("advertisement(s) into the environment, using the default");
        println("naming: groupX where X is a growing integer number.");
        println(" ");
        println("OPTIONS");
        println("-p peerid");
        println("     discovers groups at a given peer location");
        println("-r");
        println("     discovers groups by propagation");
        println("-l");
        println("     displays group id as a hex string");
        println("-a");
        println("     specify Attribute name to limit discovery to");
        println("-v");
        println("     specify Attribute value to limit discovery to");
        println("-n");
        println("     limit the number of responses to n from a single peer");
        println("-f");
        println("     flush group advertisements");
        println(" ");
        println("     no option returns known peer group advertisements");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>groups -r");
        println("or");
        println("    JXTA>groups -r -aName -vsocce* ");
        println("or");
        println("    JXTA>groups -r -aName -v*occer");
        println(" ");
        println("SEE ALSO");
        println("    whoami chpgrp join peers");
    }


    /*
     *  makes best attempt at removing env objects created this command
     */
    private void removeEnv() {
        String groupenv = "group0";
        int i = 0;
        while (env.contains(groupenv)) {
            env.remove(groupenv);
            groupenv = "group" + ++i;
        }
    }

    /**
     * Description of the Method
     */
    public void shortHelp() {
        println("     groups - discover peer groups ");
        println(" ");
        println("SYNOPSIS");
        println("    groups [-p peerid name attribute] ");
        println("           [-n n] limit the number of responses to n from a single peer");
        println("           [-r] discovers peer groups using remote propagation");
        println("           [-l] displays group id as a hex string");
        println("           [-a] specify Attribute name to limit discovery to");
        println("           [-v] specify Attribute value to limit discovery to. wild card is allowed");
        println("           [-f] flush group advertisements");
    }


    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        boolean flush = false;
        env = getEnv();
        discovery = getGroup().getDiscoveryService();
        if (args == null) {
            return (getLocal(null, null));
        }
        GetOpt getopt = new GetOpt(args, "rflp:n:a:v:");
        int c;
        try {
            while ((c = getopt.getNextOption()) != -1) {
                switch (c) {
                    case'p':
                        pid = getopt.getOptionArg();
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
                            return ShellApp.appParamError;
                        }
                        break;
                    default:
                        consoleMessage("Error: groups this should have never happened");

                }
            }

            if (flush) {
                try {
                    Enumeration eachAdv = discovery.getLocalAdvertisements(DiscoveryService.GROUP, attr, val);
                    while (eachAdv.hasMoreElements()) {
                        Advertisement anAdv = (Advertisement) eachAdv.nextElement();
                        discovery.flushAdvertisement(anAdv);
                    }

                    removeEnv();
                } catch (IOException e) {
                    printStackTrace("Flush failed", e);
                    return ShellApp.appMiscError;
                }
                return ShellApp.appNoError;
            }

            if (rflag) {
                return (discover(pid, attr, val));
            } else {
                return (getLocal(attr, val));
            }
        } catch (Exception ex) {
            printStackTrace("Illegal option: ", ex);
            shortHelp();
        }
        return ShellApp.appNoError;
    }
}

