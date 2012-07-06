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
 * <http://www.jxta.org/.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * $Id: more.java,v 1.19 2007/02/09 23:12:50 hamada Exp $
 */
package net.jxta.impl.shell.bin.more;

import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

import java.io.IOException;
import java.util.Collections;
import java.util.StringTokenizer;

/**
 * Pages through output.
 */
public class more extends ShellApp {

    private final static int MAX_CONSOLE = 20;

    private int count = 0;

    /**
     * {@inheritDoc}
     */
    public int startApp(final String[] args) {
        ShellEnv env = getEnv();

        try {
            GetOpt options = new GetOpt(args, 0, "p");

            while (true) {
                //FIXME this does not loop
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
                    default:
                        consoleMessage("Unrecognized option");
                        return syntaxError();
                }
            }

            String name = options.getNextParameter();

            if ((null != name) && (null != options.getNextParameter())) {
                consoleMessage("Unsupported parameter");
                return syntaxError();
            }

            if (null != name) {
                ShellObject obj = env.get(name);
                if (obj == null) {
                    consoleMessage("Cannot access '" + name + "'");
                    return ShellApp.appMiscError;
                }

                ShellEnv catEnv = new ShellEnv(env);


                PeerGroup current = (PeerGroup) getEnv().get("stdgroup").getObject();

                PipeService pipes = current.getPipeService();

                /*
                *  create pipe to link the two
                */
                PipeAdvertisement padv;

                try {
                    padv = (PipeAdvertisement)
                            AdvertisementFactory.newAdvertisement(
                                    PipeAdvertisement.getAdvertisementType());
                    padv.setPipeID(IDFactory.newPipeID(current.getPeerGroupID()));
                    padv.setType(PipeService.UnicastType);

                    InputPipe inpipe = pipes.createInputPipe(padv);
                    setInputPipe(inpipe);
                    env.add("stdin", new ShellObject<InputPipe>("Default InputPipe", inpipe));

                    OutputPipe outpipe = pipes.createOutputPipe(padv, Collections.singleton(getGroup().getPeerID()), 1000);
                    catEnv.add("stdout", new ShellObject<OutputPipe>("Default OutputPipe", outpipe));
                } catch (IOException ex) {
                    printStackTrace("Could not construct pipes for piped command.", ex);
                }

                final ShellApp chainCat = loadApp(null, "cat", catEnv);

                Thread catThread = new Thread() {

                    @Override
                    public void run() {
                        exec(chainCat, args);
                    }
                };

                setJoinedThread(catThread);
                catThread.start();
            }

            moreStdin();

            return ShellApp.appNoError;
        } catch (Exception ex) {
            printStackTrace("Error processing commands", ex);
            return ShellApp.appMiscError;
        }
    }

    private void moreStdin() {

        do {
            String entry = null;
            try {
                entry = waitForInput();
            } catch (IOException failed) {
                printStackTrace("whoops", failed);
            }

            if (entry == null)
                break;
            pagein(entry);
        } while (true);
    }

    private void pagein(String data) {

        StringTokenizer tokens = new StringTokenizer(data, "\n");

        while (tokens.hasMoreElements()) {
            println(tokens.nextToken());
            count++;
            if (count >= MAX_CONSOLE) {
                // reset the counter since we exited
                count = 0;
                print("-----More-----");
                String entry = null;
                try {
                    entry = consWaitForInput();
                } catch (IOException failed) {
                    //ignored
                }
                if ((entry == null) || entry.equals("q"))
                    return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Page through a Shell object or from standard input.";
    }

    private int syntaxError() {
        consoleMessage("more [-p] [<env>]");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     more  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     more [-p] [<env>]");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("     [-p]    Pretty print the object.");
        println(" ");
        println("PARAMETERS");
        println(" ");
        println("     <env>   The environment variable to print.");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'more' is a Shell command that pages through the content ");
        println("of an object stored in environment variable. The name of the");
        println("is provided as an argument. If no argument is supplied, the");
        println("command takes its input from the 'stdin' shell pipe.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> cat myfile | more");
        println(" ");
        println("This command pipes the content of the variable 'myfile' into the");
        println("'more' command and displays it to the Shell console.");
        println(" ");
        println("SEE ALSO");
        println(" ");
        println("    cat env");
        println(" ");
    }
}
