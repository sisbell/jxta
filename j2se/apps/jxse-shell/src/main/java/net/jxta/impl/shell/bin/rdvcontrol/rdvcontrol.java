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
 *  $Id: rdvcontrol.java,v 1.14 2007/05/17 03:42:15 bondolo Exp $
 */
package net.jxta.impl.shell.bin.rdvcontrol;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.id.IDFactory;
import net.jxta.impl.rendezvous.RendezVousServiceInterface;
import net.jxta.impl.rendezvous.rpv.PeerView;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.peer.PeerID;
import net.jxta.rendezvous.RendezVousService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Control the rendezvous service.
 */
public class rdvcontrol extends ShellApp {
    RendezVousService rdv;

    boolean verbose = false;

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {

        rdv = getGroup().getRendezVousService();

        if (null == rdv) {
            consoleMessage("No Rendezvous Service in group " + getGroup().getPeerGroupName());
            return ShellApp.appMiscError;
        }

        GetOpt options = new GetOpt(args, "v");

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

                case'v':
                    verbose = true;
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        String command = options.getNextParameter();

        if (null == command) {
            consoleMessage("Missing <command>");
            return syntaxError();
        }

        String subcommand = options.getNextParameter();

        if (null == subcommand) {
            consoleMessage("Missing <subcommand>");
            return syntaxError();
        }

        String subargs[] = new String[args.length - options.getNextOptionIndex()];

        System.arraycopy(args, options.getNextOptionIndex(), subargs, 0, subargs.length);

        try {
            if ("rpv".equals(command)) {
                if ("addseed".equals(subcommand)) {
                    return rpv_addseed(subargs);
                } else if ("reseed".equals(subcommand)) {
                    return rpv_reseed(subargs);
                } else if ("probe".equals(subcommand)) {
                    return rpv_probe();
                } else if ("delete".equals(subcommand)) {
                    return rpv_delete(subargs, false);
                } else if ("fail".equals(subcommand)) {
                    return rpv_delete(subargs, true);
                } else {
                    consoleMessage("Unrecognized sub-command : " + subcommand);
                    return syntaxError();
                }
            } else if ("rdv".equals(command)) {
                if ("start".equals(subcommand)) {
                    return rdv_start();
                } else if ("stop".equals(subcommand)) {
                    return rdv_stop();
                } else {
                    consoleMessage("Unrecognized sub-command : " + subcommand);
                    return syntaxError();
                }
            } else if ("edge".equals(command)) {
                if ("connect".equals(subcommand)) {
                    return edge_connect(subargs);
                } else if ("disconnect".equals(subcommand)) {
                    return edge_disconnect(subargs);
                }
                if ("challenge".equals(subcommand)) {
                    return edge_challenge();
                } else {
                    consoleMessage("Unrecognized sub-command : " + subcommand);
                    return syntaxError();
                }
            } else if ("auto".equals(command)) {
                if ("start".equals(subcommand)) {
                    return auto_start(subargs);
                } else if ("stop".equals(subcommand)) {
                    return auto_stop(subargs);
                } else {
                    consoleMessage("Unrecognized sub-command : " + subcommand);
                    return syntaxError();
                }
            } else {
                consoleMessage("Unrecognized command : " + command);
                return syntaxError();
            }
        } catch (Throwable all) {
            printStackTrace("Caught Throwable while processing command.", all);
        }

        return ShellApp.appMiscError;
    }

    private int rpv_addseed(String[] subargs) {
        GetOpt options = new GetOpt(subargs, "");

        while (true) {
            //FIXME this does not loop
            int option = options.getNextOption();

            if (-1 == option) {
                break;
            }

            switch (option) {
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        String seed = options.getNextParameter();

        if (null == seed) {
            consoleMessage("Missing <seed>");
            return syntaxError();
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unexpected parameter");
            return syntaxError();
        }

        RendezVousServiceInterface stdRdv;
        if (rdv instanceof RendezVousServiceInterface) {
            stdRdv = (RendezVousServiceInterface) rdv;

            PeerView rpv = stdRdv.getPeerView();
            
            if( null == rpv) {
                consoleMessage("No peerview available.");
                return ShellApp.appMiscError;
            }
            
            rpv.addSeed(URI.create(seed));
        } else {
            consoleMessage("Rendezvous is not of correct type");
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    private int rpv_reseed(String[] subargs) {
        if (subargs.length > 0) {
            consoleMessage("Unexpected parameter");
            return syntaxError();
        }

        RendezVousServiceInterface stdRdv;
        if (rdv instanceof RendezVousServiceInterface) {
            stdRdv = (RendezVousServiceInterface) rdv;

            PeerView rpv = stdRdv.getPeerView();

            if( null == rpv) {
                consoleMessage("No peerview available.");
                return ShellApp.appMiscError;
            }
            
            rpv.seed();
        } else {
            consoleMessage("Rendezvous is not of correct type");
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    private int rpv_probe() {
        consoleMessage("Not implemented (sorry).");
        return ShellApp.appNoError;
    }

    private int rpv_delete(String[] subargs, boolean fail) {
        GetOpt options = new GetOpt(subargs, "");

        while (true) {
            //FIXME this does not loop
            int option = options.getNextOption();

            if (-1 == option) {
                break;
            }

            switch (option) {
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        String peerStr = options.getNextParameter();

        if (null == peerStr) {
            consoleMessage("Missing peer id");
            return syntaxError();
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unexpected parameter");
            return syntaxError();
        }

        PeerID fail_peer;

        try {
            fail_peer = (PeerID) IDFactory.fromURI(new URI(peerStr));
        } catch (URISyntaxException badURI) {
            consoleMessage("bad peer id");
            return syntaxError();
        } catch (ClassCastException badURI) {
            consoleMessage("bad peer id");
            return syntaxError();
        }

        if (rdv instanceof RendezVousServiceInterface) {
            RendezVousServiceInterface stdRdv = (RendezVousServiceInterface) rdv;

            PeerView rpv = stdRdv.getPeerView();

            if( null == rpv) {
                consoleMessage("No peerview available.");
                return ShellApp.appMiscError;
            }
            
            rpv.notifyFailure(fail_peer, fail);
        } else {
            consoleMessage("Rendezvous is not of correct type");
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    private int rdv_start() {

        rdv.startRendezVous();

        return ShellApp.appNoError;
    }

    private int rdv_stop() {

        rdv.stopRendezVous();

        return ShellApp.appNoError;
    }

    private int edge_connect(String[] subargs) {
        GetOpt options = new GetOpt(subargs, "");

        while (true) {
            int option = options.getNextOption();

            if (-1 == option) {
                break;
            }

            switch (option) {
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        String connectStr = options.getNextParameter();

        if (null == connectStr) {
            consoleMessage("Missing <URI>");
            return syntaxError();
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unexpected parameter");
            return syntaxError();
        }

        EndpointAddress connect_peer;

        try {
            connect_peer = new EndpointAddress(connectStr);
        } catch (Exception badURI) {
            consoleMessage("bad connect URI.");
            return syntaxError();
        }

        try {
            rdv.connectToRendezVous(connect_peer);
        } catch (IOException failed) {
            printStackTrace("Connect Failure", failed);
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    private int edge_disconnect(String[] subargs) {
        GetOpt options = new GetOpt(subargs, "");

        while (true) {
            //FIXME this does not loop
            int option = options.getNextOption();

            if (-1 == option) {
                break;
            }

            switch (option) {
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        String peerStr = options.getNextParameter();

        if (null == peerStr) {
            consoleMessage("Missing <peerid>");
            return syntaxError();
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unexpected parameter");
            return syntaxError();
        }

        PeerID disconnect_peer;

        try {
            disconnect_peer = (PeerID) IDFactory.fromURI(new URI(peerStr));
        } catch (URISyntaxException badURI) {
            consoleMessage("bad peer id");
            return syntaxError();
        } catch (ClassCastException badURI) {
            consoleMessage("bad peer id");
            return syntaxError();
        }

        rdv.disconnectFromRendezVous(disconnect_peer);

        return ShellApp.appNoError;
    }

    private int edge_challenge() {
        consoleMessage("Not implemented (sorry).");
        return ShellApp.appNoError;
    }

    private int auto_start(String[] subargs) {
        GetOpt options = new GetOpt(subargs, "");

        while (true) {
            //FIXME this does not loop
            int option = options.getNextOption();

            if (-1 == option) {
                break;
            }

            switch (option) {
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        String intervalStr = options.getNextParameter();

        if (null != intervalStr) {
            if (null != options.getNextParameter()) {
                consoleMessage("Unexpected parameter");
                return syntaxError();
            }
        }

        return ShellApp.appNoError;
    }

    private int auto_stop(String[] subargs) {
        if (subargs.length > 0) {
            consoleMessage("Unexpected parameter");
            return syntaxError();
        }

        rdv.setAutoStart(false);
        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("usage : rdvcontrol [-v] <command> <subcommand> [[options] <parameters>]");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Controls rendezvous service behaviour";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     rdvcontrol - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("     rdvcontrol [-v] <command> <subcommand> [[options] <parameters>]");
        println(" ");
        println("OPTIONS");
        println("     [-v]    print verbose information");
        println(" ");
        println("COMMANDS -- NOTE : MANY ARE NOT YET IMPLEMENTED. HELP WELCOME");
        println("     rpv");
        println("       addseed <URI>        Adds a seed rendezvous.");
        println("       reseed               Force aggressive search for peeerview entries.");
        println("       probe");
        println("       delete <peerID>      Removes the specified peerview entry.");
        println("       fail <peerID>        Removes and announces failure of specified peerview entry.");
        println(" ");
        println("     rdv");
        println("       start                Starts rendezvous server behaviour for this peer.");
        println("       stop                 Stops rendezvous server behaviour for this peer.");
        println("       disconnect");
        println(" ");
        println("     edge");
        println("       connect <URI>        Connect to the specified Rendezvous.");
        println("       disconnect <peerID>  Disconnect from the specified Rendezvous.");
        println("       challenge <timeout> <peerID> Challenge the specified Rendezvous.");
        println(" ");
        println("     auto");
        println("       start [interval]     Starts auto-rdv behaviour at (optional) specified millisecond interval.");
        println("       stop                 Stops auto-rdv behaviour.");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'rdvcontrol' is used for controlling the behaviour of the ");
        println("rendezvous service.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>rdvcontrol auto stop");
        println(" ");
        println(" ");
        println("SEE ALSO");
        println("    rdvstatus");
    }
}
