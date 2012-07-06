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
 *  $Id: SwingShellConsole.java,v 1.11 2007/02/09 23:12:40 hamada Exp $
 */
package net.jxta.impl.shell;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.text.BadLocationException;

import net.jxta.credential.Credential;
import net.jxta.exception.PeerGroupException;
import net.jxta.impl.rendezvous.RendezVousServiceInterface;
import net.jxta.impl.rendezvous.rpv.PeerView;
import net.jxta.impl.rendezvous.rpv.PeerViewEvent;
import net.jxta.impl.rendezvous.rpv.PeerViewListener;
import net.jxta.impl.shell.bin.Shell.Shell;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;


/**
 * A Swing based container for JXTA Shell sessions.
 */
public class SwingShellConsole extends ShellConsole {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(SwingShellConsole.class.getName());

    /**
     * The number of swing consoles opened.
     */
    private final static AtomicInteger consoleCount = new AtomicInteger(0);

    /**
     * Lines of console input awaiting shell processing.
     */
    private final List<String> lines = new ArrayList<String>();

    /**
     * The panel in which our stuff lives.
     */
    private JPanel panel;

    /**
     * Is this console embedded in another application? If {@code true} the 
     * Shell must ignore exit commands.
     */
    private final boolean embedded;

    /**
     * The frame that holds the TextArea object making up the console. If we
     * are running in a panel provided by someone else then this will not be
     * initialized.
     */
    private final JFrame frame;

    /**
     * The TextArea object that displays the status
     */
    private final JLabel statusStart;

    /**
     * The TextArea object that displays the status
     */
    private final JLabel statusEnd;

    /**
     * The TextArea object that displays the data
     */
    private final JTextArea text;

    /**
     * The length of the static text displayed, excludes the prompt and the
     * current input line.
     */
    private int textLength = 0;

    /**
     * Length of the prompt.
     */
    private int promptLength = 0;

    /**
     * Length of the current input line.
     */
    private int lineLength = 0;

    /**
     * Location of the insertion point within the current input line
     */
    private int lineInsert = 0;

    /**
     * Keeps bits for the status line.
     */
    private StatusKeeper statusKeeper = null;

    /**
     * Creates a new console window with rows row and cols columns to display.
     *
     * @param consoleName the name of the console
     * @param rootApp     The root shell application for this console instance.
     * @param rows        the number of rows displayed
     * @param cols        the number of columns displayed
     */
    public SwingShellConsole(ShellApp rootApp, String consoleName, int rows, int cols) {
        this(null, rootApp, consoleName, rows, cols);
    }

    /**
     * Creates a new console window with rows row and cols columns to display.
     *
     * @param inPanel     The panel in which the shell console will live.
     * @param rootApp     The root shell application for this console instance.
     * @param consoleName the name of the console
     * @param rows        the number of rows displayed
     * @param cols        the number of columns displayed
     */
    public SwingShellConsole(JPanel inPanel, ShellApp rootApp, String consoleName, int rows, int cols) {
        super(rootApp, consoleName);

        embedded = (null != inPanel);
                 
        // Shell embedded in another application 
        if (null == System.getProperty(Shell.JXTA_SHELL_EMBEDDED_KEY)) {
            try {
                System.setProperty(Shell.JXTA_SHELL_EMBEDDED_KEY, Boolean.toString(embedded));
            } catch (SecurityException couldntSet) {
                //ignored

            }
        }

        int fontsize = 12;
        String fontname = System.getProperty("SHELLFONTNAME", "Lucida Sans Typewriter");
        String fontsizeProp = System.getProperty("SHELLFONTSIZE");
        if (fontsizeProp != null) {
            try {
                fontsize = Integer.valueOf(fontsizeProp);
            } catch (NumberFormatException e) {
                // will use default size
            }
        }

        try {
            if (null == inPanel) {
                // We must provide a frame for the panel to live in.
                frame = new JFrame(consoleName + " - " + consoleCount.incrementAndGet());
                frame.setMinimumSize(new Dimension(fontsize * 18, fontsize * 6));
                frame.setLocation((fontsize * 4), (fontsize * 4));
                panel = new JPanel(new GridBagLayout());
                frame.getContentPane().add(panel);
                frame.addWindowListener(new WindowAdapter() {

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void windowClosing(WindowEvent e) {
                        ShellApp sa = getShellApp();

                        if (null != sa) {
                            sa.stopApp();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void windowGainedFocus(WindowEvent e) {
                        text.getCaret().setVisible(true);
                        text.setEditable(false);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void windowLostFocus(WindowEvent e) {
                        text.getCaret().setVisible(false);
                    }
                });
            } else {
                frame = null;
                panel = inPanel;
                panel.setLayout(new GridBagLayout());
            }
        } catch (InternalError error) {
            HeadlessException horseman = new HeadlessException("InternalError");
            horseman.initCause(error);
            throw horseman;
        }

        this.statusStart = new JLabel(" ", JLabel.LEADING);
        statusStart.setFont(new Font(fontname, Font.PLAIN, fontsize));

        GridBagConstraints constr = new GridBagConstraints();

        constr.gridwidth = 1;
        constr.gridheight = 1;
        constr.gridx = 0;
        constr.gridy = 0;
        constr.weightx = 1;
        constr.weighty = 0;
        constr.anchor = GridBagConstraints.FIRST_LINE_START;
        constr.fill = GridBagConstraints.HORIZONTAL;

        panel.add(statusStart, constr);

        this.statusEnd = new JLabel(" ", JLabel.TRAILING);
        statusEnd.setFont(new Font(fontname, Font.PLAIN, fontsize));

        constr.gridwidth = 1;
        constr.gridheight = 1;
        constr.gridx = 1;
        constr.gridy = 0;
        constr.weightx = 1;
        constr.weighty = 0;
        constr.anchor = GridBagConstraints.FIRST_LINE_END;
        constr.fill = GridBagConstraints.HORIZONTAL;

        panel.add(statusEnd, constr);

        this.text = new JTextArea();
        text.setRows(rows);
        text.setColumns(cols);
        text.setFont(new Font(fontname, Font.PLAIN, fontsize));
        text.setEditable(false);
        text.addKeyListener(new SwingShellConsole.keyHandler());
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.getCaret().setVisible(true);

        JScrollPane stsp = new JScrollPane();
        stsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        stsp.getViewport().add(text);
        stsp.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);

        constr.gridwidth = 2;
        constr.gridheight = 1;
        constr.gridx = 0;
        constr.gridy = 1;
        constr.weightx = 1;
        constr.weighty = 1;
        constr.anchor = GridBagConstraints.LAST_LINE_END;
        constr.fill = GridBagConstraints.BOTH;

        panel.add(stsp, constr);

        if (null != frame) {
            frame.pack();
            frame.setVisible(true);
        }
        text.requestFocus();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Terminates the current console window. If this was the last window open,
     * the program is terminated.
     */
    @Override
    public synchronized void destroy() {
        super.destroy();

        if (frame != null) {
            frame.dispose();
        }
        panel = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String read() throws InterruptedIOException {
        synchronized (lines) {
            while ((null != panel) && lines.isEmpty()) {
                try {
                    lines.wait(0);
                } catch (InterruptedException woken) {
                    Thread.interrupted();
                    InterruptedIOException wake = new InterruptedIOException("Interrupted");
                    wake.initCause(woken);
                    throw wake;
                }
            }

            if (null == panel) {
                return null;
            }

            return lines.remove(0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(String msg) {
        try {
            text.getCaret().setVisible(false);
            text.insert(msg, textLength);
            textLength += msg.length();
            text.setCaretPosition(textLength + promptLength + lineInsert);
            text.getCaret().setVisible(true);
        } catch (RuntimeException ohno) {
            LOG.log(Level.SEVERE, "Failure : TextLength=" + textLength + " promptLength=" + promptLength +
                    " lineLength=" + lineLength + " text=" + text.getText().length(), ohno);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        try {
            text.setText("");
            textLength = 0;
            promptLength = 0;
            lineLength = 0;
            lineInsert = 0;
            text.setCaretPosition(textLength + promptLength + lineInsert);
            text.getCaret().setVisible(true);
        } catch (RuntimeException ohno) {
            LOG.log(Level.SEVERE, "Failure : TextLength=" + textLength + " promptLength=" + promptLength +
                    " lineLength=" + lineLength + " text=" + text.getText().length(), ohno);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setPrompt(String newPrompt) {
        try {
            text.replaceRange(newPrompt, textLength, textLength + promptLength);
            promptLength = newPrompt.length();
            text.setCaretPosition(textLength + promptLength + lineInsert);
            text.getCaret().setVisible(true);
        } catch (RuntimeException ohno) {
            LOG.log(Level.SEVERE, "Failure : TextLength=" + textLength + " promptLength=" + promptLength +
                    " lineLength=" + lineLength + " text=" + text.getText().length(), ohno);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setCommandLine(String cmd) {
        try {
            text.replaceRange(cmd, textLength + promptLength,
                    textLength + promptLength + lineLength);
            lineLength = cmd.length();
            lineInsert = lineLength;
            text.setCaretPosition(textLength + promptLength + lineInsert);
            text.getCaret().setVisible(true);
        } catch (RuntimeException ohno) {
            LOG.log(Level.SEVERE, "Failure : TextLength=" + textLength + " promptLength=" + promptLength + 
                    " lineLength=" + lineLength + " text=" + text.getText().length(), ohno);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setStatusGroup(PeerGroup group) {
        // remove listeners
        if (null != statusKeeper) {
            MembershipService membership = statusKeeper.statusGroup.getMembershipService();
            membership.removePropertyChangeListener("defaultCredential", statusKeeper.membershipProperties);
            statusKeeper.membershipProperties = null;
            RendezVousService rendezVous = statusKeeper.statusGroup.getRendezVousService();
            rendezVous.removeListener(statusKeeper.rendezvousEventListener);
            statusKeeper.rendezvousEventListener = null;

            if (rendezVous instanceof RendezVousServiceInterface) {
                RendezVousServiceInterface stdRdv = (RendezVousServiceInterface) rendezVous;
                PeerView rpv = stdRdv.getPeerView();
                if (null != rpv) {
                    rpv.removeListener(statusKeeper.peerviewEventListener);
                    statusKeeper.peerviewEventListener = null;
                }
            }
            statusKeeper = null;
        }

        // install listeners
        if (null != group) {
            statusKeeper = new StatusKeeper(group);
            MembershipService membership = statusKeeper.statusGroup.getMembershipService();
            statusKeeper.membershipProperties = new MembershipPropertyListener();
            membership.addPropertyChangeListener("defaultCredential", statusKeeper.membershipProperties);
            try {
                statusKeeper.credential = (membership.getDefaultCredential() != null);
            } catch (PeerGroupException failed) {
                statusKeeper.credential = false;
            }

            RendezVousService rendezVous = statusKeeper.statusGroup.getRendezVousService();
            statusKeeper.rendezvousEventListener = new RendezvousEventListener();
            rendezVous.addListener(statusKeeper.rendezvousEventListener);
            statusKeeper.rendezvous = rendezVous.isRendezVous();
            statusKeeper.connectedClients = rendezVous.getConnectedPeerIDs().size();
            statusKeeper.connectedRdv = Collections.list(rendezVous.getConnectedRendezVous()).size();

            if (rendezVous instanceof RendezVousServiceInterface) {
                RendezVousServiceInterface stdRdv = (RendezVousServiceInterface) rendezVous;
                PeerView rpv = stdRdv.getPeerView();
                if (null != rpv) {
                    statusKeeper.peerviewEventListener = new PeerViewEventListener();
                    rpv.addListener(new PeerViewEventListener());
                    statusKeeper.peerview = statusKeeper.statusGroup.getRendezVousService().getLocalWalkView().size();
                } else {
                    statusKeeper.peerview = -1;
                }
            }
            updateStatusString();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCursorDownName() {
        return KeyEvent.getKeyText(KeyEvent.VK_DOWN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCursorUpName() {
        return KeyEvent.getKeyText(KeyEvent.VK_UP);
    }

    /**
     * Handle key actions
     */
    private class keyHandler extends KeyAdapter {

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void keyPressed(KeyEvent e) {
            final int val = e.getKeyCode();
            final char ch = e.getKeyChar();
            final int mod = e.getModifiers();

            boolean consumed;

            // There may be user confusion about Ctrl-C: since this is a shell,
            // users might expect Ctrl-C to terminate the currently running
            // command. At this writing, we don't have command termination, so
            // we'll go ahead and grab Ctrl-C for copy.  (Suggest "ESC" for
            // termination...)

            try {
                if ((mod & InputEvent.CTRL_MASK) != 0) {
                    consumed = control(val);
                } else {
                    if (KeyEvent.CHAR_UNDEFINED == ch) {
                        consumed = handling(val);
                    } else {
                        consumed = typing(ch);
                    }
                }
                if (consumed) {
                    text.setCaretPosition(textLength + promptLength + lineInsert);
                    text.getCaret().setVisible(true);
                    // consume the event so that it doesn't get processed by the TextArea control.
                    e.consume();
                }
            } catch (RuntimeException ohno) {
                LOG.log(Level.SEVERE, "Failure : TextLength=" + textLength + " promptLength=" + promptLength +
                    " lineLength=" + lineLength + " text=" + text.getText().length(), ohno);
            }
        }
    }

    /**
     * Handles non-character editing of the command line. Handling is as follows:
     * <p/>
     * <p/><ul>
     * <li> Ctrl-C - copys the current selection to the Clipboard.</li>
     * <li> Ctrl-V - Inserts text from the ClipBoard into the current line.</li>
     * <li> Ctrl-D - Sends an EOT command.</li>
     * <li> Ctrl-L - Clear the text area.</li>
     * <li> Ctrl-U - Clear the command line</li>
     * <li> Ctrl-bksp - Clear the command line</li>
     * </ul>
     * <p/>
     * <p/>There may be user confusion about Ctrl-C: since this is a shell, users
     * might expect Ctrl-C to terminate the currently running command.
     * At this writing, we don't have command termination, so we'll go ahead
     * and grab Ctrl-C for copy.  (Suggest Esc for termination...)
     *
     * @param val       the KeyCode value of the key pressed
     */
    private boolean control(int val) {
        switch (val) {
            case KeyEvent.VK_C:
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("--> COPY <--");
                }
                copy();
                return true;
            case KeyEvent.VK_V:
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("--> PASTE <--");
                }
                paste();
                return true;
                // Let's try a ^D quit...
            case KeyEvent.VK_D:
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("--> QUIT <--");
                }
                setCommandLine("\004");
                submit(true);
                setCommandLine("");
                return true;
            case KeyEvent.VK_L:
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("--> CLEAR <--");
                }
                setCommandLine("clear");
                submit(true);
                return true;
            case KeyEvent.VK_U:
            case KeyEvent.VK_BACK_SPACE:
                setCommandLine("");
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles non-character editing of the command line. Handling is as follows:
     * <p/>
     * <p/><ul>
     * <li> Cursor keys left and right - move the caret and update the
     * current insertLine value
     * <li> <Home> - Move cursor to the beginning of the current line.
     * <li> <End> - Move cursor to the end of the current line.
     * </ul>
     *
     * @param val       the KeyCode value of the key pressed
     * @return  ture if val is non-character
     */
    private boolean handling(int val) {
        switch (val) {
            case KeyEvent.VK_HOME:
                lineInsert = 0;
                return true;

            case KeyEvent.VK_END:
                lineInsert = lineLength;
                return true;

            case KeyEvent.VK_KP_LEFT:
            case KeyEvent.VK_LEFT:
                lineInsert--;
                if (lineInsert < 0) {
                    lineInsert = 0;
                }
                return true;

            case KeyEvent.VK_KP_RIGHT:
            case KeyEvent.VK_RIGHT:
                lineInsert++;
                if (lineInsert > lineLength) {
                    lineInsert = lineLength;
                }
                return true;

            case KeyEvent.VK_KP_UP:
            case KeyEvent.VK_UP:
                setCommandLine(getCursorUpName());
                submit(false);
                return true;

            case KeyEvent.VK_KP_DOWN:
            case KeyEvent.VK_DOWN:
                setCommandLine(getCursorDownName());
                submit(false);
                return true;

            default:
                return false;
        }
    }

    /**
     * Handles the editing of the command line. Handling is as follows:
     * <p/>
     * <p/><ul>
     * <li> backspace - Delete the character ahead of lineInsert from input line.</li>
     * <li> delete - Delete the character after lineInsert from input line.</li>
     * <li>enter - Finish the input line by calling <code>submit()</code>.</li>
     * <li>otherwise insert the character.</li>
     * </ul>
     *
     * @param ch        the character associated with the key pressed
     * @return if ch is BS, DEL, or Enter
     */
    private boolean typing(char ch) {

        switch (ch) {
            case KeyEvent.VK_BACK_SPACE:
                if (lineInsert >= 1 && lineInsert <= lineLength) {
                    text.replaceRange("", textLength + promptLength + lineInsert - 1, textLength + promptLength + lineInsert);
                    lineInsert--;
                    lineLength--;
                }
                return true;

            case KeyEvent.VK_DELETE:
                if (lineInsert < lineLength) {
                    text.replaceRange("", textLength + promptLength + lineInsert, textLength + promptLength + lineInsert + 1);
                    lineLength--;
                }
                return true;

            case KeyEvent.VK_ENTER:
                submit(true);
                return true;

            default:
                text.insert(Character.toString(ch), textLength + promptLength + lineInsert++);
                lineLength++;
                return true;
        }
    }

    /**
     * Copy the selection to the system clipboard.
     */
    private void copy() {

        String selection = text.getSelectedText();

        if ((null != selection) && (selection.length() > 0)) {
            StringSelection select = new StringSelection(selection);
            Clipboard clip = text.getToolkit().getSystemClipboard();
            clip.setContents(select, select);
        }
    }

    /**
     * Paste text from the clipboard into the shell. Text is added to at the
     * end of the current command line. If the clipboard contents is non-text,
     * we'll bail out silently.
     */
    private void paste() {

        Clipboard cb = text.getToolkit().getSystemClipboard();
        Transferable trans = cb.getContents(this);
        if (trans == null) {
            return;
        }

        String cbText;
        try {
            cbText = (String) trans.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            return;
        } catch (IOException e) {
            return;
        }

        if (cbText == null) {
            return;
        }

        // Add the clipboard text to the end of the current command line.
        // If there are multiple lines in the clipboard, we paste and
        // execute each line as if the user entered it and and hit return.
        int current = 0;
        boolean fullLine = true;
        do {
            int lineEnd = cbText.indexOf('\n', current);

            if (-1 == lineEnd) {
                lineEnd = cbText.length();
                fullLine = false;
            }

            // Append text to the current line.
            String aLine = cbText.substring(current, lineEnd);
            text.insert(aLine, textLength + promptLength + lineInsert);
            lineInsert += aLine.length();
            lineLength += aLine.length();

            if (fullLine) {
                submit(true);
            }
            current = lineEnd + 1;
        } while (current < cbText.length());
    }

    /**
     * Finishes an input line and provides it as input to the console reader.
     *
     * @param appendNewLine Clear the line and append a newline
     */
    private void submit(boolean appendNewLine) {

        synchronized (lines) {
            try {
                lines.add(text.getText(textLength + promptLength, lineLength) + "\n");
            } catch (BadLocationException ble) {
                IllegalArgumentException badLoc = new IllegalArgumentException("bad location");
                badLoc.initCause(ble);
                throw badLoc;
            }

            if (appendNewLine) {
                text.append("\n");
                textLength += promptLength + lineLength + 1;
                promptLength = 0;
                lineLength = 0;
                lineInsert = 0;
            }

            lines.notify();
        }
    }

    /**
     * Container for status statistics
     */
    private static class StatusKeeper {

        final PeerGroup statusGroup;

        boolean credential = false;

        boolean rendezvous = false;

        int peerview = -1;

        int connectedClients = 0;

        int clientReconnects = 0;

        int clientDisconnects = 0;

        int clientFailures = 0;

        int connectedRdv = 0;

        int rdvReconnects = 0;

        int rdvDisconnects = 0;

        int rdvFailures = 0;

        SwingShellConsole.MembershipPropertyListener membershipProperties = null;

        SwingShellConsole.RendezvousEventListener rendezvousEventListener = null;

        PeerViewListener peerviewEventListener = null;

        StatusKeeper(PeerGroup group) {
            statusGroup = group;
        }
    }

    private void updateStatusString() {

        StringBuilder status = new StringBuilder();

        status.append(statusKeeper.credential ? " AUTH" : " auth");
        status.append(" : ");
        status.append(statusKeeper.rendezvous ? "RDV  " : "EDGE ");
        status.append(statusKeeper.peerview > 0 ? " pv:" + statusKeeper.peerview : "");
        status.append("  rdv: ").append(statusKeeper.connectedRdv).append(" / ").append(statusKeeper.rdvReconnects).append(":").append(statusKeeper.rdvDisconnects).append(":").append(statusKeeper.rdvFailures);
        status.append("  client: ").append(statusKeeper.connectedClients).append(" / ").append(statusKeeper.clientReconnects).append(":").append(statusKeeper.clientDisconnects).append(":").append(statusKeeper.clientFailures);

        Runtime vm = Runtime.getRuntime();

        String vmStats = Long.toString(vm.freeMemory() / 1024) + "k/" + Long.toString(vm.totalMemory() / 1024) + "k";

        statusStart.setText(status.toString());

        statusEnd.setText(vmStats);
    }

    /**
     * Monitors property changed events for Membership Service
     */
    private class MembershipPropertyListener implements PropertyChangeListener {

        /**
         * {@inheritDoc}
         */
        public synchronized void propertyChange(PropertyChangeEvent evt) {
            Credential cred = (Credential) evt.getNewValue();

            statusKeeper.credential = (null != cred);

            updateStatusString();
        }
    }

    /**
     * Monitors property changed events for Membership Service
     */
    private class RendezvousEventListener implements RendezvousListener {

        /**
         * {@inheritDoc}
         */
        public synchronized void rendezvousEvent(RendezvousEvent event) {

            int theEventType = event.getType();

            if (LOG.isLoggable(Level.FINE)) {
                LOG.finer("[" + statusKeeper.statusGroup.getPeerGroupName() + "] Processing " + event);
            }

            switch (theEventType) {
                case RendezvousEvent.RDVCONNECT:
                    statusKeeper.connectedRdv++;
                    break;
                case RendezvousEvent.RDVRECONNECT:
                    statusKeeper.rdvReconnects++;
                    break;
                case RendezvousEvent.RDVFAILED:
                    statusKeeper.rdvFailures++;
                    statusKeeper.connectedRdv--;
                    break;
                case RendezvousEvent.RDVDISCONNECT:
                    statusKeeper.rdvDisconnects++;
                    statusKeeper.connectedRdv--;
                    break;
                case RendezvousEvent.CLIENTCONNECT:
                    statusKeeper.connectedClients++;
                    break;
                case RendezvousEvent.CLIENTRECONNECT:
                    statusKeeper.clientReconnects++;
                    break;
                case RendezvousEvent.CLIENTFAILED:
                    statusKeeper.clientFailures++;
                    statusKeeper.connectedClients--;
                    break;
                case RendezvousEvent.CLIENTDISCONNECT:
                    statusKeeper.clientDisconnects++;
                    statusKeeper.connectedClients--;
                    break;
                case RendezvousEvent.BECAMERDV:
                    statusKeeper.rendezvous = true;
                    break;
                case RendezvousEvent.BECAMEEDGE:
                    statusKeeper.rendezvous = false;
                    break;
                default:
                    if (LOG.isLoggable(Level.WARNING)) {
                        LOG.warning("[" + statusKeeper.statusGroup.getPeerGroupName() + "] Unexpected RDV event : " + event);
                    }
                    break;
            }

            updateStatusString();
        }
    }

    /**
     * Monitors property changed events for Membership Service
     */
    private class PeerViewEventListener implements PeerViewListener {

        /**
         * {@inheritDoc}
         */
        public synchronized void peerViewEvent(PeerViewEvent event) {

            int theEventType = event.getType();

            if (LOG.isLoggable(Level.FINE)) {
                LOG.finer("[" + statusKeeper.statusGroup.getPeerGroupName() + "] Processing " + event);
            }

            switch (theEventType) {
                case PeerViewEvent.ADD:
                    statusKeeper.peerview++;
                    break;
                case PeerViewEvent.FAIL:
                    statusKeeper.peerview--;
                    break;
                case PeerViewEvent.REMOVE:
                    statusKeeper.peerview--;
                    break;
                default:
                    if (LOG.isLoggable(Level.WARNING)) {
                        LOG.warning("[" + statusKeeper.statusGroup.getPeerGroupName() + "] Unexpected PeerView event : " + event);
                    }
                    break;
            }

            updateStatusString();
        }
    }
}
