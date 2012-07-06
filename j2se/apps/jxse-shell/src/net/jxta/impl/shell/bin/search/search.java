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
 * $Id: search.java,v 1.35 2007/02/09 23:12:51 hamada Exp $
 */

package net.jxta.impl.shell.bin.search;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.impl.util.TimeUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;

/**
 * Discover jxta advertisements
 */
public class search extends ShellApp {
    private ShellEnv env;
    private DiscoveryService discovery = null;
    private int threshold = Integer.MAX_VALUE;
    private String pid = null;
    private String attr = null;
    private String val = null;
    private boolean rflag = false;
    private boolean pflag = false;
    private boolean sflag = false;
    private boolean lflag = false;

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        boolean flush = false;

        env = getEnv();
        discovery = getGroup().getDiscoveryService();

        GetOpt getopt = new GetOpt(args, "rlfp:n:a:v:");
        int c;

        while ((c = getopt.getNextOption()) != -1) {
            switch (c) {
                case'p':
                    pid = getopt.getOptionArg();
                    pflag = true;
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
                    threshold = Integer.parseInt(getopt.getOptionArg());
                    break;

                case'a':
                    sflag = true;
                    attr = getopt.getOptionArg();
                    break;

                case'v':
                    sflag = true;
                    val = getopt.getOptionArg();
                    if (val.equals("*")) {
                        //let's stop here
                        consoleMessage("* is not allowed, you must specify at least one char");
                        return ShellApp.appParamError;
                    }
                    break;

                default:
                    consoleMessage("Illegal option");
                    syntaxError();
                    return ShellApp.appParamError;
            }
        }

        if (null != getopt.getNextParameter()) {
            consoleMessage("Unexpected Parameter");
            syntaxError();
            return ShellApp.appParamError;
        }

        if (flush) {
            try {
                Enumeration eachAdv = discovery.getLocalAdvertisements(DiscoveryService.ADV, attr, val);

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

        try {
            if (rflag || pflag) {
                return (discover(pid, attr, val));
            } else {
                return (getLocal(attr, val));
            }
        } catch (Throwable ex) {
            printStackTrace("Error", ex);
            return ShellApp.appMiscError;
        }
    }

    private int discover(String address, String attr, String val) {

        discovery.getRemoteAdvertisements(
                address, DiscoveryService.ADV, attr, val, threshold, null);
        consoleMessage("JXTA Advertisement search message sent");
        return ShellApp.appNoError;
    }

    private int getLocal(String attr, String val) {

        Enumeration res;

        try {
            res = discovery.getLocalAdvertisements(DiscoveryService.ADV, attr, val);
        } catch (Exception e) {
            printStackTrace("Could not get Advertisements", e);
            return ShellApp.appMiscError;
        }

        removeEnv();

        int j = 0;
        while (res.hasMoreElements()) {
            Advertisement adv = (Advertisement) res.nextElement();
            String msg = "";
            if (sflag) {
                msg = "(Search criteria: Attribute=\"" + attr + "\" Value=\"" + val + "\")";
            }
            env.add("adv" + j, new ShellObject<Advertisement>(adv.getAdvType(), adv));
            if (lflag) {
                long life = discovery.getAdvLifeTime(adv);
                long exp = discovery.getAdvExpirationTime(adv);
                println("JXTA Advertisement adv" + j + " [" + adv.getAdvType() + "] " + msg +
                        "\nExpires on : " + new Date(TimeUtils.toAbsoluteTimeMillis(life)) +
                        "\nFor Others : " + exp + " ms");
            } else {
                println("JXTA Advertisement adv" + j + " [" + adv.getAdvType() + "] " + msg);
            }
            j++;
            if (j >= threshold) {
                break;
            }
        }
        return ShellApp.appNoError;
    }

    @Override
    public String getDescription() {
        return "Discover jxta advertisements";
    }

    public int syntaxError() {
        consoleMessage("usage - [(-p <peerid> | -r ) [-n <num>] | -f | -l] [-a <attr> -v <value>]");
        return ShellApp.appParamError;
    }

    @Override
    public void help() {
        println("NAME");
        println("     search - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("    search [(-p <peerid> | -r ) [-n <num>] | -f | -l] [-a <attr> -v <value>]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("use \"search\" to search for advertisements within a peer group or at a");
        println("specified peer location. Running \"search\" command with no options lists");
        println("only the advertisements already known by the peer (cached). The '-r' option is used");
        println("to send a propagate request to find new advertisements.");
        println("search stores results in the local cache, and inserts ");
        println("advertisement(s) into the environment, using the default");
        println("naming: advX where X is a growing integer number.");
        println(" ");
        println("OPTIONS");
        println("    [-p <peerid>]  Searchs for advertisements at a given peer location.");
        println("    [-r]           Search advertisements using remote propagation.");
        println("    [-a <attr>]    Specify Attribute name to limit search to.");
        println("    [-v <value>]   Specify Attribute value to limit search to.");
        println("    [-n <num>]     Limit the number of responses from a single peer.");
        println("    [-f]           Flush advertisement cache of adv matching <attr> and <value>.");
        println("    [-l]           Verbose display of advertisement info.");
        println("     ");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>search -r");
        println(" ");
        println("    Search for new advertisements in the current peer group");
        println(" ");
        println("SEE ALSO");
        println("    whoami publish info peers groups");
    }

    /**
     * makes best attempt at removing env objects created this command
     */
    private void removeEnv() {
        String advenv = "adv0";
        int i = 0;
        while (env.contains(advenv)) {
            env.remove(advenv);
            advenv = "adv" + ++i;
        }
    }
}
