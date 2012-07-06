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
 *  $Id: ShellConsole.java,v 1.57 2007/02/09 23:12:40 hamada Exp $
 */
package net.jxta.impl.shell;

import java.awt.HeadlessException;
import java.io.InterruptedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.peergroup.PeerGroup;

/**
 * A container for JXTA Shell Application
 */
public abstract class ShellConsole {

    /**
     * Logger
     */
    private final static Logger LOG = Logger.getLogger(ShellConsole.class.getName());

    /**
     *  The name of the system property that will force the shell to come up in
     *  terminal mode.
     */
    private final static String NOWINDOWPROPNAME = "SHELLNOWINDOW";

    /**
     * The name of this console.
     */
    private String consoleName = null;

    /**
     * The root application for this console. Exiting this application will
     * close the console.
     */
    protected ShellApp rootApp;

    /**
     * Creates a new terminal window with rows row and cols columns to display.
     *
     * @param useName the name of the console
     */
    public ShellConsole(ShellApp rootApp, String useName) {
        this.rootApp = rootApp;
        consoleName = useName;
    }

    /**
     * Gets the consoleName attribute of the ShellConsole object
     *
     * @return The consoleName value
     */
    public String getConsoleName() {
        return consoleName;
    }

    /**
     * Clear the shell window.
     */
    public abstract void clear();

    /**
     * Terminates the current console.
     */
    public void destroy() {
        rootApp = null;
    }

    /**
     * Read from the console.
     *
     * @return a line read from the console.
     * @throws InterruptedIOException when the reading thread is interrupted
     * before a line has been read.
     */
    public abstract String read() throws InterruptedIOException;

    /**
     * Write to the console.
     *
     * @param msg The message to write.
     */
    public abstract void write(String msg);

    /**
     * Sets the prompt to be used when reading input.
     */
    public abstract void setPrompt(String prompt);

    /**
     * Replaces the current command line with the command indicated.
     *
     * @param cmd the command replacing the typed command
     */
    public abstract void setCommandLine(String cmd);

    /**
     * Specify which group should be monitored for status.
     */
    public abstract void setStatusGroup(PeerGroup group);

    /**
     * Returns the application associated with this console.
     *
     * @return the application associated with this console or <tt>null</tt> if
     * the console has been destroyed.
     */
    public ShellApp getShellApp() {
        return rootApp;
    }

    /**
     * Returns the command this console sends if the cursor down key was
     * pressed.
     *
     * @return command this console send if the cursor down key was pressed
     */
    public abstract String getCursorDownName();


    /**
     * Returns the command this console sends if the cursor up key was pressed.
     *
     * @return command this console send if the cursor up key was pressed
     */
    public abstract String getCursorUpName();

    /**
     * Creates a new ShellConsole instance.
     *
     * @param useName the name of the console to be created.
     */
    public static ShellConsole newConsole(ShellApp rootApp, String useName) {
        boolean tryWindow = !Boolean.getBoolean(NOWINDOWPROPNAME);

        try {
            return newConsole(rootApp, useName, tryWindow);
        } catch (HeadlessException noWindow) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Could not open console window. Trying text console", noWindow);
            }
            return newConsole(rootApp, useName, false);
        }
    }

    /**
     * Creates a new ShellConsole instance.
     *
     * @param useName  the name of the console to be created.
     * @param windowed if true then the console created will be a window.
     */
    public static ShellConsole newConsole(ShellApp rootApp, String useName, boolean windowed) {
        if (windowed) {
            return new SwingShellConsole(rootApp, useName, 30, 70);
        } else {
            return new ConsoleShellConsole(rootApp, useName);
        }
    }
}
