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
 * $Id: sftp.java,v 1.11 2007/02/09 23:12:43 hamada Exp $
 */
package net.jxta.impl.shell.bin.sftp;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.id.IDFactory;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

import java.io.*;
import java.util.Enumeration;

/**
 * fsend Shell command: send a file to a user.
 */
public class sftp extends ShellApp implements Runnable {

    private DiscoveryService discovery = null;

    private ShellEnv env = null;
    private PipeAdvertisement userAdv = null;
    private static final int WaitingTime = 2000;
    private static final int MAXRETRIES = 5;
    public static final String SftpNameTag = "JxtaSftpUserName";
    public static final String SftpIDTag = "JxtaSftpPipeID";
    private static final String EnvName = "sftpd";
    // local user
    private String userName = null;

    private static final String SenderName = "JxtaSftpSenderName";
    private static final String JftpData = "JxtaSftpData";
    private static final String FileInfo = "JxtaSftpInfo";
    private Thread thread = null;

    // Some file stuff
    private String fsep = "";
    private static final String fdirname = "sftp";
    private String fdir = null;

    public sftp() {
    }

    private int syntaxError() {

        println("Usage: sftp -register userName");
        println("       sftp -login userName ");
        println("       sftp -logout userName ");
        println("       sftp -s userName destUserName filename");
        println("       sftp -search");

        return ShellApp.appParamError;
    }

    public int startApp(String[] args) {

        fdir = fdirname + File.separator;
        File f = new File(fdir);
        if (!f.exists()) {
            try {
                if (!f.mkdir()) {
                    // try "home" directory as root
                    fdir = "";
                }
            } catch (Exception ex) {
                fdir = null;
                printStackTrace("Failed creating directory " + fdir, ex);
            }
        }

        if ((args == null) || (args.length == 0)) {
            return syntaxError();
        }

        env = getEnv();
        discovery = getGroup().getDiscoveryService();

        if (args[0].equals("-register")) {
            return registerNewUser(args);
        }

        if (args[0].equals("-login")) {
            return login(args);
        }

        if (args[0].equals("-logout")) {
            return logout(args);
        }
        if (args[0].equals("-search")) {
            return findUsers();
        }

        return sendFile(args);
    }


    private boolean daemonRunning(String name) {

        ShellObject obj = env.get(EnvName + "." + name + "@" +
                (getGroup().getPeerGroupAdvertisement()).getName());
        return obj != null;
    }

    private int login(String[] args) {

        if (args.length != 2) {
            return syntaxError();
        }

        String name = args[1];
        if (daemonRunning(name)) {
            consoleMessage("user " + name + " is already listening");
            return ShellApp.appMiscError;
        }

        PipeAdvertisement adv = findUserAdv(name);
        if (adv == null) {
            consoleMessage( name + " is not a registered user");
            return ShellApp.appMiscError;
        } else
            discovery.remotePublish(adv);

        runDaemon(name, adv);

        return ShellApp.appNoError;
    }


    private int findUsers() {

        Enumeration each;
        int i = 0;


        discovery.getRemoteAdvertisements(null, DiscoveryService.ADV,
                PipeAdvertisement.NameTag,
                SftpNameTag + ".*",
                20, null);

        while (true) {
            try {
                if (i > MAXRETRIES) {
                    println("");
                    break;
                }
                Thread.sleep(WaitingTime);
                print(".");
                i++;
            } catch (Exception e) {
                printStackTrace("findUsers failed ", e);
            }
        }

        try {

            each = discovery.getLocalAdvertisements(DiscoveryService.ADV,
                    PipeAdvertisement.NameTag,
                    SftpNameTag + ".*");
            if (each.hasMoreElements()) {
                PipeAdvertisement adv;
                consoleMessage("found the following registrations:");
                while (each.hasMoreElements()) {
                    try {
                        adv = (PipeAdvertisement) each.nextElement();
                        println(adv.getName());
                    } catch (Exception e) {
                        printStackTrace("findUsers failed ", e );
                    }
                }
            }
        } catch (Exception e) {
            printStackTrace("findUsers failed ", e );
        }

        return ShellApp.appNoError;
    }

    private int logout(String[] args) {

        if (args.length != 2) {
            return syntaxError();
        }

        String name = args[1];
        if (!daemonRunning(name)) {
            consoleMessage("user " + name + " is not listening");
            return ShellApp.appMiscError;
        }
        stopDaemon(name);
        return ShellApp.appNoError;
    }

    // sftp -s <source pipe> <dest pipe> <filename>
    // is required
    private static final int OBUFLEN = 15000;

    private int sendFile(String[] args) {

        if (!args[0].equals("-s") || args.length != 4) {
            return syntaxError();
        }

        String srcName = args[1];
        String name = args[2];
        String fileName = args[3];

        // check if the srcName is registered
        if (!daemonRunning(srcName)) {
            consoleMessage("user " + srcName + " is not logged in");

            return ShellApp.appMiscError;
        }

        // Locate target name
        PipeAdvertisement adv = findUserAdv(name);
        if (adv == null) {
            consoleMessage(name + " is not a registered user");
            return ShellApp.appMiscError;
        }

        // Try and connect to target
        OutputPipe pipeOut;
        try {
            consoleMessage("found user's advertisement attempting to connect");
            pipeOut = getGroup().getPipeService().createOutputPipe(adv, 120000);
            println("Please be patient ...");
            if (pipeOut == null) {
                consoleMessage("user " + name + " is not listening. Try again later");
                return ShellApp.appMiscError;
            }
        } catch (Exception e) {
            printStackTrace( "user " + name + " is not listening. Try again later", e );
            return ShellApp.appMiscError;
        }

        // Try and open the file
        File f;
        DataInputStream fs;
        try {
            f = new File(fileName);
            fs = new DataInputStream(new FileInputStream(f));
        } catch (Exception e) {
            printStackTrace("Could not open " + fileName, e);
            pipeOut.close();
            return ShellApp.appMiscError;
        }

        // Get the user text
        long fileSize = f.length();

        // fileinfo message
        //  Strip any path info
        int pix = fileName.lastIndexOf(fsep);
        String stripped;
        if (pix != -1)
            stripped = fileName.substring(pix + 1);
        else
            stripped = fileName;

        // Make file info element for first message only
        String start = stripped + "\n" + fileSize;

        Message msg;
        byte[] buf = new byte[OBUFLEN];
        int n16kbufs = (int) (fileSize / OBUFLEN);
        int lastBufSize = (int) (fileSize % OBUFLEN);
        boolean infoDone = false;
        long stime = System.currentTimeMillis();

        int chunks = n16kbufs + (lastBufSize == 0 ? 0 : 1);

        consoleMessage("connected to user " + name);
        println("Sending file " + fileName + ", size = " + fileSize + " bytes" +
                " in " + chunks + " chunks");

        for (int i = 0; i < n16kbufs; i++) {
            try {
                // Build a message
                msg = new Message();

                // set file info element in first message
                if (i == 0) {
                    infoDone = true;
                    // file info
                    msg.addMessageElement(new ByteArrayMessageElement(FileInfo, null, start.getBytes(), null));
                }
                // read the data
                fs.readFully(buf, 0, OBUFLEN);

                // data elements
                msg.addMessageElement(new ByteArrayMessageElement(JftpData, null, buf, null));

                // src name
                msg.addMessageElement(new ByteArrayMessageElement(SenderName, null, srcName.getBytes(), null));

                pipeOut.send(msg);
                print("!");

            } catch (Exception e) {
                printStackTrace("Failed to send file " + fileName + " to user :" + name, e);

                try {
                    fs.close();
                } catch (Exception ex) {
                    printStackTrace("Close failed for " + fileName, ex);
                }
                pipeOut.close();
                return ShellApp.appMiscError;
            }
        }

        // See if we have one more buffer to send
        if (lastBufSize != 0) {
            try {
                // Build a message
                msg = new Message();

                // set file info element in first message only
                if (!infoDone) {
                    // file info
                    msg.addMessageElement(new ByteArrayMessageElement(FileInfo, null, start.getBytes(), null));
                }
                // read the data
                fs.readFully(buf, 0, lastBufSize);

                // data element
                msg.addMessageElement(new ByteArrayMessageElement(JftpData, null, buf, 0, lastBufSize, null));

                // src name element
                msg.addMessageElement(new ByteArrayMessageElement(SenderName, null, srcName.getBytes(), null));

                pipeOut.send(msg);
                print("!");

            } catch (Exception e) {
                printStackTrace("Failed to send file " + fileName + " to user :" + name, e);
                try {
                    fs.close();
                } catch (Exception ex) {
                    printStackTrace("Close failed for " + fileName, ex);
                }

                pipeOut.close();
                return ShellApp.appMiscError;
            }
        }

        // K bytes per second calcul
        long elapsedTime = System.currentTimeMillis() - stime;

        // protect agains div by 0
        if (elapsedTime == 0)
            elapsedTime = 1;

        long kbpsec = fileSize / elapsedTime;

        float secs = (float) (((float) elapsedTime) / 1000.0);

        println("\nSent: " + fileSize + " bytes in " + secs + " secs[" +
                kbpsec + "Kbytes/sec]");

        try {
            fs.close();
        } catch (Exception ex) {
            printStackTrace("Close failed for " + fileName, ex);
        }

        pipeOut.close();
        return ShellApp.appNoError;
    }


    private int registerNewUser(String[] args) {

        boolean secure = true;
        if ((args.length == 3) && ("-insecure".equals(args[2])))
            secure = false;
        else if ((args.length != 2)) {
            return syntaxError();
        }

        String name = args[1];

        // Always secure
        String type;
        if (secure)
            type = PipeService.UnicastSecureType;
        else
            type = PipeService.UnicastType;

        // Check if there is already a registered user of the
        // same name.
        PipeAdvertisement adv = findUserAdv(name);
        if (adv != null) {
            consoleMessage("Sorry, user " + name + " is already registered");
            return ShellApp.appMiscError;
        }

        try {
            // Create a pipe advertisement for this pipe.
            adv = (PipeAdvertisement)
                    AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        } catch (Exception all) {
            printStackTrace("Advertisement document could not be created", all);
            return ShellApp.appMiscError;
        }

        ShellObject obj = env.get("stdgroup");
        // extract the advertisement
        PeerGroup group = (PeerGroup) obj.getObject();

        adv.setPipeID(IDFactory.newPipeID(group.getPeerGroupID()));
        adv.setName(SftpNameTag + "." + name);
        adv.setType(type);

        try {
            // Save the document into the public folder
            discovery.publish(adv);
            discovery.remotePublish(adv);
        } catch (Exception e2) {
            printStackTrace("Advertisement could not be saved", e2);
            return ShellApp.appMiscError;
        }
        println("User : " + name + " is now registered");
        return ShellApp.appNoError;
    }

    // Make sure "name" is the advertised name
    private boolean checkUserAdv(PipeAdvertisement adv, String name) {

        if (adv == null)
            return false;
        if (adv.getName() == null)
            return false;

        String str = adv.getName();
        return str.startsWith(SftpNameTag) && str.endsWith(name);
    }


    private PipeAdvertisement findUserAdv(String name) {

        Enumeration each;

        // First look in the local storage
        try {
            each = discovery.getLocalAdvertisements(DiscoveryService.ADV,
                    PipeAdvertisement.NameTag,
                    SftpNameTag + ".*");
            if ((each != null) && (each.hasMoreElements())) {
                PipeAdvertisement adv;
                while (each.hasMoreElements()) {
                    try {
                        adv = (PipeAdvertisement) each.nextElement();
                        if (adv.getName().endsWith(name)) {
                            if (checkUserAdv(adv, name)) {
                                return adv;
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }

                    if (checkUserAdv(adv, name)) {
                        return adv;
                    }
                }

            }
        } catch (Exception e) {
            printStackTrace("Exception locating advertisement", e);
        }

        // Now, search remote
        discovery.getRemoteAdvertisements(null, DiscoveryService.ADV,
                PipeAdvertisement.NameTag,
                SftpNameTag + "." + name,
                2, null);

        // Wait a bit in order to get an answer.
        int i = 0;
        while (true) {
            try {
                if (i > MAXRETRIES) {
                    println("");
                    break;
                }
                Thread.sleep(WaitingTime);
                print(".");
                i++;
            } catch (Exception e) {
                printStackTrace("Exception locating advertisement", e);
            }

            // Look in the local storage again
            try {
                each = discovery.getLocalAdvertisements(DiscoveryService.ADV,
                        PipeAdvertisement.NameTag,
                        SftpNameTag + "." + name);

                if ((each != null) && (each.hasMoreElements())) {
                    PipeAdvertisement adv;

                    while (each.hasMoreElements()) {

                        try {
                            adv = (PipeAdvertisement) each.nextElement();
                        } catch (Exception e) {
                            continue;
                        }

                        if (checkUserAdv(adv, name)) {
                            return adv;
                        }
                    }
                }
            } catch (Exception e) {
                printStackTrace("Exception locating advertisement", e);
            }
        }
        // adv not found
        return null;
    }

    private void runDaemon(String name, PipeAdvertisement adv) {

        userAdv = adv;
        userName = name;
        thread = new Thread(this, "Jftpd:Jftpd Deamon");
        thread.start();
        // Store this object
        env.add(EnvName + "." + name + "@" +
                (getGroup().getPeerGroupAdvertisement()).getName(),
                new ShellObject<ShellApp>("Sftpd Deamon for " + name + "@" +
                        (getGroup().getPeerGroupAdvertisement()).getName(),
                        this));
    }

    private void stopDaemon(String name) {

        String dname = EnvName + "." + name + "@" +
                (getGroup().getPeerGroupAdvertisement()).getName();

        ShellObject obj = env.get(dname);

        if (obj == null) {
            consoleMessage("Daemon(" + dname + ") for " + name + " not found");
            return;
        }
        sftp d;
        try {
            d = (sftp) obj.getObject();
            d.thread.interrupt();
            env.remove(dname);
        } catch (Exception e) {
            printStackTrace("cannot stop daemon for " + name, e);
        }
    }


    public void run() {

        try {
            InputPipe pipeIn;
            try {
                pipeIn = getGroup().getPipeService().createInputPipe(userAdv);

            } catch (Exception e) {
                return;
            }

            if (pipeIn == null) {
                consoleMessage("cannot open InputPipe");
                return;
            }

            // Listen on the pipe
            Message msg;

            // Message counter
            int i = 0;

            // Receive a file
            File f;
            FileOutputStream fout = null;
            String fileName = null;
            int fsize = 0;
            int offset = 0;
            long stime = 0;
            while (true) {

                try {
                    msg = pipeIn.waitForMessage();
                    if (msg == null) {
                        if (Thread.interrupted()) {
                            // We have been asked to stop
                            consoleMessage("stop listening for user " + userName);
                            pipeIn.close();
                            return;
                        }
                        println("NULL msg");
                        continue;
                    }
                } catch (Exception e) {
                    // This exception probaly happened because the
                    // deamon has been interrupted. In any case, that
                    // shows that sftpd cannot receive messages anyway.
                    // clean up.
                    pipeIn.close();

                    return;
                }
                // Get the first tag
                String senderName;

                // Get the file information
                //   i == 0 on start or restart
                if (i++ == 0) {
                    fout = null;  // used as a flag
                    String fileInfo = (msg.getMessageElement(FileInfo)).toString();
                    if (fileInfo == null) {
                        // Something is broken
                        pipeIn.close();
                        consoleMessage("bad message received. Stopping daemon for user :" +
                                userName);
                        env.remove(EnvName + "." + userName + "@" +
                                (getGroup().getPeerGroupAdvertisement()).getName());
                        return;
                    }

                    // open the file for output.
                    //   fileInfo == "filename\nfilesize"
                    int j = fileInfo.indexOf("\n");
                    fileName = fileInfo.substring(0, j);
                    String ofile = fdir + fileName;
                    try {
                        f = new File(ofile);
                        if (f.exists()) {
                            f.delete();
                        }
                        if (f.createNewFile()) {
                            fout = new FileOutputStream(f);
                        }
                    } catch (Exception e) {
                        printStackTrace("Could not create output file " + ofile, e);
                        consoleMessage("Will receive but not write the data");
                        fout = null;
                    }

                    // file size
                    String filesize = fileInfo.substring(j + 1);
                    fsize = new Integer(filesize);

                    // start at BOF
                    offset = 0;
                    stime = System.currentTimeMillis();
                }

                // Get sender information
                senderName = (msg.getMessageElement(SenderName)).toString();
                if (i == 1) {
                    println("\nReceive " + fileName + "[" + fsize + " bytes] from " +
                            senderName);
                }

                // write data to file
                byte[] data = (msg.getMessageElement(JftpData)).getBytes(false);
                if (data == null) {
                    println("sftpd: user " + senderName + " sent empty data");
                    continue;
                }

                if (fout != null) {
                    fout.write(data, 0, data.length);
                }

                // See if the file transfer is done
                offset += data.length;
                if (offset == fsize) {
                    println("! Done");
                    i = 0;
                    if (fout != null)
                        fout.close();

                    // elapsed time and bytes per sec.
                    long elapsedTime = System.currentTimeMillis() - stime;

                    // div by 0 check
                    if (elapsedTime == 0)
                        elapsedTime = 1;

                    long kbpsec = ((long) fsize) / elapsedTime;
                    float secs = (float) (((float) elapsedTime) / 1000.0);

                    println("Received: " + fsize + " bytes in " + secs + " secs[" +
                            kbpsec + "Kbytes/sec]");
                } else {

                    print("!");

                }
            }
        } catch (Throwable all) {
            printStackTrace("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
        }
    }

    @Override
    public String getDescription() {
        return "Send a file to another peer";
    }

    @Override
    public void help() {
        println("NAME");
        println("    sftp - send a file to another peer ");
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("    sftp -register <userName>");
        println("    sftp -login <userName>");
        println("    sftp -logout <userName>");
        println("    sftp -s <user> <userName> <fileName>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("The 'sftp command implements a secure file transfer");
        println("where one peer can send a file to a second.");
        println("to use 'sftp'. The user needs to register himself. This is done");
        println("via the following steps:");
        println("Step 1: Register via 'sftp -register <username>' command. This command");
        println("        creates a secure sftp advertisement for that user. This has to");
        println("        be done only once, the first time the user registers with");
        println("        sftp. The system remembers it across reboot.");
        println(" ");
        println("Step 2: Login  via 'sftp -login <username>' command. This command");
        println("        login the user and start a listener daemon. This has to");
        println("        to be done everytime the peer is restarted.");
        println(" ");
        println("Step 3: User can securely send a file to another user via the command");
        println("        'sftp -s <myusername> <destusername> <filename>'. This will send the");
        println("        file <filename> to the dest. The file is written on the sftp");
        println("        subdirectory of the directory where the shell is started.");
        println(" ");
        println("         JXTA>sftp -s moi mike photo.gif");
        println("         sftp is connected to user mike");
        println("         Sending file photo.gif, size = 55692 bytes");
        println("  ");
        println("To stop receiving any more files the user can stop the sftp");
        println("listener daemon by entering the command 'sftp -logout <username>'");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("    -register register a new user name  ");
        println("    -login    log user and set default user");
        println("    -logout   logout");
        println("    -s        specify current user, and file names");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("      JXTA>sftp -register me");
        println("      JXTA>sftp -login me");
        println("      JXTA>sftp -s me you /tmp/nihow.jpg");
        println(" ");
        println("This example shows how a new user 'me'  can register and log into sftp,");
        println("and send a file to the user 'you'. User 'you' needs to be similarly");
        println("registered and logged on. The above file is written as sftp/nihow.jpg");
        println(" ");
        println("SEE ALSO");
    }

}





