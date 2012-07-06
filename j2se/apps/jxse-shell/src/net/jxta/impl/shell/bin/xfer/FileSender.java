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
 * $Id: FileSender.java,v 1.22 2007/02/27 18:31:45 bondolo Exp $
 */
package net.jxta.impl.shell.bin.xfer;

import java.io.*;
import java.net.Socket;
import java.lang.reflect.UndeclaredThrowableException;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.*;
import net.jxta.impl.shell.ShellApp;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.protocol.PipeAdvertisement;

/**
 * send a file from one peer to another. destination may be an endpoint address
 * or a pipe.
 */
class FileSender implements MessengerEventListener, OutputPipeListener {

    final PeerGroup group;
    final OutputPipe console;
    final XferDaemon src;
    final File f;
    final int blockSize;
    final boolean async;

    volatile boolean resolved = false;

    private class PendingMessageCounter implements OutgoingMessageEventListener {

        int pending = 0;
        int successes = 0;
        int failures = 0;

        boolean lastSuccess = false;

        OutgoingMessageEvent lastevent = null;

        /**
         * {@inheritDoc}
         */
        public synchronized void messageSendFailed(OutgoingMessageEvent event) {
            lastSuccess = false;
            lastevent = event;
            pending--;
            failures++;
            notifyAll();
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void messageSendSucceeded(OutgoingMessageEvent event) {
            lastSuccess = true;
            lastevent = event;
            pending--;
            successes++;
            notifyAll();
        }

        public synchronized void addPending() {
            pending++;
        }

        public boolean stillPending() {
            return (0 != numPending());
        }

        public synchronized int numPending() {
            return pending;
        }

        public boolean getLastResult() {
            return lastSuccess;
        }

        public OutgoingMessageEvent getLastEvent() {
            return lastevent;
        }
    }

    /**
     * Constructs a file sender object. The sender will be activated by a call
     * back to either {@link #messengerReady(MessengerEvent)} or
     * {@link #outputPipeEvent(OutputPipeEvent)}.
     */
    FileSender(PeerGroup group, OutputPipe console, XferDaemon src, int blockSize, boolean async, File f) {
        this.group = group;
        this.console = console;
        this.src = src;
        this.blockSize = blockSize;
        this.async = async;
        this.f = f;
    }

    /**
     * {@inheritDoc}
     */
    public boolean messengerReady(MessengerEvent event) {
        synchronized (this) {
            resolved = true;
            notifyAll();
        }

        if (null != event.getMessenger()) {
            send(event.getMessenger(), null, null);
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public void outputPipeEvent(OutputPipeEvent event) {
        boolean newResolve = !resolved;

        synchronized (this) {
            group.getPipeService().removeOutputPipeListener(event.getOutputPipe().getPipeID(), this);
            resolved = true;
            notifyAll();
        }


        if (newResolve) {
            send(null, event.getOutputPipe(), null);
        }
    }

    public void socketConnect(Socket socket) {

        send(null, null, socket);
    }

    // Private implementations

    private void print(String line) {
        ShellApp.pipePrint(console, line);
    }

    private void println(String line) {
        print(line + "\n");
    }

    private void consoleMessage(String message) {
        ShellApp.consoleMessage(this.getClass(), console, message);
    }

    private void printStackTrace(String annotation, Throwable failure) {
        ShellApp.printStackTrace(this.getClass(), console, annotation, failure);
    }

    // main stuff

    private void send(Messenger msngr, OutputPipe pipe, Socket socket) {
        InputStream is = null;
        DataInputStream fis = null;
        OutputStream output = null;
        PendingMessageCounter counter = new PendingMessageCounter();

        try {
            long fileSize = f.length();

            // Try and open the file
            try {
                is = new FileInputStream(f);
                fis = new DataInputStream(is);
            } catch (Throwable all) {
                printStackTrace("Failure opening file '" + f + "'.", all);
            }

            int chunks = (int) (fileSize / blockSize);
            chunks += (0 == (fileSize % blockSize)) ? 0 : 1;

            String xferidentifier = MessageElement.getUniqueName();

            if (null != socket) {
                output = socket.getOutputStream();
            }

            consoleMessage("Sending file '" + f.getName() + "' (" + fileSize + ") as " + xferidentifier + " in " + chunks + " chunks of " + blockSize + " bytes.");

            long fileSent = 0;
            int chunk = 0;
            long stime = System.currentTimeMillis();

            while (fileSent < fileSize) {
                byte[] buf = new byte[(int) Math.min(fileSize - fileSent, blockSize)];

                fis.readFully(buf);

                Message msg = new Message();

                // set file info element in first message
                if (0 == fileSent) {
                    // file info
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(os);

                    dos.writeInt(xfer.XFERFILEINFO_VERSION);
                    dos.writeUTF(group.getPeerID().toString());

                    if (null != src) {
                        dos.writeUTF(src.getUserName());

                        PipeAdvertisement srcpipe = src.getPipeAdvertisment();
                        if (null != srcpipe) {
                            DiscoveryService disco = group.getDiscoveryService();

                            long exp = disco.getAdvExpirationTime(srcpipe);
                            if (exp > 0) {
                                dos.writeUTF(srcpipe.getDocument(MimeMediaType.XMLUTF8).toString());
                                dos.writeLong(exp);
                            } else {
                                dos.writeUTF("");
                                dos.writeLong(0);
                            }
                        } else {
                            dos.writeUTF("");
                            dos.writeLong(0);
                        }

                    } else {
                        dos.writeUTF("Anonymous");
                        dos.writeUTF("");
                        dos.writeLong(0);
                    }

                    dos.writeUTF(xferidentifier);
                    dos.writeUTF(f.getName());
                    dos.writeLong(fileSize);
                    dos.writeLong(blockSize);
                    dos.flush();
                    dos.close();
                    os.flush();
                    os.close();

                    msg.addMessageElement(new ByteArrayMessageElement(xfer.XFERFILEINFO_ELEMENT, null, os.toByteArray(), null));
                }

                // identifier element
                msg.addMessageElement(new StringMessageElement(xfer.XFERIDENTIFIER_ELEMENT, xferidentifier, null));

                // sequence element
                msg.addMessageElement(new StringMessageElement(xfer.XFERSEQUENCE_ELEMENT, Integer.toString(chunk++), null));

                // data element
                msg.addMessageElement(new ByteArrayMessageElement(xfer.XFERDATA_ELEMENT, null, buf, null));

                boolean sent = false;
                long backOffSleep = 0;

                while (!sent) {
                    if (null != msngr) {
                        if (async) {
                            counter.addPending();

                            msngr.sendMessage(msg, null, null, counter);

                            synchronized (counter) {
                                while (counter.stillPending()) {
                                    try {
                                        counter.wait();
                                    } catch (InterruptedException woken) {
                                        Thread.interrupted();
                                    }
                                }

                                if (counter.getLastResult()) {
                                    sent = true;
                                } else {
                                    OutgoingMessageEvent lastevent = counter.getLastEvent();

                                    Throwable failure = lastevent.getFailure();
                                    if (null == failure)
                                        sent = false;
                                    else {
                                        if (failure instanceof Error) {
                                            throw (Error) lastevent.getFailure();
                                        } else if (failure instanceof RuntimeException) {
                                            throw (RuntimeException) lastevent.getFailure();
                                        } else if (failure instanceof IOException) {
                                            throw (IOException) lastevent.getFailure();
                                        } else {
                                            throw new UndeclaredThrowableException(failure);
                                        }
                                    }
                                }
                            }
                        } else {
                            msngr.sendMessageB(msg, null, null);
                            sent = true;
                        }
                    } else if (null != pipe) {
                        sent = pipe.send(msg);
                    } else if (null != socket) {
                        WireFormatMessage serialed = WireFormatMessageFactory.toWire(msg, WireFormatMessageFactory.DEFAULT_WIRE_MIME, null);

                        serialed.sendToStream(output);

                        sent = true;
                    } else {
                        throw new IllegalStateException("No send method available!");
                    }

                    if (sent) {
                        backOffSleep = 0;
                        break;
                    }

                    println(" ");
                    consoleMessage("Sleeping for " + (1 << backOffSleep) * 2 + " msecs.");

                    try {
                        Thread.sleep((1 << backOffSleep) * 2);
                    } catch (InterruptedException woken) {
                        Thread.interrupted();
                    }
                    backOffSleep++;
                }

                fileSent += buf.length;
                print("-");
            }

            // K bytes per second calculation
            long elapsedTime = System.currentTimeMillis() - stime;

            // protect agains div by 0
            if (elapsedTime == 0) {
                elapsedTime = 1;
            }

            long kbpsec = fileSize / elapsedTime;

            float secs = (float) (((float) elapsedTime) / 1000.0);

            println(" ");
            consoleMessage("Sent '" + f.getName() + "' (" + fileSize + ") in " + secs + " secs[" + kbpsec + "KBytes/sec]");
        } catch (IOException failed) {
            printStackTrace("Failure during sending of '" + f.getName() + "'.", failed);
        } finally {
            if (null != pipe) {
                pipe.close();
            }

            if (null != output) {
                try {
                    output.close();
                    output = null;
                } catch (IOException ignored) {
                }
            }

            if (null != msngr) {
                msngr.close();
            }

            if (null != fis) {
                try {
                    fis.close();
                    fis = null;
                } catch (IOException ignored) {
                }
            }

            if (null != is) {
                try {
                    is.close();
                    is = null;
                } catch (IOException ignored) {
                }
            }
        }
    }
}


