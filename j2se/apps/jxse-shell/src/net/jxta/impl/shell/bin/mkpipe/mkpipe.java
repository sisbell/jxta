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
 * $Id: mkpipe.java,v 1.17 2007/02/09 23:12:44 hamada Exp $
 */
package net.jxta.impl.shell.bin.mkpipe;

import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.protocol.PipeAdvertisement;

import java.io.IOException;

/**
 * create an input or output pipe from a pipe advertisement
 */

public class mkpipe extends ShellApp {
    ShellEnv env;

    public mkpipe() {
    }

    @Override
    public void stopApp() {
    }

    private int syntaxError() {
        println("usage: mkpipe -i|o pipeAdv");
        return ShellApp.appParamError;
    }

    public int startApp(String[] args) {

        // check for the proper number of arguements and make sure they are valid
        if ((args == null) || (args.length != 2)) {
            return syntaxError();
        } else if ((!args[0].equals("-i")) && (!args[0].equals("-o"))) {
            return syntaxError();
        }

        env = getEnv();

        ShellObject obj = env.get(args[1]);
        if (obj == null) {
            println("cat: cannot access " + args[1]);
            return ShellApp.appMiscError;
        }

        PipeAdvertisement adv;
        try {
            adv = (PipeAdvertisement) obj.getObject();
        } catch (Exception e) {
            // This is not an Advertisement
            println("mkpipe: " + args[1] + " is not an PipeAdvertisement");
            return ShellApp.appMiscError;
        }

        if (adv == null) {
            println("mkpipe: " + args[1] + " is empty");
            return ShellApp.appMiscError;
        }

        // InputPipe or OutputPipe ?
        if (args[0].equals("-i")) {
            InputPipe ip;
            try {
                ip = getGroup().getPipeService().createInputPipe(adv);
            } catch (IOException e) {
                println("mkpipe: cannot create an InputPipe onto " + args[1]);
                return ShellApp.appMiscError;
            }

            env.add(getReturnVariable(), new ShellObject<InputPipe>("InputPipe of " + args[1], ip));
        } else {
            OutputPipe op;
            try {
                op = getGroup().getPipeService().createOutputPipe(adv, -1);
            } catch (IOException e) {
                println("mkpipe: cannot create an OutputPipe onto " + args[1]);
                return ShellApp.appMiscError;
            }
            
            env.add(getReturnVariable(), new ShellObject<OutputPipe>("OutputPipe of " + args[1], op));
        }
        return ShellApp.appNoError;
    }

    @Override
    public String getDescription() {
        return "Create a pipe";
    }

    @Override
    public void help() {
        println("NAME");
        println("     mkpipe - create a pipe");
        println(" ");
        println("SYNOPSIS");
        println("     mkpipe -i|o <pipe advertisement>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'mkpipe' creates an input pipe or an output pipe from a given");
        println("pipe advertisement document. In order for pipes to communicate");
        println("an input and output pipe needs to be created with the same");
        println("pipe advertisement. PipeServiceService advertisements are structured documents that");
        println("contains at least the unique pipe Id. The pipe Id uniquely");
        println("identifies a pipe in the JXTA world. Pipes are not localized");
        println("or binded to a physical peer. PipeServiceService connections are established");
        println("by searching for pipe advertisements and resolving dynamically");
        println("the location of an input pipe object binded to that advertisement.");
        println("An input pipe can be binded to the same pipe advertisement on");
        println("multiple peers transparently to the output pipe. The output pipe");
        println("does not need to known on which physical peer the input pipe is");
        println("located. To communicate with the pipe, the output pipe needs");
        println("to search for the input pipe that binds that advertisement.");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("  -i create an input pipe");
        println("  -o create an output pipe");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> pipeadv = newpipe -n mypipe");
        println("    JXTA> inpipe = mkpipe -i pipeadv");
        println("    JXTA> msg = recv inpipe");
        println("    JXTA> data = get msg mytag");
        println(" ");
        println("This example creates a pipe advertisement 'pipeadv'");
        println("create an input pipe 'inpipe' and receive a message 'msg'");
        println("The body of the message associated with the tag 'mytag' is");
        println("retreived from the message via the 'get' command.");
        println(" ");
        println("SEE ALSO");
        println("    mkmsg put get send recv mkadv");
    }

}

