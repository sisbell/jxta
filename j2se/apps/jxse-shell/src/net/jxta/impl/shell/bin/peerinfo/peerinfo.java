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
 * $Id: peerinfo.java,v 1.15 2007/02/09 23:12:42 hamada Exp $
 */

package net.jxta.impl.shell.bin.peerinfo;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.meter.MonitorResources;
import net.jxta.meter.PeerMonitorInfo;
import net.jxta.meter.PeerMonitorInfoEvent;
import net.jxta.meter.PeerMonitorInfoListener;
import net.jxta.peer.PeerID;
import net.jxta.peer.PeerInfoService;
import net.jxta.platform.ModuleClassID;
import net.jxta.protocol.PeerAdvertisement;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * peerinfo command gets information about peers locally and remote
 */
public class peerinfo extends ShellApp {
    private PeerInfoService peerInfoService = null;

    public peerinfo() {
    }

    @Override
    public void stopApp() {
    }

    public int startApp(String[] args) {
        PeerID peerID = null;
        String peerName = null;

        ShellEnv env = getEnv();
        peerInfoService = getGroup().getPeerInfoService();

        GetOpt getopt = new GetOpt(args, "p:");

        try {
            for (; ;) {
                int option = getopt.getNextOption();

                if (option < 0)
                    break;

                switch (option) {
                    case'p':
                        String varName = getopt.getOptionArg();
                        ShellObject shellObject = env.get(varName);

                        if (shellObject == null) {
                            println(varName + " is not an shell environment variable name");
                            return ShellApp.appMiscError;
                        }

                        Object obj = shellObject.getObject();

                        if (obj instanceof PeerAdvertisement) {
                            PeerAdvertisement peerAdvertisement = (PeerAdvertisement) obj;
                            peerID = peerAdvertisement.getPeerID();
                            peerName = peerAdvertisement.getName();
                        } else {
                            println(varName + " does not contain a PeerAdvertisement");
                            return ShellApp.appMiscError;
                        }

                        break;

                    default:
                        println("Error: peerinfo this should have never happened");
                }
            }
        } catch (Exception ex) {
            println("Illegal option");
            help();
        }

        try {
            if (peerID == null) {
                PeerMonitorInfo peerMonitorInfo = peerInfoService.getPeerMonitorInfo();
                printPeerMonitorInfo("Local", peerMonitorInfo);
            } else {
                getRemotePeerInfo(peerName, peerID);
            }

            return ShellApp.appNoError;

        } catch (Throwable caught) {
            StringWriter theStackTrace = new StringWriter();
            caught.printStackTrace(new PrintWriter(theStackTrace));
            print(theStackTrace.toString());
            return ShellApp.appMiscError;
        }
    }

    private void getRemotePeerInfo(final String name, final PeerID peerID) {
        try {
            Thread timer = new Thread(new Runnable() {
                long timeout = 30000;
                boolean done;
                PeerMonitorInfo peerMonitorInfo;

                public void run() {
                    try {
                        PeerMonitorInfoListener peerMonitorInfoListener = new PeerMonitorInfoListener() {
                            public void peerMonitorInfoReceived(PeerMonitorInfoEvent evt) {
                                peerMonitorInfo = evt.getPeerMonitorInfo();
                                done = true;
                            }

                            public void peerMonitorInfoNotReceived(PeerMonitorInfoEvent peerMonitorInfoEvent) {
                                done = true;
                            }
                        };

                        peerInfoService.getPeerMonitorInfo(peerID, peerMonitorInfoListener, timeout);

                        for (int i = 0; ; i++) {

                            if (!done) {
                                Thread.sleep(500);
                                if (i == 0)
                                    print("Waiting for remote Peer Montoring Info ");
                                else if ((i % 2) == 1)
                                    print(".");
                            } else
                                break;
                        }

                        println("");

                        if (peerMonitorInfo != null)
                            printPeerMonitorInfo(name, peerMonitorInfo);
                        else
                            print("No response, unable to get PeerMonitorInfo");
                    } catch (Exception e) {
                        println("This should never happen");
                    }
                }
            });

            timer.start();

            timer.join();
        } catch (InterruptedException e) {
        }
    }

    private void printPeerMonitorInfo(String label, PeerMonitorInfo peerMonitorInfo) {
        boolean allowsMonitoring = peerMonitorInfo.allowsMonitoring();

        println("");
        println("Peer Monitoring Info For: " + label);
        println("");
        println("  Monitoring Enabled: " + allowsMonitoring);
        println("  Running Time: " + formatDuration(peerMonitorInfo.getRunningTime()));
        println("  Up Date: " + formatDate(peerMonitorInfo.getLastResetTime()));

        if (allowsMonitoring) {
            println("");

            ModuleClassID moduleClassIDs[] = peerMonitorInfo.getModuleClassIDs();

            if (moduleClassIDs.length == 0) {
                println("  No Registered Service Monitors");
            } else {
                for (ModuleClassID moduleClassID : moduleClassIDs) {
                    String name = MonitorResources.getMonitorTypeName(moduleClassID);
                    if (name == null)
                        name = moduleClassID.toString();

                    println("  " + name + " Monitoring Available");
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Get information about peers";
    }

    @Override
    public void help() {
        println("NAME");
        println("     peerinfo - get information about peers ");
        println(" ");
        println("SYNOPSIS");
        println("     peerinfo ");
        println("           [-p <peerEnv variable>] ");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Use this to get monitoring information about this peer or remote");
        println("peers in this group");
        println(" ");
        println("OPTIONS");
        println("-p peerid");
        println("     gets peer info for a remote peer ");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>peerinfo -p peer2");
        println(" ");
        println("    Get info about peer2");
        println(" ");
        println("SEE ALSO");
        println("    ");
    }

    /// UTILTIES CUT/PASTE from MONITOR APPLICATION

    private static final DecimalFormat secondFormat = new DecimalFormat("00.0");
    private static final DecimalFormat fullTwoDigitFormat = new DecimalFormat("00");
    private static DateFormat timeFormatter = new SimpleDateFormat("hh:mm:ss aa");
    private static DateFormat dateFormatter = new SimpleDateFormat("MM/dd hh:mm:ss aa");
    public static final String UNSET_TIME = "<N/A>";
    public static final String UNSET_DATE = "<N/A>";

    public static String formatDuration(long duration) {
        long min = duration / 60000;
        long hours = duration / 3600000;
        double sec = ((double) (duration % 60000)) / 1000.0;
        min = min % 60;

        return hours + ":" + fullTwoDigitFormat.format(min) + ":" + secondFormat.format(sec);
    }

    public static String formatTime(long time) {
        if (time == 0)
            return UNSET_TIME;
        else
            return timeFormatter.format(new Date(time));
    }

    public static String formatDate(long date) {
        if (date == 0)
            return UNSET_DATE;
        else
            return dateFormatter.format(new Date(date));
    }
}