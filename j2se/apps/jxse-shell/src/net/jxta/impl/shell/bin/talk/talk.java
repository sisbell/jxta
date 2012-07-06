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
 * $Id: talk.java,v 1.67 2007/02/09 23:12:46 hamada Exp $
 */


package net.jxta.impl.shell.bin.talk;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.InputStreamMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PipeAdvertisement;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * talk Shell command: send and receive message from other users.
 */

public class talk extends ShellApp implements Runnable, DiscoveryListener {

    @SuppressWarnings("serial")
    static class ImageWindow extends Frame {

        static ImageWindow currentWin = null;

        Image currentImage = null;
        boolean sizeDone = false;

        public ImageWindow() {
            super();
            enableEvents(WindowEvent.WINDOW_CLOSING |
                    WindowEvent.WINDOW_CLOSED |
                    ComponentEvent.COMPONENT_RESIZED);
        }

        private void initSize(int width, int height) {

            if (sizeDone) {
                return;
            }

            if (width > 640) {
                width = 640;
            } else if (width < 100) {
                width = 100;
            }
            if (height > 480) {
                height = 480;
            } else if (height < 100) {
                height = 100;
            }
            setSize(width, height);
            sizeDone = true;
        }

        @Override
        public boolean imageUpdate(Image img,
                                   int infoflags,
                                   int x,
                                   int y,
                                   int width,
                                   int height) {


            if ((infoflags & (WIDTH | HEIGHT)) == (WIDTH | HEIGHT)) {
                initSize(width, height);
            }

            // Make sure events keep comming until we have the size done.
            return super.imageUpdate(img, infoflags, x, y, width, height) || !sizeDone;
        }

        private void setImageData(byte[] data) {
            sizeDone = false;
            currentImage = getToolkit().createImage(data);
            // Get the image size.
            int width = currentImage.getWidth(this);
            int height = currentImage.getHeight(this);
            if ((width != -1) && (height != -1)) {
                initSize(width, height);
            }
        }

        // For some reason resizing a frame does not repaint it
        // unless the new size is bigger in both dimensions !
        @Override
        protected void processComponentEvent(ComponentEvent e) {
            if (e.getID() == ComponentEvent.COMPONENT_RESIZED) {
                repaint();
            }
            super.processComponentEvent(e);
        }

        @Override
        protected void processWindowEvent(WindowEvent e) {
            if (e.getID() == WindowEvent.WINDOW_CLOSING) {
                dispose();
            } else if (e.getID() == WindowEvent.WINDOW_CLOSED) {
                currentWin = null;
            }
            super.processWindowEvent(e);
        }

        @Override
        public void paint(Graphics g) {
            if (currentImage != null) {
                if (sizeDone) {
                    g.drawImage(currentImage, 0, 0, getSize().width, getSize().height, this);
                }
            }
        }

        static void showImageFromData(String title, byte[] data) {

            if (currentWin == null) {
                currentWin = new ImageWindow();
                currentWin.pack();
               // currentWin.show();
                currentWin.setVisible(true);
            }
            currentWin.setTitle(title);
            currentWin.setImageData(data);
        }
    }

    private boolean doImages = true;


    private String handleImageFromMsg(String sender, Message msg) {

        String textVersion = "";

        try {

            MessageElement imageElem = msg.getMessageElement("talkx", "image");

            if (imageElem != null) {

                textVersion = "<image>";
                String title = "from " + sender;

                MessageElement captionElem =
                        msg.getMessageElement("talkx", "image_caption");

                if (captionElem != null) {
                    String caption = captionElem.toString();
                    title = title + " : " + caption;
                    textVersion = "<image caption=" + caption + ">";
                }

                if (doImages) {
                    byte[] data = imageElem.getBytes(false);
                    ImageWindow.showImageFromData(title, data);
                    textVersion = "";
                }
            }

        } catch (Exception e) {
            printStackTrace("failure getting image from message", e);
            doImages = false;
        }

        return textVersion;
    }

    private static final int WaitingTime = 500; // 1/2 second
    private static final int MAXRETRIES = 20; // 20 times WaitingTime = 10 seconds
    public static final String TalkNameTag = "JxtaTalkUserName";
    public static final String TalkIDTag = "JxtaTalkPipeID";
    private static final String EnvName = "talkd";
    private static final String SenderName = "JxtaTalkSenderName";
    private static final String SenderMessage = "JxtaTalkSenderMessage";
    private static final String SENDERGROUPNAME = "GrpName";

    private DiscoveryService discovery = null;

    private ShellEnv env = null;
    private String userName = null;
    private Thread thread = null;
    private InputPipe pipeIn = null;

    private List<PipeAdvertisement> results;

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {

        if ((args == null) || (args.length == 0)) {
            return syntaxError();
        }

        env = getEnv();
        discovery = getGroup().getDiscoveryService();

        if (args[0].equals("-register") || args[0].equals("-r")) {
            return registerNewUser(args);
        }

        if (args[0].equals("-login") || args[0].equals("-l")) {
            return login(args);
        }

        if (args[0].equals("-logout") || args[0].equals("-lo")) {
            return logout(args);
        }
        if (args[0].equals("-search") || args[0].equals("-f")) {
            return findUsers();
        }

        return sendMessage(args);
    }


    private boolean deamonRunning(String name) {

        ShellObject obj = env.get(EnvName + "." + name + "@" + (getGroup().getPeerGroupAdvertisement()).getName());
        return obj != null;
    }

    private int login(String[] args) {

        if (args.length != 2) {
            return syntaxError();
        }

        String name = args[1];
        if (deamonRunning(name)) {
            consoleMessage("user " + name + " is already listening");
            return ShellApp.appMiscError;
        }

        PipeAdvertisement adv = findUserAdv(name);

        if (adv == null) {
            consoleMessage(name + " is not a registered user");
            return ShellApp.appMiscError;
        }

        runDeamon(name, adv);

        return ShellApp.appNoError;
    }


    private int findUsers() {

        discovery.getRemoteAdvertisements(null,
                DiscoveryService.ADV,
                PipeAdvertisement.NameTag,
                TalkNameTag + ".*",
                200,
                null);

        for (int i = 0; i <= MAXRETRIES; i++) {
            try {
                Thread.sleep(WaitingTime);
                print(".");
            } catch (Exception e) {
                //ignored
            }
        }
        println("");

        try {
            Enumeration<Advertisement> each = discovery.getLocalAdvertisements(DiscoveryService.ADV,
                    PipeAdvertisement.NameTag,
                    TalkNameTag + ".*");

            if (each.hasMoreElements()) {
                consoleMessage("Found the following talk registrations:");
                while (each.hasMoreElements()) {
                    try {
                        PipeAdvertisement adv = (PipeAdvertisement) each.nextElement();
                        String name = adv.getName();

                        if (name.startsWith(TalkNameTag)) {
                            name = name.substring(TalkNameTag.length() + 1);
                        }

                        println(name + "\t\t[" +adv.getType() + "]");
                    } catch (Exception e) {
                        //ignored
                    }
                }

            }
        } catch (Exception e) {
            printStackTrace("Discovery failed", e);
            return ShellApp.appMiscError;
        }
        return ShellApp.appNoError;
    }

    /**
     * {@inheritDoc}
     */
    public void discoveryEvent(DiscoveryEvent event) {
        DiscoveryResponseMsg res = event.getResponse();        

        if (res.getDiscoveryType() == DiscoveryService.ADV) {
            Enumeration<Advertisement> each = res.getAdvertisements();

            synchronized (this) {
                while (each.hasMoreElements()) {
                    try {
                        Advertisement adv = each.nextElement();
                        if (adv instanceof PipeAdvertisement) {
                            if (null == results) {
                                results = new ArrayList<PipeAdvertisement>();
                            }

                            results.add((PipeAdvertisement) adv);
                        }
                    } catch (Exception ex) {
                        //ignored
                    }
                }
                notify();
            }
        }
    }

    private int logout(String[] args) {

        if (args.length != 2) {
            return syntaxError();
        }

        String name = args[1];
        if (!deamonRunning(name)) {
            consoleMessage("User '" + name + "' is not listening.");
            return ShellApp.appMiscError;
        }
        stopDeamon(name);
        return ShellApp.appNoError;
    }

    private int sendMessage(String[] args) {

        String name;
        String srcName;

        if (args[0].equals("-u")) {
            if (args.length != 3) {
                return syntaxError();
            }
            srcName = args[1];
            name = args[2];

            // check if the name is registered
            if (!deamonRunning(srcName)) {
                consoleMessage("User '" + srcName + "' is not logged in.");
                return ShellApp.appMiscError;
            }
        } else {
            name = args[0];
            // There is no name for the person sending message
            // use the peer nameinstead.
            srcName = getGroup().getPeerName();
        }

        PipeAdvertisement adv = findUserAdv(name);
        if (adv == null) {
            consoleMessage("User '" + name + "' is not a registered.");
            return ShellApp.appMiscError;
        }

        OutputPipe pipeOut = null;
        consoleMessage("Found advertisement for '" + name + "'. Attempting to connect");

        for (int i = 0; i < 2; i++) {
            try {
                pipeOut = getGroup().getPipeService().createOutputPipe(adv, 25000);
                if (pipeOut != null) {
                    break;
                }
                print(".");
            } catch (Exception e) {
                //timeout exception
            }
        }

        if (pipeOut == null) {
            consoleMessage("User " + name + " is not listening. Try again later");
            return ShellApp.appMiscError;
        }

        // Get the user text

        consoleMessage("Connection established to user " + name);
        consoleMessage("Type your message. To exit, type \".\" at beginning of line");

        Message msg;
        String userInput;
        while (true) {

            try {
                userInput = waitForInput();
                if (userInput == null) {
                    break;
                }
                if (userInput.equals(".")) {
                    break;
                }
                String imagePath = null;
                String imageCaption = null;
                if (userInput.startsWith(".image")) {
                    int nextColon = userInput.indexOf(":");
                    if (nextColon == -1)
                        continue;
                    userInput = userInput.substring(nextColon + 1);

                    nextColon = userInput.indexOf(":");
                    if (nextColon != -1) {
                        imagePath = userInput.substring(0, nextColon);
                        userInput = userInput.substring(nextColon + 1);
                    } else {
                        imagePath = userInput;
                        userInput = "";
                    }
                    nextColon = userInput.indexOf(":");
                    if (nextColon != -1) {
                        imageCaption = userInput.substring(0, nextColon);
                        userInput = userInput.substring(nextColon + 1);
                    } else {
                        imageCaption = userInput;
                        userInput = "";
                    }
                }

                // Build a message
                msg = new Message();

                msg.addMessageElement(new StringMessageElement(SenderMessage, userInput, null));

                if (srcName != null) {
                    msg.addMessageElement(new StringMessageElement(SenderName, srcName, null));
                }


                try {
                    if (imagePath != null && !imagePath.equals("")) {
                        InputStream imageFile = new FileInputStream(imagePath);

                        msg.replaceMessageElement("talkx",
                                new InputStreamMessageElement("image", MimeMediaType.AOS, imageFile, null));
                        imageFile.close();
                    }

                    if (imageCaption != null && !imageCaption.equals("")) {
                        msg.replaceMessageElement("talkx",
                                new StringMessageElement("image_caption", imageCaption, null));
                    }
                } catch (Exception any) {
                    printStackTrace("Failure doing image processing", any);
                }

                msg.addMessageElement(new StringMessageElement(SENDERGROUPNAME, getGroup().getPeerGroupName(), null));
                pipeOut.send(msg);
            } catch (Exception ex) {
                printStackTrace("Failed to send message to user :" + name, ex);
                return ShellApp.appMiscError;
            }
        }
        pipeOut.close();
        return ShellApp.appNoError;
    }


    private int registerNewUser(String[] args) {

        if ((args.length != 2) && (args.length != 3)) {
            return syntaxError();
        }
        String name = args[1];

        String type;
        if (args.length == 3) {
            // Type has been specified
            String t = args[2];
            if (t.equals("-secure") || t.equals("-s")) {
                type = PipeService.UnicastSecureType;
            } else if (t.equals("-propagate") || t.equals("-p")) {
                type = PipeService.PropagateType;
            } else {
                // Default is unicast
                type = PipeService.UnicastType;
            }
        } else {
            type = PipeService.UnicastType;
        }

        // Check if there is already a registered user of the
        // same name.
        consoleMessage("Creating pipe named :" + name + " of type :" + type);
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
        adv.setName(TalkNameTag + "." + name);
        adv.setDescription("Created by JXTA Shell 'talk' command");
        adv.setType(type);

        try {
            // Save the document into the public folder
            discovery.publish(adv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.DEFAULT_EXPIRATION);
        } catch (Exception e2) {
            printStackTrace("Advertisement could not be saved", e2);
            return ShellApp.appMiscError;
        }
        consoleMessage("User '" + name + "' is now registered");
        return ShellApp.appNoError;
    }


    private boolean checkUserAdv(PipeAdvertisement adv, String name) {

        if (adv == null) {
            return false;
        }
        if (adv.getName() == null) {
            return false;
        }
        String str = adv.getName();

        return (str.startsWith(TalkNameTag) && str.endsWith(name));
    }


    private PipeAdvertisement findUserAdv(String name) {

        Enumeration advs;
        if (name.toUpperCase().equals("IP2PGRP")) {
            return getMyJxtaPipeAdv();
        }
        // First look in the local storage
        try {
            advs = discovery.getLocalAdvertisements(DiscoveryService.ADV,
                    PipeAdvertisement.NameTag,
                    TalkNameTag + "." + name);


            PipeAdvertisement adv;
            while (advs.hasMoreElements()) {
                try {
                    adv = (PipeAdvertisement) advs.nextElement();
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
        } catch (Exception e) {
            //ignored
        }

        // Now, search remote


        discovery.getRemoteAdvertisements(null,
                DiscoveryService.ADV,
                PipeAdvertisement.NameTag,
                TalkNameTag + "." + name,
                2,
                this);

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
                //one more query
                if (i == MAXRETRIES / 2) {
                    discovery.getRemoteAdvertisements(null,
                            DiscoveryService.ADV,
                            PipeAdvertisement.NameTag,
                            TalkNameTag + "." + name,
                            2,
                            this);
                }
            } catch (Exception e) {
                //ignored
            }

            try {
                synchronized (this) {
                    if (results != null) {
                        for (Object result : results) {
                            try {
                                PipeAdvertisement adv = (PipeAdvertisement) result;
                                if (checkUserAdv(adv, name)) {
                                    return adv;
                                }
                            } catch (Exception e) {
                                //ignored
                            }
                        }
                        results.clear();
                    }
                }
            } catch (Exception e) {
                //ignored
            }
        }

        return null;
    }

    private void runDeamon(String name, PipeAdvertisement adv) {

        userName = name;
        try {
            pipeIn = getGroup().getPipeService().createInputPipe(adv);
        } catch (Exception ex) {
            printStackTrace("Could not open InputPipe for " + adv.getPipeID(), ex);
            return;
        }

        if (pipeIn == null) {
            consoleMessage("Could not open InputPipe for " + adv.getPipeID());
            return;
        }

        thread = new Thread(this, "Talk:Talk Deamon for " + name + "@" + getGroup().getPeerGroupName());
        thread.setDaemon(true);
        thread.start();

        CleanupShellObject daemon = new CleanupShellObject("Talk Deamon for : " + name + "@" + getGroup().getPeerGroupName(), this);

        // Store this object
        env.add(EnvName + "." + name + "@" + getGroup().getPeerGroupName(), daemon);
    }

    /**
     * Stops the talk daemon thread
     */
    private static class CleanupShellObject extends ShellObject<talk> {
        CleanupShellObject(String name, talk object) {
            super(name, object);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * <p/>Closes the pipe if it is still open.
         */
        @Override
        protected void finalize() throws Throwable {
            super.finalize();

            talk daemon = getObject();

            synchronized (daemon) {
                if (null != daemon.pipeIn) {
                    daemon.pipeIn.close();
                }
            }
        }
    }

    private void stopDeamon(String name) {

        // full name is required to find the object
        String dname = EnvName + "." + name + "@" + getGroup().getPeerGroupName();

        ShellObject obj = env.get(dname);
        if (obj == null) {
            // Notify user that the logout failed
            consoleMessage("Daemon(" + dname + ") for " + name + " not found");
            return;
        }

        try {
            talk daemon = (talk) obj.getObject();
            env.remove(dname);
            synchronized (daemon) {
                if (null != daemon.pipeIn) {
                    daemon.pipeIn.close();
                }
            }
        } catch (Exception e) {
            printStackTrace("Could not stop deamon for " + name, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run() {

        try {
            // Listen on the pipe
            while (true) {
                Message msg;
                try {
                    msg = pipeIn.waitForMessage();
                    if (null == msg) {
                        consoleMessage("Stop listening for " + userName);
                        break;
                    }
                } catch (InterruptedException woken) {
                    Thread.interrupted();
                    consoleMessage("Stop listening for " + userName);
                    return;
                }

                String senderName;

                // Get sender information
                MessageElement nameEl = msg.getMessageElement(SenderName);
                if (nameEl != null) {
                    senderName = nameEl.toString();
                } else {
                    println("received an unknown message");
                    continue;
                }

                if (null == senderName) {
                    senderName = "Anonymous";
                }

                String senderMessage;
                MessageElement msgEl = msg.getMessageElement(SenderMessage);
                if (msgEl != null) {
                    senderMessage = msgEl.toString();
                } else {
                    println("received an unknown message");
                    continue;
                }

                senderMessage += handleImageFromMsg(senderName, msg);

                // Get message
                if (senderMessage == null) {
                    consoleMessage(senderName + " to " + userName + "> [empty message]");
                    continue;
                }
                consoleMessage(senderName + " to " + userName + "> " + senderMessage);
            }
        } catch (Throwable all) {
            printStackTrace("Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
        } finally {
            synchronized (this) {
                pipeIn = null;
                thread = null;
            }
        }
    }

    /**
     * Generate uniquePipeID that is independantly unique within a group
     *
     * @return The uniquePipeID value
     */
    private PipeAdvertisement getMyJxtaPipeAdv() {

        byte[] preCookedPID = {
                (byte) 0xD1, (byte) 0xD1, (byte) 0xD1, (byte) 0xD1,
                (byte) 0xD1, (byte) 0xD1, (byte) 0xD1, (byte) 0xD1,
                (byte) 0xD1, (byte) 0xD1, (byte) 0xD1, (byte) 0xD1,
                (byte) 0xD1, (byte) 0xD1, (byte) 0xD1, (byte) 0xD1};

        PipeID id = IDFactory.newPipeID(getGroup().getPeerGroupID(), preCookedPID);
        PipeAdvertisement pipeAdv = (PipeAdvertisement)
                AdvertisementFactory.newAdvertisement(
                        PipeAdvertisement.getAdvertisementType());

        pipeAdv.setPipeID(id);
        pipeAdv.setName("JXME chat demo");
        pipeAdv.setType(PipeService.PropagateType);
        return pipeAdv;

    }

    private int syntaxError() {

        consoleMessage(getDescription());
        println("Usage: talk -[r]egister <userName> [-[s]ecure | -[p]ropagate]");
        println("       talk -[l]ogin <userName> ");
        println("       talk -[lo]gout <userName> ");
        println("       talk -[u] <senderName> <userName> ");
        println("       talk -search || -f <userName>");

        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Talk to another peer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     talk - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("    talk -[r]egister <userName> [-[s]ecure | -[p]ropagate]");
        println("    talk -[l]ogin <userName> ");
        println("    talk -[lo]gout <userName> ");
        println("    talk -[u] <senderName> <userName> ");
        println("    talk -search || -f");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("The 'talk command implements a simple instant messaging command");
        println("where two users on two remote peers can exchange messages.");
        println("Messages are displayed on the Shell stdout. In order");
        println("to use 'talk' the user needs to register himself. This is done");
        println("via the following steps:");
        println("Step 1: Register via 'talk -register <username>' command. This command");
        println("        creates a Talk advertisement for that user. This has to");
        println("        be done only once, the first time the user registers with");
        println("        talk. The system remembers it accross reboot.");
        println("        -secure can be added in order to establish a secure talk session.");
        println("        -propagate can be added in order to establish a chatroom style talk session.");
        println(" ");
        println("Step 2: Login  via 'talk -login <username>' command. This command");
        println("        logs the user and starts a listener daemon. This has to");
        println("        to be done everytime the peer is restarted.");
        println(" ");
        println("Step 3: User can talk to another user via the command");
        println("        'talk -u <myusername> <destusername>'. This command will prompt the user");
        println("        to enter the message he/she wants to send");
        println(" ");
        println("         JXTA>talk -u moi mike");
        println("         # talk : Connected to user mike");
        println("         Type your message. To exit, type '.' at beginning of line");
        println("  ");
        println("To stop receiving any more talk messages. The user can stop the talk");
        println("listener daemon by entering the command 'talk -logout <username>'");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("    -register register a new user name.");
        println("    -login    log user and set default user.");
        println("    -logout   logout.");
        println("    -search   search for talk users.");
        println("    -u        specify current user name");
        println(" ");
        println("EXAMPLES");
        println(" ");
        println("      JXTA>talk -register me");
        println("      JXTA>talk -login me");
        println("      JXTA>talk -search");
        println("      JXTA>talk -u me you");
        println(" ");
        println("This example shows how a new user 'me'  can register and log into talk,");
        println(" an talk to the user 'you'. User 'you' needs to be registered and logged on.");
        println(" ");
        println("SEE ALSO");
        println(" ");
        println("    xfer sftp mkpipe");
    }
}
