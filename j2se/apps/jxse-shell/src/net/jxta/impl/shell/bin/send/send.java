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
 * $Id: send.java,v 1.11 2007/02/09 23:12:45 hamada Exp $
 */


package net.jxta.impl.shell.bin.send;

import net.jxta.endpoint.Message;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.pipe.OutputPipe;

public class send extends ShellApp {
    ShellEnv env;

    public send() {
    }

    @Override
    public void stopApp() {
    }

    public int startApp(String[] args) {
        if ((args == null) || (args.length != 2)) {
            println("error: send <OutputPipe> <PipeMessage>");
            return ShellApp.appParamError;
        }

        env = getEnv();

       /*
        * extract the pipe info
        */
        String pipe = args[0];

        ShellObject obj = env.get(pipe);
        if (obj == null) {
            println("send: cannot access " + pipe);
            return ShellApp.appMiscError;
        }

        OutputPipe op;
        try {
            op = (OutputPipe) obj.getObject();
        } catch (Exception e) {
            println("send: " + pipe + " is not an OutputPipe");
            println(e.toString());
            return ShellApp.appMiscError;
        }

        if (op == null) {
            println("send: cannot access pipe" + pipe);
            return ShellApp.appMiscError;
        }

        /*
        * extract the mesg info
        */

        String m = args[1];

        obj = env.get(m);
        if (obj == null) {
            println("send: cannot access " + m);
            return ShellApp.appMiscError;
        }

        Message msg;
        try {
            msg = (Message) obj.getObject();
        } catch (Exception e) {
            println("send: " + m + " is not a Message");
            println(e.toString());
            return ShellApp.appMiscError;
        }

        if (msg == null) {
            println("send: cannot access msg" + m);
            return ShellApp.appMiscError;
        }

        // ok send the msg

        try {
            op.send(msg);
        } catch (Exception e) {
            println("send: IOException");
            e.printStackTrace();
            return ShellApp.appMiscError;
        }

        return ShellApp.appNoError;
    }

    @Override
    public String getDescription() {
        return "Send a message into a pipe";
    }

    @Override
    public void help() {
        println("NAME");
        println("     send - send a message into a pipe");
        println(" ");
        println("SYNOPSIS");
        println("     send <output pipe> <msg>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'send' sends a message into an output pipe. The output pipe needs");
        println("to have been previously created via a pipe advertisement.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> pipeadv = newpipe -n mypipe");
        println("    JXTA> outpipe = mkpipe -o pipeadv");
        println("    JXTA> send outpipe msg");
        println(" ");
        println("This example creates a pipe advertisement 'pipeadv',");
        println("creates an output pipe 'outpipe' and sends the message 'msg'");
        println("through the pipe.");
        println(" ");
        println("SEE ALSO");
        println("    mkmsg put get recv mkadv newpipe mkpipe");
    }
}
