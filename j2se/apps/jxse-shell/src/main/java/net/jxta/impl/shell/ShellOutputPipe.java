/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 * $Id: ShellOutputPipe.java,v 1.27 2007/02/09 23:12:40 hamada Exp $
 */

package net.jxta.impl.shell;

import java.io.IOException;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.OutputPipe;
import net.jxta.protocol.PipeAdvertisement;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import net.jxta.logging.Logging;

/**
 * This class implements the default  JXTA Shell OutputPipe.
 * Message received on the pipe are displayed onto the console.
 */

public class ShellOutputPipe implements Runnable, OutputPipe {

    /**
     * Logger
     */
    private static final transient java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ShellOutputPipe.class.getName());

    private volatile boolean closed;
    
    private final ShellConsole cons;
    
    private final BlockingQueue<Message> queue;

    private Thread thread = null;

    public ShellOutputPipe(PeerGroup group, ShellConsole cons) {
        this.cons = cons;
        queue = new ArrayBlockingQueue<Message>(100);

        thread = new Thread(group.getHomeThreadGroup(), this, "ShellOutputPipe-" + cons.getConsoleName());
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * {@inheritDoc}
     */
    public boolean send(Message msg) throws IOException {
        boolean pushed = false;
        
        while(!pushed & !closed) {
            try {
                queue.put(msg);
                pushed = true;
            } catch(InterruptedException woken) {
                Thread.interrupted();
            }
        }
        
        if(!pushed && closed) {
            IOException failed = new IOException("Could not enqueue " + msg + " for sending. Pipe is closed.");

            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, failed.getMessage(), failed);
            }
            throw failed;
        }
       
        return pushed;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Not implemented.
     */
    public PipeAdvertisement getAdvertisement() {
        throw new UnsupportedOperationException("Not supported by ShellOutputPipe");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Not implemented.
     */
    public String getName() {
        throw new UnsupportedOperationException("Not supported by ShellOutputPipe");
    }

    /**
     * {@inheritDoc}
     */
    public ID getPipeID() {
        throw new UnsupportedOperationException("Not supported by ShellOutputPipe");
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        throw new UnsupportedOperationException("Not supported by ShellOutputPipe");
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() {
        closed = true;
        
        Thread temp = thread;
        if (null != temp) {
            temp.interrupt();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {

        try {
            while (!closed) {
                Message msg;

                try {
                    msg = queue.take();
                } catch (InterruptedException woken) {
                    Thread.interrupted();
                    continue;
                }
                
                // Get all the chunks of data in the message
                Iterator<MessageElement> els = msg.getMessageElementsOfNamespace(null);

                while (els.hasNext()) {
                    MessageElement el = els.next();

                    cons.write(el.toString());
                }
            }
        } catch (Throwable all) {
            if (LOG.isLoggable(java.util.logging.Level.SEVERE)) {
                LOG.log(java.util.logging.Level.SEVERE,
                        "Uncaught Throwable in Thread : " + Thread.currentThread().getName(), all);
            }
        } finally {
            thread = null;
        }
    }
}
