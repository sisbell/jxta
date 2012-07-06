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
 * $Id: recv.java,v 1.11 2007/05/29 16:55:05 bondolo Exp $
 */


package net.jxta.impl.shell.bin.recv;

import net.jxta.pipe.InputPipe;
import net.jxta.endpoint.Message;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

/**
 * recv: receive a message from an InputPipe
 */
public class recv extends ShellApp {
    ShellEnv env;
    
    public recv() {
    }
    
    @Override
    public void stopApp() {
    }
    
    private int syntaxError() {
        consoleMessage("Usage: recv [-t timeout] <InputPipe>");
        return ShellApp.appParamError;
    }
    
    public int startApp(String[] argv) {
        env = getEnv();
        
        int timeout = 0;
        
        GetOpt options = new GetOpt(argv, 0, "t:");
        
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
                case't':
                    timeout = Integer.parseInt(options.getOptionArg());
                    break;
                    
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }
        
        String pipe = options.getNextParameter();
        
        if (null == pipe) {
            consoleMessage("Missing <pipe> parameter.");
            return syntaxError();
        }
        
        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }
        
        ShellObject obj = env.get(pipe);
        if (obj == null) {
            consoleMessage("cannot access " + pipe);
            return ShellApp.appMiscError;
        }
        
        InputPipe ip;
        try {
            ip = (InputPipe) obj.getObject();
        } catch (Exception e) {
            printStackTrace( pipe + " is not an InputPipe", e);
            return ShellApp.appMiscError;
        }
        
        Message msg = null;
        try {
            msg = ip.poll(timeout);
        } catch (InterruptedException woken) {
            Thread.interrupted();
            // In this case we have no loop semantic so we just fail.
        }
        
        if (msg == null) {
            consoleMessage("No message received.");
            return ShellApp.appMiscError;
        } else {
            println("Message received.");
        }
        
        env.add(getReturnVariable(), new ShellObject<Message>("Message from " + pipe, msg));
        return ShellApp.appNoError;
    }
    
    @Override
    public String getDescription() {
        return "Receive a message from a pipe";
    }
    
    @Override
    public void help() {
        println("NAME");
        println("     recv - receive a message from a pipe");
        println(" ");
        println("SYNOPSIS");
        println("     recv [-t timeout] <input pipe>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'recv' receives a message from previously created input pipe.");
        println("A timeout in milliseconds can be given. If no timeout is given " );
        println("the call will block until a message is received. A timeout of ");
        println("zero corresponds to a blocking call.");
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
        println("    mkmsg put get send mkadv newpipe mkpipe");
    }
}
