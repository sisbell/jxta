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
 * $Id: HistoryQueue.java,v 1.4 2007/02/09 23:12:43 hamada Exp $
 */

package net.jxta.impl.shell.bin.history;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that implements a history queue. It is a singelton class
 * that holds the history queus for all currently running ShellConsole objects.
 * <p/>
 * <br>The different queues are stored in a hashtable, where the key is
 */
public class HistoryQueue {
    /**
     * The maximum number of commands
     */
    protected static final int MAX_HISTORY = 100;

    /**
     * The vector that holds the commands
     */
    private List<String> queue;

    /**
     * The index at for the next commend to retrieve
     */
    private int nextCommand = -1;

    /**
     * Array containing the commands that are not added to the command list
     */
    private final String exclude[];

    /**
     * Create a new history queue
     */
    public HistoryQueue() {
        this(new String[0]);
    }

    /**
     * Create a new history queue
     *
     * @param exclude the commands to not add to the queue (that are the
     *                usually Cursor up and down events )
     */
    public HistoryQueue(String exclude[]) {
        queue = new ArrayList<String>(MAX_HISTORY);
        nextCommand = -1;
        this.exclude = exclude;
    }

    @Override
    public String toString() {
        return "History queue of " + queue.size();
    }

    /**
     * Adds a new command to the queue.
     * <p/>
     * <p/>If the queue has already reached  MAX_HISTORY, the element at
     * index 0 is discarded.
     *
     * @param cmd the command to add
     */
    public synchronized void addCommand(String cmd) {
        cmd = cmd.trim();

        // no empty commands
        if (0 == cmd.length())
            return;

        // none of the excludes
        for (String anExclude : exclude) {
            if (anExclude.equals(cmd))
                return;
        }

        // remove extra commands
        while (queue.size() >= MAX_HISTORY) {
            queue.remove(0);
        }

        String last = null;
        if (queue.size() > 0) {
            last = queue.get(queue.size() - 1);
        }

        // add if not the same as the last command.
        if (!cmd.equals(last)) {
            queue.add(cmd);
        }

        nextCommand = queue.size();
    }

    /**
     * Removes the last command added to the queue
     */
    public synchronized void removeLastCommand() {
        if (queue.size() > 0) {
            queue.remove(queue.size() - 1);
            nextCommand = queue.size();
        }
    }

    /**
     * Returns the command immediately preceeding the previous command
     */
    public String getNextCommand() {
        nextCommand--;
        if (nextCommand < 0) nextCommand = -1;
        return getCommand(nextCommand);
    }

    /**
     * Returns the command immediately succedding the previous command
     * THIS needs help !!
     */
    public String getPreviousCommand() {
        nextCommand++;
        if (nextCommand > queue.size()) nextCommand = queue.size();
        return getCommand(nextCommand);
    }

    /**
     * Gets the command at index, if there is an element at index.
     * Otherwise, an empty string is returned
     *
     * @return the command at index, if there is an element at index
     *         or an empty string, if index is out of range
     */
    public String getCommand(int index) {
        return (index < 0 || index >= queue.size()) ? "" : queue.get(index);
    }

    /**
     * Prints the available commands to a String.
     * If no commands are available, we return and empty string
     */
    public String printHistory() {
        StringBuilder buffer = new StringBuilder();
        int k;
        int i;
        int max = queue.size();
        int length = String.valueOf(MAX_HISTORY).length();
        String number;

        for (i = 0; i < max; i++) {
            number = String.valueOf(i);
            k = 0;
            while ((k++ + number.length()) != length)
                buffer.append(" ");
            buffer.append(number).append(" ");
            buffer.append(queue.get(i));
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
