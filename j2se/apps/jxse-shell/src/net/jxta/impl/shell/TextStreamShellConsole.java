/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 *  $Id: TextStreamShellConsole.java,v 1.7 2007/02/09 23:12:40 hamada Exp $
 */
package net.jxta.impl.shell;

import net.jxta.peergroup.PeerGroup;

import java.io.*;
import java.util.logging.Level;

/**
 * A console for the JXTA Shell which reads and writes from char streams.
 */
public class TextStreamShellConsole extends ShellConsole {

    /**
     * Log4J Logger
     */
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(TextStreamShellConsole.class.getName());

    /**
     * We read from here
     */
    private BufferedReader stdin;

    /**
     * We read from here
     */
    private BufferedWriter stdout;

    /**
     * Command prompt
     */
    private String prompt;

    /**
     * Creates a new console
     *
     *
     * @param rootApp root applcation
     * @param useName the name of the console
     * @param in  the input
     * @param out the output
     */
    public TextStreamShellConsole(ShellApp rootApp, String useName, Reader in, Writer out) {
        super(rootApp, useName);
        stdin = new BufferedReader(in);
        stdout = new BufferedWriter(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCommandLine(String cmd) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrompt(String newPrompt) {
        prompt = newPrompt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCursorDownName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCursorUpName() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        write("\u000c");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String read() throws InterruptedIOException {

        if (null != prompt) {
            write(prompt);
            prompt = "";
        }

        try {
            return stdin.readLine();
        } catch (IOException ex) {
            if (LOG.isLoggable(java.util.logging.Level.WARNING)) {
                LOG.log(Level.WARNING, "Could not read from stdin", ex);
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(String msg) {
        try {
            stdout.write(msg);
            stdout.flush();
        } catch (IOException ex) {
            if (LOG.isLoggable(java.util.logging.Level.WARNING)) {
                LOG.log(Level.WARNING, "Failed writing to stdout", ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatusGroup(PeerGroup group) {
    }
}
