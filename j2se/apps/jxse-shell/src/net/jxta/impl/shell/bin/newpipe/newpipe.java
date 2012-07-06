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
 * $Id: newpipe.java,v 1.5 2007/02/09 23:12:41 hamada Exp $
 */
package net.jxta.impl.shell.bin.newpipe;

import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

/**
 * Create a new pipe advertisment
 */
public class newpipe extends ShellApp {

    public newpipe() {
    }

    /**
     *  {@inheritDoc}
     */
    public int startApp(String[] argv) {
        ShellEnv env = getEnv();

        String name = null;
        boolean secure = false;
        boolean propagate = false;

        PipeAdvertisement adv;

        GetOpt options = new GetOpt(argv, 0, "n:sp");

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
                    propagate = true;
                    break;

                case's':
                    secure = true;
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

        if (propagate && secure) {
            consoleMessage("secure or propagate, but not both allowed");
            return syntaxError();
        }

        try {
            PeerGroup parent = (PeerGroup) env.get("stdgroup").getObject();

            adv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(
                PipeAdvertisement.getAdvertisementType());

            adv.setPipeID(IDFactory.newPipeID(parent.getPeerGroupID()));
            adv.setName(name);
//          adv.setDescription("created by newpipe");

            String pipeType = PipeService.UnicastType;

            if (secure) {
                pipeType = PipeService.UnicastSecureType;
            }

            if (propagate) {
                pipeType = PipeService.PropagateType;
            }

            adv.setType(pipeType);

            ShellObject<PipeAdvertisement> newAdv = new ShellObject<PipeAdvertisement>("Pipe Advertisement", adv);
            env.add(getReturnVariable(), newAdv);

            return ShellApp.appNoError;
        } catch (Throwable all) {
            printStackTrace("Failure creating pipe advertisment", all);
            return ShellApp.appMiscError;
        }
    }

    public int syntaxError() {
        consoleMessage("Usage: newpipe [-p | -s] [-n <name>]");
        return ShellApp.appParamError;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Create a new pipe advertisment";
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     newpipe - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     newpipe [-p | -s] [-n <name>]");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("      [-s]             Create a secure pipe.");
        println("      [-p]             Create a propagate pipe.");
        println("      [-n <name>]      Optional name for the pipe");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("newpipe creates a new pipe advertisement with a random pipe id. ");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("          JXTA>mypipeadv = newpipe -n mypipe");
        println(" ");
        println(" This creates a new pipe advertisement of the default type. The new ");
        println(" pipe is given the name 'mypipe'. Before you can do anything with");
        println(" the pipe you need to instantiate it via the 'mkpipe' command.");
        println(" ");
        println("SEE ALSO");
        println("    mkpipe");
    }
}
