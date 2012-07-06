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
 * $Id: ShellApp.java,v 1.44 2007/02/09 23:12:41 hamada Exp $
 */


package net.jxta.impl.shell;

import net.jxta.document.Advertisement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.endpoint.TextMessageElement;
import net.jxta.id.ID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.platform.Application;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the base class any JXTA Shell application must extend.
 */
public abstract class ShellApp implements Application {

    /**
     * The command is still running.
     */
    public final static int appSpawned = -1;

    /**
     * The command completed successfully.
     */
    public final static int appNoError = 0;

    /**
     * An error occurred resulting from incorrect or missing parameters.
     */
    public final static int appParamError = 1;

    /**
     * Something bad happened. Don't know what it means, don't care why it
     * happened.
     */
    public final static int appMiscError = Integer.MAX_VALUE;

    /**
     * The default peergroup associated with this app.
     */
    private PeerGroup group = null;

    /**
     * The assigned id for this application, if any.
     */
    private ID id = null;

    /**
     * The implementation advertisement for this application, if any.
     */
    private Advertisement implAdv = null;

    /**
     * The environment for this application. Copied from the host shell.
     */
    private ShellEnv env = null;

    /**
     * The "stdin" input pipe for this command.
     */
    private InputPipe inputPipe = null;

    /**
     * The "stdout" output pipe for this command.
     */
    private OutputPipe outputPipe = null;

    /**
     * The console input. Differs from the stdin if stdin has been
     * redirected. consin is not normally redirected.
     */
    private InputPipe consin = null;

    /**
     * The console output. Differs from the stdout if stdout has been
     * redirected. consout is not normally redirected.
     */
    private OutputPipe consout = null;

    /**
     * if the result is a shell object then store it using this name.
     */
    private String returnVarName = null;

    /**
     * Has this app begun running?
     */
    protected volatile boolean started = false;

    /**
     * Has this app in the process of quitting?
     */
    protected volatile boolean stopped = false;

    /**
     * If this thread is enabled, then this is the root thread of another
     * command which this command can use to determine when it should finish.
     */
    private Thread dependsOn = null;

    /**
     * private buffer for input pipe
     */
    private List<String> buffered = new ArrayList<String>();

    /**
     * private buffer for console input pipe.
     */
    private List<String> consbuffer = new ArrayList<String>();

    /**
     * {@inheritDoc}
     */
    public final void init(PeerGroup pg, ID assignedID, Advertisement impl) {
        setGroup(pg);
        setAssignedID(assignedID);
        setImplAdvertisement(impl);

        started = true;
    }

    /**
     * {@inheritDoc}
     */
    public void stopApp() {
        stopped = true;
    }

    /**
     * Return a String containing a single line description of the function of
     * this ShellApp.
     */
    public String getDescription() {
        return "No description available for this ShellApp";
    }

    /**
     * Print to the stdout a full description of the functionality of this
     * Shell command.
     */
    public void help() {
        println("No help available for this ShellApp");
    }

    // Accessor Methods

    /**
     * Return the group in which this ShellApp is executing. Most ShellApps
     * should instead use the value of the "stdgroup" environment variable.
     *
     * @return the current peer group.
     */
    protected PeerGroup getGroup() {
        return group;
    }

    /**
     * Set the group will be the default group for this ShellApp. Most ShellApps
     * should instead use the value of the "stdgroup" environment variable.
     */
    private PeerGroup setGroup(PeerGroup g) {
        PeerGroup old = group;
        group = g;
        return old;
    }

    /**
     * Return the assignedID for this ShellApp if any.
     *
     * @return the assigned ID for this ShellApp or <code>null</code> if none
     *         was specified.
     */
    protected final ID getAssignedID() {
        return this.id;
    }


    /**
     * Set the assignedID for this ShellApp
     *
     * @param id The assigned ID for this application
     */
    private void setAssignedID(ID id) {
        this.id = id;
    }

    /**
     * Return the implementation advertisement for this ShellApp, if any.
     *
     * @return implementation Advertisement for this ShellApp or <code>null</code>
     *         if none was specified.
     */
    protected final Advertisement getImplAdvertisement() {
        return this.implAdv;
    }

    /**
     * Sets the implementation advertisement for this ShellApp.
     *
     * @param adv The implementation advertisement for this ShellApp.
     */
    private Advertisement setImplAdvertisement(Advertisement adv) {
        Advertisement old = this.implAdv;
        this.implAdv = adv;
        return old;
    }

    /**
     * Return a modifiable instance of this ShellApp's environment.
     *
     * @return This ShellApp's environment.
     */
    protected final ShellEnv getEnv() {
        return env;
    }

    /**
     * Sets this ShellApp's environment.
     *
     * @return The environment for this ShellApp to use.
     */
    protected final ShellEnv setEnv(ShellEnv e) {
        ShellEnv old = env;
        env = e;
        return old;
    }

    protected final Thread setJoinedThread(Thread dependsOn) {
        Thread old = this.dependsOn;
        this.dependsOn = dependsOn;
        return old;
    }

    /**
     * Return the standard input pipe.
     *
     * @return the standard input pipe.
     */
    protected final InputPipe getInputPipe() {
        return inputPipe;
    }

    /**
     * Set the standard input pipe to the provided pipe. The previous pipe is
     * returned.
     *
     * @param ip the new standard input pipe.
     * @return the old standard input pipe.
     */
    protected final InputPipe setInputPipe(InputPipe ip) {
        InputPipe old = inputPipe;

        inputPipe = ip;

        buffered.clear();

        return old;
    }

    /**
     * Return the standard output pipe.
     *
     * @return the standard output pipe.
     */
    protected final OutputPipe getOutputPipe() {
        return outputPipe;
    }

    /**
     * Set the standard output pipe to the provided pipe. The previous pipe is
     * returned.
     *
     * @param op the new standard output pipe.
     * @return the old standard output pipe.
     */
    protected final OutputPipe setOutputPipe(OutputPipe op) {
        OutputPipe old = outputPipe;
        outputPipe = op;
        return old;
    }

    /**
     * Return the console input pipe.
     *
     * @return the console input pipe.
     */
    protected final InputPipe getInputConsPipe() {
        return consin;
    }

    /**
     * Set the console input pipe to the provided pipe. The previous pipe is
     * returned.
     *
     * @param ip the new input output pipe.
     * @return the old input output pipe.
     */
    protected final InputPipe setInputConsPipe(InputPipe ip) {
        InputPipe old = consin;

        consin = ip;

        consbuffer.clear();

        return old;
    }

    /**
     * Return the console output pipe.
     *
     * @return the console output pipe.
     */
    protected final OutputPipe getOutputConsPipe() {
        return consout;
    }

    /**
     * Set the console output pipe to the provided pipe. The previous pipe is
     * returned.
     *
     * @param op the new console output pipe.
     * @return the old console output pipe.
     */
    protected final OutputPipe setOutputConsPipe(OutputPipe op) {
        OutputPipe old = consout;
        consout = op;
        return old;
    }

    /**
     * Return the name of the environment into which this ShellApp should
     * return any environment variable result.
     *
     * @return the name of the environment variable or null if no name has been
     *         set.
     */
    protected final String getReturnVariable() {
        return returnVarName;
    }

    /**
     * Set the name of the environment into which this ShellApp should
     * return any environment variable result.
     *
     * @param varName name of the environment variable.
     */
    protected final void setReturnVariable(String varName) {
        returnVarName = varName;
    }

    // IO Operations

    /**
     * Print to standard output
     *
     * @param line the line to print
     */
    protected final void print(String line) {
        if (null == outputPipe) {
            return;
        }

        pipePrint(outputPipe, line);
    }

    /**
     * Print to standard output appending a newline.
     *
     * @param line the line to print
     */
    protected final void println(String line) {
        if (null == outputPipe) {
            return;
        }

        pipePrintln(outputPipe, line);
    }

    /**
     * Poll for input on standard input.
     *
     * @return an input line or <code>null</code> if no input is available.
     */
    protected final String pollInput() throws IOException {
        if (null == inputPipe) {
            return null;
        }

        if (inputPipe == consin)
            return consPollInput();
        else
            return pipePollInput(inputPipe, buffered);
    }

    /**
     * Wait for input on standard input.
     *
     * @return an input line or <code>null</code> if standard input has been
     *         closed.
     */
    protected final String waitForInput() throws IOException {
        if (null == inputPipe) {
            return null;
        }

        if (inputPipe == consin)
            return consWaitForInput();
        else
            return pipeWaitForInput(inputPipe, buffered, true);
    }

    /**
     * Print to the console output.
     *
     * @param line the line to print
     */
    protected final void consprint(String line) {
        if (null == consout) {
            return;
        }

        pipePrint(consout, line);
    }

    /**
     * Print to console output appending a newline.
     *
     * @param line the line to print
     */
    protected final void consprintln(String line) {
        if (null == consout) {
            return;
        }

        pipePrintln(consout, line);
    }

    /**
     * Poll for input on console input.
     *
     * @return an input line or <code>null</code>  if no input is available.
     */
    protected final String consPollInput() throws IOException {
        if (null == consin) {
            return null;
        }

        return pipePollInput(consin, consbuffer);
    }

    /**
     * Wait for input on console input.
     *
     * @return an input line or <code>null</code> if standard input has been
     *         closed.
     */
    protected final String consWaitForInput() throws IOException {
        if (null == consin) {
            return null;
        }

        String msg = pipeWaitForInput(consin, consbuffer, false);

        // create an EOT
        if (msg != null) {
            if (-1 != msg.indexOf('\u0004')) {
                msg = null;
            }
        }

        return msg;
    }

    protected final String getCmdShortName() {
        return getCmdShortName(this.getClass());
    }

    public static String getCmdShortName(Class clas) {
        String cmdClass = clas.getName();
        String cmdName;

        int lastDot = cmdClass.lastIndexOf(".");

        if (-1 != lastDot) {
            int secondLast = cmdClass.lastIndexOf(".", lastDot - 1);

            String cmdPackage = "";
            if (-1 != secondLast) {
                cmdPackage = cmdClass.substring(secondLast + 1, lastDot);
            }
            cmdName = cmdClass.substring(lastDot + 1);

            if (!cmdName.equals(cmdPackage)) {
                cmdName = cmdPackage + "." + cmdName;
            }
        } else {
            cmdName = cmdClass;
        }

        return cmdName;
    }

    /**
     * print a message from the specified class on the specified pipe.
     *
     * @param clas    the class which is printing the message
     * @param consout the pipe on which to print the message
     * @param message the message to print.
     */
    public static void consoleMessage(Class clas, OutputPipe consout, String message) {

        pipePrintln(consout, "# " + getCmdShortName(clas) + " - " + message);
    }

    /**
     * Print a message on the console. The message will identify this ShellApp
     * as the source of the message.
     *
     * @param message The message to print.
     */
    protected final void consoleMessage(String message) {
        consoleMessage(getClass(), consout, message);
    }

    /**
     * Print a stack trace to the console with the specified annotation. The
     * message will identify this ShellApp as the source of the exception.
     *
     * @param clas       the class which is printing the message
     * @param consout    the pipe on which to print the message
     * @param annotation Explanation or annotation for the stack trace.
     * @param failure    the stack trace.
     */
    public static void printStackTrace(Class clas, OutputPipe consout, String annotation, Throwable failure) {
        consoleMessage(clas, consout, annotation);

        StringWriter theStackTrace = new StringWriter();
        failure.printStackTrace(new PrintWriter(theStackTrace));
        pipePrintln(consout, theStackTrace.toString());
    }

    /**
     * Print a stack trace to the console with the specified annotation. The
     * message will identify this ShellApp as the source of the exception.
     *
     * @param annotation Explanation or annotation for the stack trace.
     * @param failure    the stack trace.
     */
    protected final void printStackTrace(String annotation, Throwable failure) {
        printStackTrace(getClass(), consout, annotation, failure);
    }

    /**
     * Load a shell application.
     *
     * @param returnvar The env variable in which the command should put its result.
     * @param appName   The name of the application to load.
     * @param env       The enviroment to use.
     */
    protected ShellApp loadApp(String returnvar, String appName, ShellEnv env) {
        ShellApp app;

        try {
            app = new ShellCmds(env).getInstance(appName);
        } catch (Exception failed) {
            printStackTrace("Exception in command : " + appName, failed);
            return null;
        }

        if (null == app) {
            return null;
        }

        // Set up the default environment and pipes

        app.setEnv(env);

        app.setJoinedThread(dependsOn);

        ShellObject obj = env.get("consin");
        if (null != obj) {
            app.setInputConsPipe((InputPipe) obj.getObject());
        }

        obj = env.get("consout");
        if (null != obj) {
            app.setOutputConsPipe((OutputPipe) obj.getObject());
        }

        obj = env.get("stdin");
        if (null != obj) {
            app.setInputPipe((InputPipe) obj.getObject());
        }

        obj = env.get("stdout");
        if (null != obj) {
            app.setOutputPipe((OutputPipe) obj.getObject());
        }

        // Set the variable name for the return value (if any).
        app.setReturnVariable(returnvar);

        // now init it!
        app.init((PeerGroup) env.get("stdgroup").getObject(), null, null);

        return app;
    }

    /**
     * Load and run a shell application.
     *
     * @param returnvar The env variable in which the command should put its result.
     * @param appName   The name of the application to load.
     * @param env       The enviroment to use.
     */
    protected int exec(String returnvar, String appName, String[] args, ShellEnv env) {
        ShellApp app = loadApp(returnvar, appName, env);

        if (null == app) {
            consoleMessage("Could not load application : " + appName);
            return ShellApp.appMiscError;
        }

        return exec(app, args);
    }

    /**
     * Run a loaded shell Application.
     *
     * @param app  the application
     * @param args arguments for the application.
     */
    protected int exec(ShellApp app, String[] args) {
        try {
            // start the app
            int result = app.startApp(args);

            if (ShellApp.appSpawned != result) {
                if ((ShellApp.appNoError != result) && (getEnv().contains("echo"))) {
                    consoleMessage("'" + app + "' returned error code : " + result);
                }

                app.stopApp();
            }

            return result;
        } catch (Throwable e) {
            printStackTrace("Exception in command : " + app, e);
            return ShellApp.appMiscError;
        }
    }

    // Private implementations

    /**
     * print to the specified pipe.
     *
     * @param pipe The destination pipe.
     * @param line The text to print.
     */
    public static void pipePrint(OutputPipe pipe, String line) {
        if (null == pipe) {
            return;
        }

        //Create a message off this string.
        try {
            Message msg = new Message();

            MessageElement elem = new StringMessageElement("ShellOutputPipe", line, null);

            msg.addMessageElement(elem);

            pipe.send(msg);
        } catch (IOException failure) {
            failure.printStackTrace();
        }
    }

    /**
     * print to the specified pipe appending a newline after text.
     *
     * @param pipe The destination pipe.
     * @param line The text to print.
     */
    public static void pipePrintln(OutputPipe pipe, String line) {
        pipePrint(pipe, line + "\n");
    }

    /*
    *  Return a string containing one line of input from the specified pipe.
    *
    *  @param input the pipe to poll.
    *  @param lineBuffer a vector containing buffered lines associated with
    *  this pipe. If not present then it is VERY LIKELY YOU WILL LOSE MESSAGES.
    *  @return String containing the new line or null if no line was available.
    *  @throws IOException if the input pipe somehow becomes uninitialized.
    */
    protected String pipePollInput(InputPipe input, List<String> lineBuffer) throws IOException {
        boolean onePoll = false;

        do {
            if (null != lineBuffer) {
                // Check in the buffer first
                synchronized (lineBuffer) {
                    if (!lineBuffer.isEmpty()) {
                        try {
                            return lineBuffer.remove(0);
                        } catch (Exception e) {
                            // This is a very strange case, but if that happens, let just
                            // wait for a message on the InputPipe.
                        }
                    }
                }
            }

            if (input == null) {
                throw new IOException("Input pipe null");
            }

            if (onePoll) {
                return null;
            }

            Message msg = null;

            try {
                msg = input.poll(1000);
            } catch (InterruptedException woken) {
                Thread.interrupted();
            }

            // no message was waiting.
            if (null == msg) {
                return null;
            }

            onePoll = true;

            // Get all the chunks of data in the message
            Iterator<MessageElement> elems = msg.getMessageElementsOfNamespace(null);
            while (elems.hasNext()) {
                Reader plainReader = null;
                BufferedReader inputReader = null;
                try {
                    MessageElement elem = elems.next();
                    elems.remove();
                    String name = elem.getElementName();

                    // because of pipe redirection we accept both input and
                    // and output elements.
                    if (!name.equals("ShellInputPipe") && !name.equals("ShellOutputPipe")) {
                        continue;
                    }

                    if (elem instanceof TextMessageElement) {
                        plainReader = ((TextMessageElement) elem).getReader();
                    } else {
                        InputStream inputStream = elem.getStream();
                        plainReader = new InputStreamReader(inputStream);
                    }

                    inputReader = new BufferedReader(plainReader);

                    do {
                        String command = inputReader.readLine();
                        if (null != command)
                            if (null != lineBuffer)
                                synchronized (lineBuffer) {
                                    lineBuffer.add(command);
                                }
                            else
                                return command;
                        else
                            break;
                    } while (true);
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (null != plainReader) {
                            plainReader.close();
                        }
                    } catch (IOException ignored) {
                        //ignored
                    }

                    try {
                        if (null != inputReader) {
                            inputReader.close();
                        }
                    } catch (IOException ignored) {
                        //ignored
                    }
                }
            }
            msg.clear();
        } while (true);
    }

    private String pipeWaitForInput(InputPipe input, List<String> lineBuffer, boolean join) throws IOException {
        String msg;

        while (true) {
            msg = pipePollInput(input, lineBuffer);

            if ((null != msg) || stopped)
                break;

            if (join && ((null == dependsOn) || (!dependsOn.isAlive())))
                break;
        }

        return msg;
    }
}
