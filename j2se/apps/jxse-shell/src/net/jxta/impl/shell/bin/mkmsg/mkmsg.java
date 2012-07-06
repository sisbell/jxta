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
 * $Id: mkmsg.java,v 1.15 2007/02/09 23:12:43 hamada Exp $
 */
package net.jxta.impl.shell.bin.mkmsg;

import net.jxta.endpoint.Message;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

/**
 * mkmsg: create a Message
 */
public class mkmsg extends ShellApp {
    ShellEnv env;

    public mkmsg() {
    }

    public int startApp(String[] args) {
        if (args.length > 0) {
            println("error: mkmsg");
            shortHelp();
            return ShellApp.appParamError;
        }

        env = getEnv();

        Message msg = new Message();
        env.add(getReturnVariable(), new ShellObject<Message>("Message", msg));
        
        return ShellApp.appNoError;
    }


    public void shortHelp() {
        println("NAME");
        println("     mkmsg - make a pipe message");
        println(" ");
        println("SYNOPSIS");
        println("     mkmsg ");
        println(" ");
    }

    @Override
    public String getDescription() {
        return "Make a pipe message";
    }

    @Override
    public void help() {
        println("NAME");
        println("     mkmsg - make a pipe message");
        println(" ");
        println("SYNOPSIS");
        println("     mkmsg ");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("'mkmsg' creates a new message to send/receive data from a pipe.");
        println("The message object is stored in a Shell environment variable.");
        println("If no name is assigned via the '=' operator, a default");
        println("environment variable is created 'env#' for holding the");
        println("message object (# is a growing integer number).");
        println(" ");
        println("JXTA messages are composed of multiple tag body parts. Each");
        println("tag body is uniquely identified via a unique tag name. The tag");
        println("name is used to insert ('put' command) a new tag body in a message,");
        println("or to retrieve ('get' command) a tag body from a message");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("      JXTA>mkmsg");
        println("             ");
        println("This creates a message object and puts it in the environment");
        println("variable 'env#' where # is an integer number (ex. env4)");
        println("You can assign a specific name to the message variable by assigning");
        println("it a name via the '=' Shell operator. See below for example:");
        println(" ");
        println("      JXTA>mymsg = mkmsg");
        println("      JXAT>put mymsg mytag data");
        println("      JXTA>send outpipe mymsg");
        println(" ");
        println("This create a new msg 'mymsg', stored data in the message body tag 'mytag'.");
        println("The message is then sent via the output pipe 'outpipe'");
        println(" ");
        println("SEE ALSO");
        println("    send recv get put mkpipe mkadv ");
    }
}
