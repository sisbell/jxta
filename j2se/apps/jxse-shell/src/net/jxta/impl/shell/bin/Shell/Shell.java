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
 *  $Id: Shell.java,v 1.86 2007/02/09 23:12:45 hamada Exp $
 */
package net.jxta.impl.shell.bin.Shell;

import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.impl.shell.*;
import net.jxta.impl.shell.bin.history.HistoryQueue;
import net.jxta.impl.shell.bin.join.join.PeerGroupShellObject;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import net.jxta.logging.Logging;
import net.jxta.platform.Module;

/**
 * This class implements a JXTA Shell
 */
public class Shell extends ShellApp implements Runnable {

    /**
     * Logger
     */
    private static final transient java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Shell.class.getName());

    /**
     * Description of the Field
     */
    public final static String HISTORY_ENV_NAME = "History";

    /**
     * Description of the Field
     */
    public final static String JXTA_SHELL_EMBEDDED_KEY = "JXTA_SHELL_EMBEDDED";

    /**
     * Description of the Field
     */
    public final static String PARENT_SHELL_ENV_NAME = "parentShell";

    /**
     * Description of the Field
     */
    public final static String CMD_PROMPT = "JXTA>";

    /**
     *  Tracks how many shell instances we have created.
     */
    private static AtomicInteger shellInstance = new AtomicInteger(0);

    /**
     *  Unique instance number for this shell instance.
     */
    private final int thisInstance;

    private ShellConsole cons = null;

    private boolean execShell = true;

    private boolean gotMyOwnFrame = false;

    /**
     * If true then this shell is a sub-shell of a another shell.
     */
    private boolean gotParent = false;

    /**
     * The shell environment of our parent shell.
     */
    private ShellEnv parentEnv = null;

    private String pipecmd = null;

    private BufferedReader scriptReader = null;

    /**
     * Child Shells will install a env var in their parent for their instance
     */
    private String parentEnvEnvName = null;

    private Thread thread = null;

    /**
     * Default constructor (don't delete)
     */
    public Shell() {
        thisInstance = shellInstance.incrementAndGet();
    }

    /**
     * Create a new shell with the specified console
     */
    public Shell(ShellConsole console) {
        this();

        cons = console;
    }


    /**
     * Create a new shell with embedded functionality.
     *
     * @param embedded
     */
    public Shell(boolean embedded) {
        this();

        System.setProperty(JXTA_SHELL_EMBEDDED_KEY, Boolean.toString(embedded));
    }

    /**
     * Main processing method for the Shell object
     */
    public void run() {
        try {
            if (null != pipecmd) {
                startApp(new String[0]);
            } else {
                runShell();
            }
        } catch (Throwable all) {
            System.out.flush();
            System.err.println("Uncaught Throwable in thread :" + Thread.currentThread().getName());
            all.printStackTrace(System.err);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {

        GetOpt options = new GetOpt(argv, "xsf:e:");

        while (true) {
            int option;
            try {
                option = options.getNextOption();
            } catch (IllegalArgumentException badopt) {
                consoleMessage("Illegal argument :" + badopt);
                return syntaxError();
            }

            if (-1 == option) {
                break;
            }

            switch (option) {
                case'f':
                    String scriptfile = options.getOptionArg();
                    if (!initScriptFile(scriptfile)) {
                        consoleMessage("Cannot access " + scriptfile);
                        return ShellApp.appMiscError;
                    }
                    break;

                case'e':
                    execScript(options.getOptionArg());
                    return Shell.appNoError;

                case's':
                    gotMyOwnFrame = true;
                    break;

                case'x':
                    execShell = false;
                    break;

                default:
                    return syntaxError();
            }
        }

        ShellEnv env = getEnv();

        if (null == env) {
            // There is no Parent Shell

            gotMyOwnFrame = true;
            env = new ShellEnv();

            ShellObject<PeerGroup> stdgrpobj = new PeerGroupShellObject("Default Group", getGroup());
            env.add("stdgroup", stdgrpobj);

            PeerGroup child = null;
            PeerGroup current = getGroup();
            while (true) {
                PeerGroup next = current.getParentGroup();
                if (next == null) {
                    break;
                }
                child = current;
                current = next;
            }

            // Unless one of our ancestor groups does not support getParent and thus we
            // know nothing, we can always find the platform.
            if (current != null) {
                ShellObject<PeerGroup> worldgrpobj;
                if (current.getPeerGroupID().equals(getGroup().getPeerGroupID())) {
                    worldgrpobj = stdgrpobj;
                } else {
                    worldgrpobj = new PeerGroupShellObject("World Peer Group", current);
                }

                env.add("worldgroup", worldgrpobj);
            }

            // Unless our initial group is the platform, our before-last ancestor is
            // the netpg.
            if (child != null) {
                ShellObject<PeerGroup> rootgrpobj;
                if (child.getPeerGroupID().equals(getGroup().getPeerGroupID())) {
                    rootgrpobj = stdgrpobj;
                } else {
                    rootgrpobj = new PeerGroupShellObject("Root Peer Group", child);
                }

                env.add("rootgroup", rootgrpobj);
            }
        } else {
            // This is a child Shell.

            gotParent = true;

            // Recover the parent env, and duplicate it
            parentEnv = env;

            env = new ShellEnv(parentEnv);

            parentEnvEnvName = parentEnv.createName();

            // Store this Shell into the parent's environment
            parentEnv.add(parentEnvEnvName, new ShellObject<Shell>("Child Shell " + Integer.toString(thisInstance), this));

            // and store our parent into our environment
            ShellObject<Shell> parentShell = (ShellObject<Shell>) parentEnv.get("shell");
            if (parentShell != null) {
                env.add(PARENT_SHELL_ENV_NAME, parentShell);
            }
        }

        setEnv(env);

        // Store this Shell into the environment

        /*
        * Hardwiring the shell environment variable here allows us to retrieve
        * it from exit (or wherever), when we need to get the current instance
        * of the shell.
        */
        env.add("shell", new ShellObject<Shell>("Shell " + Integer.toString(thisInstance), this));

        if (gotMyOwnFrame) {
            if (null == cons) {
                cons = ShellConsole.newConsole(this, "JXTA Shell - (" + getGroup().getPeerName() + ")");
            }

            env.add("console", new ShellObject<ShellConsole>("console", cons));

            cons.setStatusGroup(getGroup());

            // Create the default InputPipe
            ShellInputPipe defaultInputPipe = new ShellInputPipe(getGroup(), cons);

            env.add("stdin", new ShellObject<InputPipe>("Default InputPipe", defaultInputPipe));

            env.add("consin", new ShellObject<InputPipe>("Default Console InputPipe", defaultInputPipe));

            setInputPipe(defaultInputPipe);
            setInputConsPipe(defaultInputPipe);

            // Create the default OutputPipe
            ShellOutputPipe defaultOutputPipe = new ShellOutputPipe(getGroup(), cons);

            env.add("stdout", new ShellObject<OutputPipe>("Default OutputPipe", defaultOutputPipe));

            env.add("consout", new ShellObject<OutputPipe>("Default Console OutputPipe", defaultOutputPipe));

            setOutputPipe(defaultOutputPipe);
            setOutputConsPipe(defaultOutputPipe);

            // start the shell on its own thread.
            thread = new Thread(getGroup().getHomeThreadGroup(), this, "JXTA Shell " + thisInstance);
            thread.start();

            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("Shell started.");
            }
            // HACK 20070814 bondolo This test is required because the Shell uses a return value (-1) not supported by StdPeerGroup.
            if(null == argv) {
                return Module.START_OK;
            } else {
                return ShellApp.appSpawned;
            }
        } else {
            ShellObject console = env.get("console");

            cons = (ShellConsole) console.getObject();

            if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
                LOG.info("Shell starting.");
            }

            // a child shell just runs until exit.
            if (null != pipecmd) {
                processCmd(pipecmd);
            } else {
                runShell();
            }

            return ShellApp.appNoError;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void stopApp() {
        // Only stop once.
        if(stopped) {
            return;
        }
        
        super.stopApp();

        // Remove itself from the parent ShellEnv (GC)
        if (parentEnv != null) {
            parentEnv.remove(parentEnvEnvName);
        }

        // Destroy ourself
        if (gotMyOwnFrame) {
            cons.setStatusGroup(null);
            cons.destroy();
            cons = null;
        }

        // Interrupt our parsing thread.
        if (thread != null) {
            thread.interrupt();
        }

        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("Shell stopped.");
        }

        if (isRootShell()) {
            PeerGroup pg = getGroup();

            getEnv().clear();

            // Until we fix all non-daemon threads in non-jxta code...
            if (!isEmbedded()) {
                pg.stopApp();

//                System.exit(0);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p/>Use the value of stdgroup environment variable.
     */
    @Override
    public PeerGroup getGroup() {
        ShellEnv env = getEnv();

        ShellObject stdgroup = null;
        if (null != env) {
            stdgroup = env.get("stdgroup");
        }

        return (null != stdgroup) ? (PeerGroup) stdgroup.getObject() : super.getGroup();
    }

    /**
     * if true then this is a root shell. A root shell is the shell which owns
     * the console.
     *
     * @return if true then this shell owns the console.
     */
    public boolean isRootShell() {
        return !gotParent;
    }

    /**
     * Description of the Method
     *
     * @return error result code.
     */
    private int syntaxError() {
        consoleMessage("Usage: Shell [-f filename] [-e cmds] [-s] [-x]");

        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "JXTA Shell command interpreter";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("   Shell  - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("   Shell [Shell [-f filename] [-e cmds] [-s] [-x]");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        info();
        println(" ");
        println("Every Shell command is of the form <cmd>;..;<cmd>. The ';'");
        println("is used as command separator. Multiple commands can be entered");
        println("in one line. Pipelines can be created by combining the pipe stdout");
        println("of a command into the pipe stdin of another command using the");
        println("pipe '|' operator. For example the following command:");
        println(" ");
        println("   JXTA> cat env1 | more");
        println(" ");
        println("Pipes the output of the command 'cat' into the stdin of the command ");
        println("'more'. An arbitrary number of commands can be pipelined together with ");
        println("th '|' pipe operator.");
        println(" ");
        println("The '=' operator can be used to assign the value of a command ");
        println("output to an environment variable. For example :");
        println(" ");
        println("   JXTA> myadv = newpipe -n mypipe");
        println(" ");
        println(" This command creates a new pipe advertisement and stores it in the");
        println(" 'myadv' environment variable.");
        println(" ");
        println("COMMANDS");
        println(" ");
        println(" The Shell provides the following built-in commands: ");
        println(" ");
        println("   clear      Clear the shell's screen");
        println(" ");
        println(" The additional commands available may be discovered by : ");
        println(" ");
        println("   man ");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("    [-f <filename>] Execute the script file");
        println("    [-e <commands>] Execute the commands");
        println("    [-s]            Fork a new Shell console in a new window");
        println("    [-x]            Skip execution of .jshrc");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA> Shell -f /home/tra/batch");
        println(" ");
        println("This command executes the commands stored in the Shell script");
        println("file '/home/tra/myfile' in the current Shell environment.");
        println(" ");
        println("ENVIRONMENT");
        println(" ");
        println("The Java implementation of 'Shell' uses two Java system");
        println("properties to configure the use of GUI:");
        println("    SHELLNOWINDOW - if 'true' then no window is created.");
        println("    SHELLFONTSIZE - Size in points for the font used in GUI window.");
        println(" ");
        println("The following environment variables are defined by default:");
        println(" ");
        println("    consin      = Default Console InputPipe");
        println("    consout     = Default Console OutputPipe");
        println("    stdout      = Default OutputPipe");
        println("    stdin       = Default InputPipe");
        println("    shell       = This Shell object");
        println("    stdgroup    = Default peer group");
        println("    rootgroup   = Default Infrastructure group");
        println("    worldgroup  = World PeerGroup");
        println("    echo        = (if defined) Echo all commands before executing them");
        println("    parentShell = (if defined) For child Shells this is the parent Shell");
        println(" ");
        println("FILES");
        println(" ");
        println("  $CWD/.jshrc");
        println(" ");
        println("   A default startup script that is executed when the Shell ");
        println("   is invoked.");
        println(" ");
        println("SEE ALSO");
        println("    exit env ");
    }

    /**
     * Description of the Method
     */
    private void info() {
        println(" ");
        println("The JXTA Shell provides an interactive environment to the JXTA " +
                "platform. The Shell provides basic commands to discover peers and " +
                "peergroups, to join and resign from peergroups, to create pipes " +
                "between peers, and to send pipe messages. ");
        println(" ");
        println("The Shell provides environment  variables that permit binding " +
                "symbolic names to JXTA platform objects. Environment variables " +
                "allow Shell commands to exchange data. The command 'env' " +
                "displays all defined environment variables in the current Shell " +
                "session.");
        println(" ");
        println("The Shell creates a JXTA InputPipe (stdin) for reading input from " +
                "the keyboard, and a JXTA OutputPipe (stdout) to display information " +
                "on the Shell console. All commands executed by the Shell have their " +
                "initial 'stdin' and 'stdout' set up to the Shell's stdin and stdout pipes. " +
                "The Shell also creates the environment variable 'stdgroup' that " +
                "contains the current JXTA PeerGroup in which the Shell and commands " +
                "are executed.");
        println(" ");
        println("The 'man' command is available to list the commands available. " +
                "Type 'man <command>' to get help about a particular command. " +
                "To exit the Shell, use the 'exit' command. ");
    }

    /**
     * Description of the Method
     *
     * @param script
     */
    private void execScript(String script) {
        try {
            processMultipleCmd(script);
        } catch (Exception ez1) {
            printStackTrace("Failed with ", ez1);
        }
    }

    /**
     * Returns the ShellConsole object associated with this shell. It may be
     * null, if this shell does not run in its own window
     *
     * @return ShellConsole object associated with this shell
     */
    public ShellConsole getConsole() {
        return cons;
    }

    /**
     * Returns the HistoryQueue that holds the cmds in a history list If the
     * queue does not exist, it is created.
     *
     * @return HistoryQueue object used to retrieve commands
     */
    private HistoryQueue getHistoryQueue() {
        ShellEnv env = getEnv();
        ShellObject obj = env.get(HISTORY_ENV_NAME);
        HistoryQueue queue = null;

        if (obj != null) {
            if (HistoryQueue.class.isAssignableFrom(obj.getObjectClass())) {
                queue = (HistoryQueue) obj.getObject();
            }
        } else {
            String exclude[] = {cons.getCursorDownName(), cons.getCursorUpName()};

            queue = new HistoryQueue(exclude);
            env.add(HISTORY_ENV_NAME, new ShellObject<HistoryQueue>("History", queue));
        }

        return queue;
    }

    /**
     * Prepare to run a command on a pipe.
     *
     * @param cmd The command to be executed.
     */
    private void initPipe(String cmd) {
        String myName = "JXTA Shell - " + thisInstance + " : [" + cmd + "]";

        pipecmd = cmd;
        thread = new Thread(getGroup().getHomeThreadGroup(), this, myName);
        thread.start();
    }

    /**
     * Description of the Method
     *
     * @param fn
     * @return {@code true} if the script file was found otherwise false {@code false}.
     */
    private boolean initScriptFile(String fn) {
        try {
            scriptReader = new BufferedReader(new FileReader(fn));
            return true;
        } catch (Exception e) {
            printStackTrace("Failed with ", e);
            return false;
        }
    }

    /**
     * Description of the Method
     */
    private void startupFile() {
        File startupFile = new File(".jshrc");

        if (!startupFile.exists()) {
            return;
        }

        BufferedReader scriptReader = null;
        try {
            scriptReader = new BufferedReader(new FileReader(startupFile));
            // nothing to do
            String cmd = scriptReader.readLine();

            while (cmd != null) {
                processMultipleCmd(cmd);
                cmd = scriptReader.readLine();
            }
        }
        catch (Exception e) {
            if (LOG.isLoggable(java.util.logging.Level.WARNING)) {
                LOG.log(Level.WARNING, "Failure with .jshrc ", e);
            }
        }
        finally {
            try {
                if (null != scriptReader) {
                    scriptReader.close();
                }
            }
            catch (IOException ignored) {
                //ignored
            }
        }
    }

    /**
     * Process the a single command
     *
     * @param cmd the command string.
     */
    private void processCmd(String cmd) {

        if (LOG.isLoggable(java.util.logging.Level.INFO)) {
            LOG.info("BEGINING OF COMMAND : " + cmd);
        }

        // get the args as a list of tokens
        List<String> args = new ArrayList<String>(Arrays.asList(tokenizeLine(cmd)));

        if (args.size() < 1) {
            return;
        }

        // Get the returnvar, if any.
        String returnvar = null;
        if (args.size() >= 2) {
            if ("=".equals(args.get(1))) {
                returnvar = args.remove(0);
                args.remove(0);
            }
        }

        String app = args.remove(0);

        // echo the command if the echo enviroment variable is defined
        if (getEnv().contains("echo")) {
            consoleMessage("Executing command : " + cmd);
        }

        // "clear" is an internal command; just handle it here, nothing to load.

        if (app.equals("clear")) {
            cons.clear();
            return;
        } else if (app.equals(cons.getCursorUpName())) {
            HistoryQueue queue = getHistoryQueue();
            if (queue != null) {
                cons.setCommandLine(queue.getNextCommand());
            }
            return;
        } else if (app.equals(cons.getCursorDownName())) {
            HistoryQueue queue = getHistoryQueue();
            if (queue != null) {
                cons.setCommandLine(queue.getPreviousCommand());
            }
            return;
        } else if (app.startsWith("!")) {
            try {
                int number = Integer.valueOf(app.substring(1));
                HistoryQueue queue = getHistoryQueue();
                if (queue != null) {
                    queue.removeLastCommand();
                    cons.setCommandLine(queue.getCommand(number));
                    return;
                }
            } catch (Exception iox) {
                // was not a history command, let the remainder of
                // the method handle the command
                /*
                *  If there are no commands starting with
                *  '!', then why not catch the exception,
                *  tell the user that there is nothing like this
                *  in the history, and return a null?
                */
            }
        }

        ShellApp appl = loadApp(returnvar, app, getEnv());

        if (null != appl) {
            exec(appl, args.toArray(new String[args.size()]));
        } else {
            consoleMessage("Could not load command '" + app + "'");
        }
    }

    /**
     * Process the <cmd>(";" <cmd>)* commands
     * <p/>
     * <p/>FIXME 20010611 bondolo@jxta.org does not handle quoting in any form.
     *
     * @param cmd the command string.
     */
    private void processMultipleCmd(String cmd) {

        HistoryQueue queue = getHistoryQueue();

        if (queue != null) {
            queue.addCommand(cmd);
        }

        StringTokenizer tokens = new StringTokenizer(cmd, ";");
        while (tokens.hasMoreElements()) {
            processPipeCmd(tokens.nextToken());
        }
    }

    /**
     * Process the <cmd> ("|" <cmd>)* commands
     * <p/>
     * <p/>FIXME 20010611 bondolo@jxta.org does not handle quoting in any form.
     *
     * @param cmd the command string.
     */
    private void processPipeCmd(String cmd) {

        List<String> cmds = new ArrayList<String>();

        StringTokenizer tokens = new StringTokenizer(cmd, "|");

        while (tokens.hasMoreElements()) {
            cmds.add(tokens.nextToken());
        }

        // at the beginning start with stdin and stdout

        PeerGroup current = (PeerGroup) getEnv().get("stdgroup").getObject();
        InputPipe stdin = (InputPipe) getEnv().get("stdin").getObject();
        OutputPipe stdout = (OutputPipe) getEnv().get("stdout").getObject();

        // these are for building the pipeline

        InputPipe pipein = null;
        OutputPipe pipeout = null;
        InputPipe lastin = stdin;
        Thread willDependOn = null;

        // The first and last command in the pipe needs to be treated separatly

        PipeService pipes = current.getPipeService();

        for (int i = 0; i < cmds.size() - 1; i++) {
            /*
             *  create Shell cmd pipe to link the two
             */
            PipeAdvertisement padv;

            try {
                padv = (PipeAdvertisement)
                        AdvertisementFactory.newAdvertisement(
                                PipeAdvertisement.getAdvertisementType());
                padv.setPipeID(IDFactory.newPipeID(current.getPeerGroupID()));
                padv.setType(PipeService.UnicastType);

                pipein = pipes.createInputPipe(padv);
                pipeout = pipes.createOutputPipe(padv, Collections.singleton(current.getPeerID()), 0);
            } catch (IOException ex) {
                printStackTrace("Could not construct pipes for piped command.", ex);
            }

            /*
            *  create the environment by cloning the parent.
            */
            ShellEnv pipeenv = new ShellEnv(getEnv());

            pipeenv.add("stdout", new ShellObject<OutputPipe>("Default OutputPipe", pipeout));

            pipeenv.add("stdin", new ShellObject<InputPipe>("Default InputPipe", lastin));

            /*
            *  create a new Shell process to run this pipe command
            */
            Shell pipeShell = (Shell) loadApp(null, "Shell", pipeenv);

            pipeShell.setJoinedThread(willDependOn);
            pipeShell.initPipe(cmds.get(i));
            willDependOn = pipeShell.thread;

            /*
            *  update last in pipe for the next command
            */
            lastin = pipein;
        }

        /*
        *  Set the pipeline for the last command and let it go/
        *  only stdin needs redirection since stdout is the right one
        */
        getEnv().add("stdout", new ShellObject<OutputPipe>("Default OutputPipe", stdout));

        ShellObject<InputPipe> oldin = (ShellObject<InputPipe>) getEnv().get("stdin");

        getEnv().add("stdin", new ShellObject<InputPipe>("Default InputPipe", lastin));

        setJoinedThread(willDependOn);

        processCmd(cmds.get(cmds.size() - 1));

        setJoinedThread(null);

        // restore the original stdin
        getEnv().add("stdin", oldin);
    }

    /**
     * This method implements the default input stream (keyboard).
     */
    private void runShell() {

        if (execShell || (scriptReader == null)) {
            consprintln("=============================================");
            consprintln("=======<[ Welcome to the JXTA Shell ]>=======");
            consprintln("=============================================");
            info();
        }

        // check if there is a .jshrc file
        if (execShell) {
            startupFile();
        }

        while (!stopped) {
            String cmd;

            try {
                if (scriptReader != null) {
                    cmd = scriptReader.readLine();
                } else {
                    cons.setPrompt(CMD_PROMPT);

                    cmd = waitForInput();
                }
            } catch (IOException e) {
                System.err.println("Shell is reconnecting to console");
                // This shell has lost its standard InputPipe. Try
                // to reconnect to the special keyboard InputPipe.
                setInputPipe(getInputConsPipe());
                continue;
            }

            if (cmd == null) {
                if (!stopped) {
                    exec(null, "exit", new String[0], getEnv());
                }
                break;
            }

            processMultipleCmd(cmd);
        }
    }

    /**
     * Return true if this is an embedded shell. IE used by an application that
     * wouldn't like it if System.exit were called.
     *
     * @return The embedded value
     * @author <a href="mailto:burton@openprivacy.org">Kevin A. Burton</a>
     */
    public static boolean isEmbedded() {

        String value = System.getProperty(JXTA_SHELL_EMBEDDED_KEY, "false");

        return Boolean.valueOf(value);
    }

    /**
     * converts a command line string into a series of tokens.
     */
    public String[] tokenizeLine(String line) {
        List<String> tokens = new ArrayList<String>();

        StringBuilder currentToken = new StringBuilder();
        int current = 0;
        int quote = -1;
        boolean escape = false;

        while (current < line.length()) {
            final char currentChar = line.charAt(current);

            if (escape) {
                currentToken.append(currentChar);
                escape = false;
            } else if (-1 != quote) {
                if (currentChar == quote) {
                    quote = -1;
                } else {
                    currentToken.append(currentChar);
                }
            } else {
                switch (currentChar) {
                    case' ':
                    case'\t':
                        if (currentToken.length() > 0) {
                            tokens.add(currentToken.toString());
                            currentToken.setLength(0);
                        }
                        break;

                    case'=':
                    case'|':
                    case';':
                        if (currentToken.length() > 0) {
                            tokens.add(currentToken.toString());
                            currentToken.setLength(0);
                        }
                        tokens.add(Character.toString(currentChar));
                        break;

                    case'"':
                    case'\'':
                        quote = currentChar;
                        break;

                    case'\\':
                        escape = true;
                        break;

                    default:
                        currentToken.append(currentChar);
                        break;
                }
            }

            current++;
        }

        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
            currentToken.setLength(0);
        }

        return tokens.toArray(new String[tokens.size()]);
    }
}
