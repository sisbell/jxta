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
 * $Id: set.java,v 1.6 2007/02/09 23:12:48 hamada Exp $
 */

package net.jxta.impl.shell.bin.set;

import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

/**
 * Set an environment variable
 */
public class set extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        if ((args == null) || (args.length != 2)) {
            consoleMessage(" error : set <env to> <env from>");
            return ShellApp.appParamError;
        }

        ShellEnv env = getEnv();

        ShellObject<?> obj = env.get(args[1]);
        if (obj == null) {
            consoleMessage("Cannot access " + args[1]);
            return ShellApp.appMiscError;
        }

        env.add(args[0], obj);

        return ShellApp.appNoError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Set an environment variable";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("    set - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("    set <env to> <env from>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("    Assign a value to a Shell environment variable.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>set var1 doc");
        println(" ");
        println("    This example assigns the value of enviroment variable ");
        println("    'doc' to the environment variable 'var1'.");
        println(" ");
        println("SEE ALSO");
        println("    Shell env unset");
    }
}
