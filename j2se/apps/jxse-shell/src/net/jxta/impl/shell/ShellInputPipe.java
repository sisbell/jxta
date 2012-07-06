/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 *  reserved.
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
 *  $Id: ShellInputPipe.java,v 1.25 2007/02/09 23:12:40 hamada Exp $
 */
package net.jxta.impl.shell;

import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.protocol.PipeAdvertisement;

import java.io.InterruptedIOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This class implements the default JXTA Shell InputPipe. Strings entered at
 * the keyboard are received by the Shell like if they were messages received
 * from a regular InputPipe.
 */
public class ShellInputPipe implements Runnable, InputPipe {

    /**
     * Logger
     */
    private static final transient Logger LOG = Logger.getLogger(ShellInputPipe.class.getName());

    /**
     * if we have been closed then no more new messages will be queued. Msgs
     * already in the queue can be retrieved.
     */
    private volatile boolean closed = false;

    /**
     * contains the messages.
     */
    private final BlockingQueue<Message> queue;

    /**
     *  The console we will be reading from.
     */
    private ShellConsole cons;

    /**
     *  
     */
    private Thread thread = null;

    /**
     * Constructor for the ShellInputPipe object
     *
     * @param group the group
     * @param shellConsole console
     */
    public ShellInputPipe(PeerGroup group, ShellConsole shellConsole) {
        this.cons = shellConsole;
        queue = new ArrayBlockingQueue<Message>(100);
        thread = new Thread(group.getHomeThreadGroup(), this, "ShellInputPipe-" + shellConsole.getConsoleName());
        thread.setDaemon(true);
        thread.start();
    }


    /**
     * {@inheritDoc}
     */
    public Message waitForMessage() throws InterruptedException {
        return poll(0);
    }

    /**
     * {@inheritDoc}
     */
    public Message poll(int time) throws InterruptedException {
        return queue.poll((0 == time) ? Long.MAX_VALUE : time, TimeUnit.MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() {
        // Close the queue
        closed = true;

        Thread copyThread = thread;
        if (copyThread != null) {
            copyThread.interrupt();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Not implemented.
     */
    public PipeAdvertisement getAdvertisement() {
        throw new UnsupportedOperationException("Not supported by ShellInputPipe");
    }


    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Not implemented.
     */
    public String getName() {
        throw new UnsupportedOperationException("Not supported by ShellInputPipe");
    }


    /**
     * {@inheritDoc}
     */
    public ID getPipeID() {
        throw new UnsupportedOperationException("Not supported by ShellInputPipe");
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        throw new UnsupportedOperationException("Not supported by ShellInputPipe");
    }

    /**
     * {@inheritDoc}
     */
    public void run() {

        try {
            while (!closed) {
                String line;

                try {
                    line = cons.read();
                } catch (InterruptedIOException woken) {
                    Thread.interrupted();
                    continue;
                }
                
                if (line == null) {
                    break;
                }
                
                // Create a message off this string.
                Message msg = new Message();
                MessageElement elem = new StringMessageElement("ShellInputPipe", line, null);

                msg.addMessageElement(elem);

                boolean pushed = false;

                while (!pushed && !closed) {
                    try {
                        queue.put(msg);
                        pushed = true;
                    } catch (InterruptedException woken) {
                        Thread.interrupted();
                    }
                }
            }
        } catch (Throwable all) {
            if (LOG.isLoggable(java.util.logging.Level.SEVERE)) {
                LOG.log(java.util.logging.Level.SEVERE,
                        "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
            }
        } finally {
            thread = null;
        }
    }
}
