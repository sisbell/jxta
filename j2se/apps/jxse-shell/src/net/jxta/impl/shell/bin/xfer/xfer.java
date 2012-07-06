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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
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
 * $Id: xfer.java,v 1.28 2007/02/15 20:53:26 bondolo Exp $
 */
package net.jxta.impl.shell.bin.xfer;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.EndpointAddress;
import net.jxta.id.IDFactory;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.socket.JxtaSocket;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import net.jxta.id.ID;
import net.jxta.peer.PeerID;

/**
 * send a file from one peer to another. destination may be an endpoint address
 * or a pipe.
 */
public class xfer extends ShellApp {
    
    static final String XFERFILEINFO_ELEMENT = "JxtaXfer:Fileinfo";
    static final String XFERIDENTIFIER_ELEMENT = "JxtaXfer:Identifier";
    static final int XFERFILEINFO_VERSION = 2;
    static final String XFERSEQUENCE_ELEMENT = "JxtaXfer:Sequence";
    static final String XFERDATA_ELEMENT = "JxtaXfer:Data";
    
    private ShellEnv env = null;
    
    private static final long MAX_SEARCH_TIME = 60 * 1000; // 1 Minutes
    private static final int WAITINGTIME = 2 * 1000; // 2 seconds
    private static final int MAXRETRIES = 5;
    public static final String XFERUSERNAME = "JxtaXferUserName";
    public static final String SftpIDTag = "JxtaSftpPipeID";
    private static final String ENVNAME = "jxtaxfer";
    private static final String XFERSERVICENAME = "jxtaxfer";
    
    PeerGroup group;
    DiscoveryService discovery;
    
    private int syntaxError() {
        consoleMessage(getCmdShortName() +
                "\n\t( (\"register\" [-s|-p] userName) |" +
                "\n\t( (\"login\" [-e | -s] userName) |" +
                "\n\t( (\"logout\" userName) |" +
                "\n\t( (\"send\" [-b blockSize] [-d destAddr] [[-p destPID]] [-u userName] [-a] [-s] destUserName filename) |" +
                "\n\t( (\"search\") )");
        
        return ShellApp.appParamError;
    }
    
    @Override
    public String getDescription() {
        return "Send a file to another peer";
    }
    
    @Override
    public void help() {
        println("NAME");
        println("    xfer - send a file to another peer ");
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("xfer  ( (\"register\" [-s|-p] userName) |");
        println("      ( (\"login\" [-e | -s] userName) |");
        println("      ( (\"logout\" userName) |");
        println("      ( (\"send\" [-b blockSize] [-d destAddr] [[-p destPID]] [-u userName] [-a] [-s] destUserName filename) |");
        println("      ( (\"search\") )");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("    The 'xfer' command implements a file transfer between peers. The file transfer ");
        println("    can be completed using either pipes or via direct endpoint communication. ");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("    register    Register a new user name  ");
        println("          [-s]             Use a secure pipe.");
        println("          [-p]             Use a propagate pipe.");
        println("          username         The name of the new user.");
        println(" ");
        println("    login       Login user");
        println("          [-e]             Create an Endpoint Listener rather than an Input Pipe Listener.");
        println("          [-s]             Create a Server Socket rather than an Input Pipe Listener.");
        println("          username         The name of the user.");
        println(" ");
        println("    logout      Logout user");
        println("          username         The name of the user.");
        println(" ");
        println("    send        Send a file ");
        println("          [-b blockSize]   Chunk size in bytes to break file into.");
        println("          [-d destAddr]    Destination endpoint.");
        println("          [[-p peerID]]    Destination peer id for pipe. Can be repeated.");
        println("          [-u userName]    Source user name. Receiver will see this user as the sender.");
        println("          [-a]             Asynchronously send file. Used with Endpoint Listener mode.");
        println("          [-s]             Send file via socket.");
        println("          destUserName     The name of the intended recipient.");
        println("          filename         The path of a local file to be sent.");
        println(" ");
        println("    search      Search for users.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("      JXTA>xfer register me");
        println("      JXTA>xfer login me");
        println("      JXTA>xfer send -u me you /tmp/nihow.jpg");
        println(" ");
        println("This example shows how a new user 'me' can register and log into xfer,");
        println("and send a file to the user 'you'. User 'you' needs to be similarly");
        println("registered and logged on. The above file is written as /xfer/nihow.jpg");
        println(" ");
        println("SEE ALSO");
        println(" ");
        println("   talk, sftp");
        println(" ");
    }
    
    public int startApp(String[] argv) {
        env = getEnv();
        ShellObject obj = env.get("stdgroup");
        
        group = (PeerGroup) obj.getObject();
        discovery = group.getDiscoveryService();
        
        // start processing args
        
        if (argv.length < 1) {
            return syntaxError();
        }
        
        String command = argv[0].toLowerCase();
        
        if ("register".equals(command)) {
            boolean secure = false;
            boolean propagate = false;
            String userName;
            
            if (argv.length < 2) {
                consoleMessage("Missing <userName>");
                return syntaxError();
            }
            
            GetOpt options = new GetOpt(argv, 1, "ps");
            
            while (true) {
                int option = options.getNextOption();
                
                if (-1 == option) {
                    break;
                }
                
                switch (option) {
                    case'p':
                        propagate = true;
                        break;
                        
                    case's':
                        secure = true;
                        break;
                        
                    default:
                        consoleMessage("Unrecognized option");
                        return syntaxError();
                }
                
                if (propagate && secure) {
                    consoleMessage("Secure or propagate but not both allowed");
                    return syntaxError();
                }
            }
            
            userName = options.getNextParameter();
            
            if (null == userName) {
                consoleMessage("Missing <userName>");
                return syntaxError();
            }
            
            String pipeType = PipeService.UnicastType;
            
            if (secure) {
                pipeType = PipeService.UnicastSecureType;
            }
            
            if (propagate) {
                pipeType = PipeService.PropagateType;
            }
            
            return registerNewUser(userName, pipeType);
        } else if ("login".equals(command)) {
            boolean endpoint = false;
            boolean socket = false;
            String userName;

            if (argv.length < 2) {
                consoleMessage("Missing userName");
                return syntaxError();
            }
            
            GetOpt options = new GetOpt(argv, 1, "es");
            
            while (true) {
                int option = options.getNextOption();
                
                if (-1 == option) {
                    break;
                }
                
                switch (option) {
                    case'e':
                        endpoint = true;
                        break;
                        
                    case's':
                        socket = true;
                        break;
                        
                    default:
                        consoleMessage("Unrecognized option");
                        return syntaxError();
                }
            }
            
            userName = options.getNextParameter();
            
            if (null == userName) {
                consoleMessage("Missing userName");
                return syntaxError();
            }
            
            if ( socket && endpoint ) {
                consoleMessage("Only one of 'socket' or 'endpoint' may be enabled.");
                return syntaxError();
            }
            
            return login(userName, endpoint, socket );
        } else if ("logout".equals(command)) {
            String userName;
            
            if (argv.length < 2) {
                consoleMessage("Missing userName");
                return syntaxError();
            }
            
            GetOpt options = new GetOpt(argv, 1, "");
            
            while (true) {
                //FIXME this does not loop
                int option = options.getNextOption();
                
                if (-1 == option) {
                    break;
                }
                
                switch (option) {
                    default:
                        consoleMessage("Unrecognized option");
                        return syntaxError();
                }
            }
            
            userName = options.getNextParameter();
            
            if (null == userName) {
                consoleMessage("Missing userName");
                return syntaxError();
            }
            
            return logout(userName);
        } else if ("send".equals(command)) {
            int blockSize = 15360;
            EndpointAddress destAddr = null;
            Set<PeerID> destPIDs = new HashSet<PeerID>();
            String srcUserName = null;
            String destUserName;
            String filename;
            boolean async = false;
            boolean socket = false;
            
            if (argv.length < 3) {
                consoleMessage("Missing destination or filename");
                return syntaxError();
            }
            
            GetOpt options = new GetOpt(argv, 1, "ab:d:p:su:");
            
            while (true) {
                int option = options.getNextOption();
                
                if (-1 == option) {
                    break;
                }
                
                switch (option) {
                    case'a':
                        async = true;
                        break;
                        
                    case'b':
                        try {
                            blockSize = Integer.parseInt(options.getOptionArg());
                        } catch (NumberFormatException badnum) {
                            consoleMessage("bad value for block size");
                            return syntaxError();
                        }
                        
                        if (blockSize < 1) {
                            consoleMessage("bad value for block size");
                            return syntaxError();
                        }
                        break;
                        
                    case'd':
                        try {
                            destAddr = new EndpointAddress(options.getOptionArg());
                        } catch (Throwable bad) {
                            consoleMessage("bad endpoint address");
                            return syntaxError();
                        }
                        break;
                        
                    case'p':
                        try {
                            URI peerID = new URI(options.getOptionArg());
                            destPIDs.add((PeerID) IDFactory.fromURI(peerID));
                        } catch (ClassCastException badID) {
                            consoleMessage("ID is not a peer ID");
                            return syntaxError();
                        } catch (URISyntaxException badID) {
                            consoleMessage("bad peer ID");
                            return syntaxError();
                        }
                        break;
                        
                    case's':
                        socket = true;
                        break;
                        
                    case'u':
                        srcUserName = options.getOptionArg();
                        break;
                        
                    default:
                        consoleMessage("Unrecognized option");
                        return syntaxError();
                }
            }
            
            destUserName = options.getNextParameter();
            
            if (null == destUserName) {
                consoleMessage("Missing userName");
                return syntaxError();
            }
            
            filename = options.getNextParameter();
            
            if (null == filename) {
                consoleMessage("Missing filename");
                return syntaxError();
            }
            
            if (null != destAddr) {
                destAddr = new EndpointAddress(destAddr, XFERSERVICENAME, destUserName);
            }
            
            if ((null != destAddr) && !destPIDs.isEmpty()) {
                consoleMessage("Cannot specify both a destination endpoint address and destination peers");
                return syntaxError();
            }
            
            return sendFile(filename, srcUserName, destUserName, destAddr, blockSize, destPIDs, async, socket);
        } else if ("search".equals(command)) {
            return findUserAdvs();
        }
        
        consoleMessage("Unrecognized Command");
        return syntaxError();
    }
    
    /**
     * Register
     */
    private int registerNewUser(String userName, String type) {
        
        // Check if there is already a registered user of the sme name.
        print("# " + getCmdShortName() + " - Searching for existing advertisement for user '" + userName + "'");
        
        PipeAdvertisement adv = findUserAdv(userName);
        if (adv != null) {
            consoleMessage("Sorry, user '" + userName + "' is already registered");
            return ShellApp.appMiscError;
        }
        
        try {
            // Create a pipe advertisement for this pipe.
            adv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        } catch (Exception all) {
            printStackTrace("Advertisement document could not be created", all);
            return ShellApp.appMiscError;
        }
        
        adv.setPipeID(IDFactory.newPipeID(group.getPeerGroupID()));
        adv.setName(XFERUSERNAME + "." + userName);
        adv.setType(type);
        
        try {
            // Save the document into the public folder
            discovery.publish(adv, DiscoveryService.DEFAULT_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
            discovery.remotePublish(adv, DiscoveryService.DEFAULT_EXPIRATION);
        } catch (Exception e2) {
            printStackTrace("Advertisement could not be published.", e2);
            return ShellApp.appMiscError;
        }
        
        consoleMessage("Created advertisement for user '" + userName + "'.");
        
        return ShellApp.appNoError;
    }
    
    /**
     * Stop the receiver daemon for the specified user name.
     *
     * @param userName user to log in.
     * @param endpoint if false, login as an input pipe listener or if true, login as an endpoint listener
     * @return result code
     */
    private int login(String userName, boolean endpoint, boolean socket ) {
        PipeAdvertisement adv = null;
        
        if (isDaemonRunning(userName)) {
            consoleMessage("User '" + userName + "' is already listening");
            return ShellApp.appMiscError;
        }
        
        if (!endpoint) {
            print("# " + getCmdShortName() + " - Searching for Advertisement for user '" + userName + "'");
            adv = findUserAdv(userName);
            if (adv == null) {
                consoleMessage("User '" + userName + "' is not a registered user");
                return ShellApp.appMiscError;
            }
        }
        
        return runDaemon(userName, adv, socket) ? ShellApp.appNoError : ShellApp.appMiscError;
    }
    
    /**
     * Stop the receiver daemon for the specified user name.
     *
     * @param userName user to log out.
     * @return result code
     */
    private int logout(String userName) {
        
        if (!isDaemonRunning(userName)) {
            consoleMessage("User '" + userName + "' is not listening");
            return ShellApp.appMiscError;
        }
        
        consoleMessage("Stoping listener for '" + userName + "'");
        
        return stopDaemon(userName) ? ShellApp.appNoError : ShellApp.appMiscError;
    }
    
    /**
     * Send a file to a remote peer.
     *
     * @param filename     the full local path of the file to send.
     * @param srcUserName  optionally our user name otherwise null.
     * @param destUserName the name of the destination user.
     * @param destAddr     destination peer for direct endpoint transfers.
     * @param blockSize    block size in bytes to segment file into.
     * @param async        use async messengers to send file segments.
     * @return result code
     */
    private int sendFile(String filename, String srcUserName, String destUserName, EndpointAddress destAddr, int blockSize, Set<? extends ID> destPIDs, boolean async, boolean socket ) {
        
        XferDaemon srcDaemon = null;
        
        // get the src daemon if any
        if (null != srcUserName) {
            
            srcDaemon = getDaemon(srcUserName);
            
            if (null == srcDaemon) {
                consoleMessage("User '" + srcUserName + "' is not logged in.");
                
                return ShellApp.appMiscError;
            }
        }
        
        File f = new File(filename);
        
        if (!f.exists()) {
            consoleMessage("file '" + filename + "' not found.");
            return ShellApp.appMiscError;
        }
        
        FileSender sender = new FileSender(group, getOutputConsPipe(), srcDaemon, blockSize, async, f);
        
        PipeAdvertisement adv = null;
        
        if (null == destAddr) {
            // Locate target name
            print("# " + getCmdShortName() + " - Searching for advertisement for user '" + destUserName + "'.");
            adv = findUserAdv(destUserName);
            if (adv == null) {
                consoleMessage("Advertisement for user '" + destUserName + "' not found.");
                return ShellApp.appMiscError;
            }
            
            consoleMessage("Found advertisement for user '" + destUserName + "' attempting to connect to " + adv.getType());
            
            // Try and connect to target
            if( !socket ) {
                try {
                    getGroup().getPipeService().createOutputPipe(adv, destPIDs, sender);
                } catch (Throwable all) {
                    printStackTrace("failure reaching user '" + destUserName + "'.", all);
                    return ShellApp.appMiscError;
                }
            }
        } else {
            // asynchronously get a messenger to the destination address.
            consoleMessage("Getting messenger for '" + destAddr + "'");
            boolean working = group.getEndpointService().getMessenger(sender, destAddr, null);
            if (!working) {
                consoleMessage("Could not get messenger for '" + destAddr + "'.");
                return ShellApp.appMiscError;
            }
        }
        
        Socket connectSocket = null;
        
        synchronized (sender) {
            long start = System.currentTimeMillis();
            long finish = start + MAX_SEARCH_TIME;
            
            while ((!sender.resolved) && (System.currentTimeMillis() < finish)) {
                try {
                    long waitFor = Math.max(1, finish - System.currentTimeMillis());
                    waitFor = Math.min(waitFor, 30 * 1000);
                    consoleMessage("Please be patient ...(" + ((finish - System.currentTimeMillis()) / 1000) + " secs)");
                    
                    if( !socket ) {
                        sender.wait(waitFor);
                    } else {
                        try {
                            connectSocket = new JxtaSocket(group, adv, (int) MAX_SEARCH_TIME);
                            sender.resolved = true;
                        } catch (Throwable all) {
                            printStackTrace("failure reaching user '" + destUserName + "'.", all);
                            return ShellApp.appMiscError;
                        }
                    }
                } catch (InterruptedException woken) {
                    Thread.interrupted();
                }
            }
            
            if (!sender.resolved) {
                if (null != adv) {
                    getGroup().getPipeService().removeOutputPipeListener(adv.getPipeID(), sender);
                }
                
                consoleMessage("User '" + destUserName + "' is not listening. Try again later.");
                return ShellApp.appMiscError;
            } else {
                consoleMessage("Located user '" + destUserName + "' after " + (System.currentTimeMillis() - start) + " msec.");
                if( socket ) {
                    sender.socketConnect( connectSocket );
                }
            }
        }
        
        return ShellApp.appNoError;
    }
    
    /**
     * Search for a specific user by name.
     */
    private PipeAdvertisement findUserAdv(String name) {
        
        String attribute = XFERUSERNAME + "." + name;
        Enumeration each;
        
        int currentRetry = 0;
        while (currentRetry <= MAXRETRIES) {
            try {
                print(".");
                
                // Look in the local storage
                each = discovery.getLocalAdvertisements(DiscoveryService.ADV, PipeAdvertisement.NameTag, attribute);
                
                while (each.hasMoreElements()) {
                    PipeAdvertisement adv;
                    
                    try {
                        adv = (PipeAdvertisement) each.nextElement();
                    } catch (ClassCastException skip) {
                        continue;
                    }
                    
                    println(" ");
                    return adv;
                }
                
                // Now, search remote
                discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, PipeAdvertisement.NameTag, attribute, 2, null);
                
                // nap for a while to let responses come in.
                try {
                    Thread.sleep(WAITINGTIME);
                } catch (InterruptedException woken) {
                    Thread.interrupted();
                }
                
                currentRetry++;
            } catch (Exception ex) {
                println(" ");
                printStackTrace("failure locating advertisement", ex);
            }
        }
        
        println(" ");
        
        // adv not found
        return null;
    }
    
    /**
     * Search for any users.
     */
    private int findUserAdvs() {
        
        String attribute = XFERUSERNAME + "." + "*";
        
        int currentRetry = 0;
        while (currentRetry <= MAXRETRIES) {
            try {
                print(".");
                
                // Now, search remote
                discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, PipeAdvertisement.NameTag, attribute, 20, null);
                
                // nap for a while to let responses come in.
                try {
                    Thread.sleep(WAITINGTIME);
                } catch (InterruptedException woken) {
                    Thread.interrupted();
                }
                
                currentRetry++;
            } catch (Throwable ex) {
                println(" ");
                printStackTrace("Failure locating advertisement", ex);
            }
        }
        
        println(" ");
        
        // Look in the local storage
        Enumeration each = null;
        
        try {
            each = discovery.getLocalAdvertisements(DiscoveryService.ADV, PipeAdvertisement.NameTag, attribute);
        } catch (IOException failed) {
            printStackTrace("Failure locating advertisement", failed);
        }
        
        while (each.hasMoreElements()) {
            PipeAdvertisement adv;
            
            try {
                adv = (PipeAdvertisement) each.nextElement();
            } catch (ClassCastException skip) {
                continue;
            }
            
            println(adv.getName().substring(XFERUSERNAME.length() + 1));
        }
        
        return ShellApp.appNoError;
    }
    
    /**
     * Create a new daemon for the specified user name.
     *
     * @param userName user to log in.
     * @param adv      optional pipe advertisement. If null then endpoint listener will be created.
     * @return if true then daemon was registered successfully otherwise false.
     */
    private boolean runDaemon(String userName, PipeAdvertisement adv, boolean socket) {
        
        try {
            XferDaemon daemon = new XferDaemon(getOutputConsPipe(), group, userName, adv, socket);
            daemon.setDaemon(true);
            daemon.start();        
            
            // Store this object
            String dname = ENVNAME + "." + userName + "@" + (group.getPeerGroupAdvertisement()).getName();
            
            ShellObject<?> obj = new ShellObject<XferDaemon>("Xfer Deamon for " + userName + "@" + (group.getPeerGroupAdvertisement()).getName(), daemon);
            env.add(dname, obj);
            consoleMessage("Login of user '" + userName + "' successful.");
            return true;
        } catch (Throwable ex) {
            printStackTrace("Failed to create daemon for user '" + userName + "'.", ex);
        }
        
        return false;
    }
    
    /**
     * Check if a daemon is running for the specified user name.
     *
     * @param userName user to check.
     * @return if true then daemon was found otherwise false.
     */
    private boolean isDaemonRunning(String userName) {
        
        return (null != getDaemon(userName));
    }
    
    
    /**
     * Gets the daemon for the specified user name
     *
     * @param userName The user name to get the XferDaemon object for
     * @return the XferDaemon for the specified user name if available, otherwise null.
     */
    private XferDaemon getDaemon(String userName) {
        
        String dname = ENVNAME + "." + userName + "@" + (group.getPeerGroupAdvertisement()).getName();
        
        ShellObject obj = env.get(dname);
        
        if (null == obj) {
            return null;
        }
        
        if (!XferDaemon.class.isInstance(obj.getObject())) {
            throw new RuntimeException(dname + " is not an XferDaemon object");
        }
        
        return (XferDaemon) obj.getObject();
    }
    
    /**
     * Stops the daemon for the specified user name
     *
     * @param userName The user name to close the XferDaemon
     */
    private boolean stopDaemon(String userName) {
        
        String dname = ENVNAME + "." + userName + "@" + (group.getPeerGroupAdvertisement()).getName();
        
        XferDaemon d = getDaemon(userName);
        
        if (null == d) {
            return false;
        }
        
        try {
            d.close();
            env.remove(dname);
            return true;
        } catch (Throwable ex) {
            printStackTrace("Cannot stop daemon for '" + userName + "'.", ex);
            return false;
        }
    }
}


