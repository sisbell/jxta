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
 * $Id: XferDaemon.java,v 1.19 2007/02/27 18:31:42 bondolo Exp $
 */
package net.jxta.impl.shell.bin.xfer;

import net.jxta.discovery.DiscoveryService;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.WireFormatMessageFactory;
import net.jxta.impl.shell.ShellApp;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaServerSocket;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * send a file from one peer to another. destination may be an endpoint address
 * or a pipe.
 */
class XferDaemon extends Thread implements EndpointListener, PipeMsgListener {
    
    private static final String XFERSERVICENAME = "jxtaxfer";
    
    private OutputPipe console;
    
    private volatile boolean closed = false;
    
    private Map<String, FileReceiver> incoming = Collections.synchronizedMap(new HashMap<String, FileReceiver>());
    
    private String userName;
    private PipeAdvertisement adv;
    private PeerGroup group;
    
    private boolean socket;
    private InputPipe pipeIn;
    private JxtaServerSocket serverSocket;
    private boolean endpointListenerRegistered = false;
    
    // Some file stuff
    private static final String INCOMINGDIR = "xfer";
    private File incomingdir = null;
    
    /**
     * Create a new xfer daemon for the specified user name.
     */
    public XferDaemon(OutputPipe console, PeerGroup group, String userName, PipeAdvertisement adv, boolean socket) {
        super("Shell Xfer Daemon : " + userName);
        
        this.console = console;
        this.group = group;
        this.userName = userName;
        this.adv = adv;
        this.socket = socket;
        
        try {
            setupIncomingDir();
        } catch (IOException failed) {
            printStackTrace("Failed creating directory for receiving files.", failed);
            throw new UndeclaredThrowableException(failed);
        }
        
        if (null != adv) {
            if( !socket ) {
                try {
                    pipeIn = group.getPipeService().createInputPipe(adv, this);
                } catch (IOException failure) {
                    printStackTrace("Xfer daemon for '" + userName + "' caught exception opening input pipe.", failure);
                }
                
                if (pipeIn == null) {
                    consoleMessage("Failed to construct Input Pipe for : " + adv.getPipeID());
                    
                    throw new RuntimeException("Could not open Input Pipe to :" + adv.getPipeID());
                }
                
                consoleMessage("Logged in user '" + userName + "' as " + pipeIn.getType() + " Input Pipe Listener.");
            } else {
                try {
                    serverSocket = new JxtaServerSocket(group, adv);
                } catch (IOException failure) {
                    printStackTrace("Xfer daemon for '" + userName + "' caught exception opening input pipe.", failure);
                }
                
                if (serverSocket == null) {
                    consoleMessage("Failed to construct Server Socket for : " + adv.getPipeID());
                    
                    throw new RuntimeException("Could not open Server Socket to :" + adv.getPipeID());
                }
                
                consoleMessage("Logged in user '" + userName + "' as " + adv.getType() + " Server Socket.");
            }
            
            // republish the advertisement as our own.
            try {
                DiscoveryService disco = group.getDiscoveryService();
                
                disco.publish(adv, DiscoveryService.DEFAULT_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            } catch (IOException failure) {
                printStackTrace("Xfer daemon for '" + userName + "' caught exception publishing pipe advertisement.", failure);
            }
        } else {
            try {
                EndpointService endpoint = group.getEndpointService();
                endpointListenerRegistered = endpoint.addIncomingMessageListener(this, XFERSERVICENAME, userName);
            } catch (Throwable failure) {
                printStackTrace("Xfer daemon for '" + userName + "' caught exception registering Endpoint Listener.", failure);
            }
            
            if (!endpointListenerRegistered) {
                throw new RuntimeException("Could not login endpoint listener for: " + XFERSERVICENAME + "/" + userName);
            }
            
            consoleMessage("Logged in user '" + userName + "' as Endpoint Listener.");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        
        try {
            while (!closed) {
                try {
                    if( !socket ) {
                        synchronized (this) {
                            wait();
                        }
                    } else {
                        try {
                            Socket newSocket = serverSocket.accept();
                            
                            newSocket.setSoTimeout(0);
                            
                            Thread handler = new Thread( new SocketReceiver(newSocket), "Xfer socket receiver thread" );
                            handler.setDaemon( true );
                            handler.start();
                            consoleMessage( "Xfer daemon started new receiver thread " + handler );
                        } catch( SocketTimeoutException ignored ) {
                            // ignored
                        }
                    }
                } catch (InterruptedException woken) {
                    Thread.interrupted();
                }
            }
            
            consoleMessage("Xfer daemon for '" + userName + "' closing.");
        } catch (Throwable all) {
            printStackTrace("Xfer daemon for '" + userName + "' caught exception!", all);
        } finally {
            closed = true;
        }
    }
    
    /**
     * Close this daemon.
     */
    synchronized void close() {
        
        if (null != pipeIn) {
            pipeIn.close();
            pipeIn = null;
        }
        
        if( null != serverSocket ) {
            try {
                serverSocket.close();
            } catch( IOException ignored ) {
                // ignored
            }
            serverSocket = null;
        }
        
        if (endpointListenerRegistered) {
            EndpointService endpoint = group.getEndpointService();
            endpoint.removeIncomingMessageListener(XFERSERVICENAME, userName);
            endpointListenerRegistered = false;
        }
        
        closed = true;
        notifyAll();
    }
    
    
    /**
     * Return the pipe advertisement associated with this daemon if any.
     *
     * @return pipe advertisement for this daemon. will be null if daemon is
     *         registered as an endpoint listener rather than a pipe listener
     */
    PipeAdvertisement getPipeAdvertisment() {
        return adv;
    }
    
    /**
     * Return the pipe advertisement associated with this daemon if any.
     *
     * @return User name associated with this daemon.
     */
    String getUserName() {
        return userName;
    }
    
    /**
     * Create the received file directory.
     */
    private void setupIncomingDir() throws IOException {
        String userhome = null;
        
        try {
            userhome = System.getProperty("user.home");
        } catch (Exception ignored) {
            //ignored
        }
        
        incomingdir = new File(userhome + File.separator + INCOMINGDIR);
        
        if (!incomingdir.exists()) {
            if (!incomingdir.mkdirs()) {
                throw new IOException("Could not create : " + incomingdir);
            }
        }
    }
    
    // Private implementations
    
    private void print(String line) {
        ShellApp.pipePrint(console, line);
    }

    private void consoleMessage(String message) {
        ShellApp.consoleMessage(this.getClass(), console, message);
    }
    
    private void printStackTrace(String annotation, Throwable failure) {
        ShellApp.printStackTrace(this.getClass(), console, annotation, failure);
    }
    
    /**
     * {@inheritDoc}
     */
    public void pipeMsgEvent(PipeMsgEvent event) {
        Message msg = event.getMessage();
        
        processIncomingMessage(msg, null, null);
    }
    
    /**
     * {@inheritDoc}
     */
    public void processIncomingMessage(Message message, EndpointAddress srcAddr, EndpointAddress dstAddr) {
        
        try {
            // This is a message part of a file transfer.
            MessageElement newXfer = message.getMessageElement(xfer.XFERFILEINFO_ELEMENT);
            
            MessageElement oldXfer = message.getMessageElement(xfer.XFERIDENTIFIER_ELEMENT);
            
            if (null == oldXfer) {
                consoleMessage("No file identifier");
                return;
            }
            
            String identifier = oldXfer.toString();
            FileReceiver thisFile;
            
            if (null != newXfer) {
                thisFile = new FileReceiver(group, console, incomingdir, newXfer);
                
                incoming.put(identifier, thisFile);
            } else {
                thisFile = incoming.get(identifier);
            }
            
            if (null != thisFile) {
                MessageElement sequenceElement = message.getMessageElement(xfer.XFERSEQUENCE_ELEMENT);
                
                int sequence = Integer.parseInt(sequenceElement.toString());
                
                MessageElement dataElement = message.getMessageElement(xfer.XFERDATA_ELEMENT);
                
                boolean done = thisFile.processElement(sequence, dataElement);
                
                if (done) {
                    incoming.remove(identifier);
                }
            } else {
                consoleMessage("No handler for : " + identifier);
            }
        } catch (Throwable all) {
            printStackTrace("Xfer daemon for '" + userName + "' caught exception!", all);
        }
    }
    
    /**
     *  A worker thread to read from a socket. JXTA Socket currently only
     *  supports blocking read operations.
     */
    private class SocketReceiver implements Runnable {
        
        private final Socket socket;
        
        private SocketReceiver( Socket socket ) {
            this.socket = socket;
        }
        
        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                InputStream input = socket.getInputStream();
                
                while( socket.isConnected() ) {
                    try {
                        Message message = WireFormatMessageFactory.fromWire( input, WireFormatMessageFactory.DEFAULT_WIRE_MIME, null );
                        
                        processIncomingMessage( message, null, null );
                    } catch( IOException failed ) {
                        if( !(failed instanceof EOFException) ) {
                            printStackTrace("Xfer daemon for '" + userName + "' caught exception!", failed);
                        }
                        break;
                    }
                }
            } catch (Exception all ) {
                printStackTrace("Xfer daemon for '" + userName + "' caught exception!", all);
            } finally {
                try {
                    socket.close();
                } catch( IOException ignored ) {
                    // ignored
                }
            }
        }
    }
}
