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
 * $Id: FileReceiver.java,v 1.11 2007/02/27 18:31:45 bondolo Exp $
 */
package net.jxta.impl.shell.bin.xfer;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.MessageElement;
import net.jxta.impl.shell.ShellApp;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.OutputPipe;
import net.jxta.protocol.PipeAdvertisement;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.URI;
import net.jxta.peer.PeerID;

/**
 * Manages a file being received.
 */
class FileReceiver {
    RandomAccessFile fout;
    final OutputPipe console;
    
    final PeerID srcPeerID;
    final String srcUserName;
    final String srcPipeAdv;
    final String identifier;
    final String filename;
    final long filesize;
    final long blockSize;
    
    File file;
    final int chunks;
    boolean[] receivedChunks;
    
    long written;
    long starttime;
    
    FileReceiver(PeerGroup group, OutputPipe console, File incomingdir, MessageElement newXfer) throws IOException {
        this.console = console;
        DataInputStream dis = new DataInputStream(newXfer.getStream());
        int fileinfoversion = dis.readInt();
        
        if (fileinfoversion != xfer.XFERFILEINFO_VERSION) {
            throw new IllegalArgumentException("Incorrect file info version");
        }

        srcPeerID = PeerID.create(URI.create(dis.readUTF()));
        srcUserName = dis.readUTF();
        srcPipeAdv = dis.readUTF();
        long srcPipeExp = dis.readLong();
        identifier = dis.readUTF();
        filename = dis.readUTF();
        filesize = dis.readLong();
        blockSize = dis.readLong();
        
        consoleMessage("New Incoming " + identifier + " file '" + filename + "' (" + filesize + ") from '" + srcUserName + "'.");
        
        // Save the pipe Adv of the endpoint
        if (srcPipeAdv.length() > 0) {
            try {
                XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, new StringReader(srcPipeAdv));
                Advertisement srcPipe = AdvertisementFactory.newAdvertisement(asDoc);
                
                if (srcPipe instanceof PipeAdvertisement) {
                    DiscoveryService disco = group.getDiscoveryService();
                    
                    consoleMessage("Publishing sender pipe advertisment for user '" + srcUserName + "' with expiry of " + srcPipeExp + " millis.");
                    disco.publish(srcPipe, srcPipeExp, srcPipeExp);
                }
            } catch (Throwable all) {
                printStackTrace("Failed to publish pipe advertisment for user '" + srcUserName + "'.", all);
            }
        }
        
        written = 0;
        chunks = (int) (filesize / blockSize) + ((0 != (filesize % blockSize)) ? 1 : 0);
        receivedChunks = new boolean[chunks];
        
        file = new File(incomingdir, filename);
        
        if (file.exists()) {
            // We should not overwrite an existing file. Instead, create a new file.
            file = new File(incomingdir, filename + ".copy");
            if (file.exists()) {
                file.delete();
            }
        }
        
        file.createNewFile();
        fout = new RandomAccessFile(file, "rw");
        fout.setLength(filesize);
        
        starttime = System.currentTimeMillis();
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
    
    /**
     * processes a chunk of a file.
     *
     * @param chunk   the index number of the chunk.
     * @param element the element containing the data for the chunk.
     * @return true if the file transfer is complete, otherwise false.
     * @throws java.io.IOException if an io error occurs
     */
    synchronized boolean processElement(int chunk, MessageElement element) throws IOException {
        if (null == fout) {
            return true;
        }
        
        fout.seek(chunk * blockSize);
        
        fout.write(element.getBytes(false));
        
        written += element.getByteLength();
        
        print("+");
        
        receivedChunks[chunk] = true;
        
        if (written >= filesize) {
            fout.getFD().sync();
            fout.close();
            fout = null;
            
            println(" Done");
            
            // elapsed time and bytes per sec.
            long elapsedTime = System.currentTimeMillis() - starttime;
            
            // div by 0 check
            if (elapsedTime == 0) {
                elapsedTime = 1;
            }
            
            long kbpsec = filesize / elapsedTime;
            float secs = (float) (((float) elapsedTime) / 1000.0);
            
            consoleMessage("Received '" + filename + "' (" + filesize + ") in " + secs + " secs[" + kbpsec + "KBytes/sec]");
            
            return true;
        }
        
        return false;
    }
}
