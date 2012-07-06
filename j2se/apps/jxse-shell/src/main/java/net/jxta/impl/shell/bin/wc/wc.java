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
 * $Id: wc.java,v 1.7 2007/02/09 23:12:53 hamada Exp $
 */
package net.jxta.impl.shell.bin.wc;

import net.jxta.document.Advertisement;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * wc
 *
 */
public class wc extends ShellApp {

    protected ShellEnv env;

    protected boolean showLines;
    protected boolean showWords;
    protected boolean showChars;

    public wc() {
    }

    @Override
    public void stopApp() {
    }

    public int startApp(String[] args) {

        try {
            ShellObject obj;
            String name;
            env = getEnv();

            if (args == null) {
                // By default, we show all three counts.
                showLines = showWords = showChars = true;
                readStdin();
                return ShellApp.appNoError;
            } else {
                // Check for command-line option flags.
                showLines = showWords = showChars = false;
                GetOpt getopt = new GetOpt(args, "clw");
                int c;
                try {
                    while ((c = getopt.getNextOption()) != -1) {
                        switch (c) {
                            case'c':
                                showChars = true;
                                break;
                            case'l':
                                showLines = true;
                                break;
                            case'w':
                                showWords = true;
                                break;
                            default:
                                println("Error: option not supported.");
                                return ShellApp.appParamError;
                        }
                    }
                }
                catch (Exception ex) {
                    println("Usage: " + syntax());
                    return ShellApp.appParamError;
                }

                // Get the ShellObject name, which should be the next command-line
                // arg after the option flags.  If there were no option flags
                // then we show all three counts.
                int pos = getopt.getNextOptionIndex();
                if (pos == 0)
                    showLines = showWords = showChars = true;
                if (pos < args.length) {
                    name = args[pos];
                    obj = env.get(name);
                    if (obj == null) {
                        println("wc: cannot access " + name);
                        return ShellApp.appMiscError;
                    }
                } else {
                    // There's no ShellObject specified on the command-line,
                    // so read from stdin.
                    readStdin();
                    return ShellApp.appNoError;
                }

                // Get the object contained by the environment object.
                Object objContent = obj.getObject();

                // Is it an Advertisement?
                if (Advertisement.class.isInstance(objContent)) {
                    try {
                        readAdvertisement((Advertisement) objContent);
                        return ShellApp.appNoError;
                    } catch (Exception e) {
                        println("wc: exception reading Advertisement. " + e.toString());
                        return ShellApp.appMiscError;
                    }
                }

                // Is it a Document?
                if (Document.class.isInstance(objContent)) {
                    try {
                        readDocument((Document) objContent);
                        return ShellApp.appNoError;
                    } catch (Exception e) {
                        println("wc: exception reading Document. " + e.toString());
                        return ShellApp.appMiscError;
                    }
                }

                println("wc: cannot read this kind of object.");
                return ShellApp.appMiscError;
            }
        } catch (Exception ex) {
            println("wc: exception " + ex.toString());
            ex.printStackTrace();
            return ShellApp.appMiscError;
        }
    }


    protected void readStdin() throws IOException {

        String inData;

        // Get the data from stdin.  The input method is different if it's
        // from the console, since we need to give the user a way to 
        // terminate multi-line input.
        //
        // If the ShellApp's current input pipe is the same as its console
        // input pipe, then we're reading user data entry from the console.
        //
        if (getInputPipe() == getInputConsPipe())
            inData = readStdinConsole();
        else
            inData = readStdinPipe();

        // Process the data we've collected.
        wcCount(inData);
    }


    protected String readStdinConsole() throws IOException {

        // We're reading user input from the console: accept lines of
        // input until user types a period alone on a line.
        // Would prefer to check for CTRL-D, but it's not showing up in
        // the input.  This code is probably temporary, until we come up
        // with a system standard way of terminating user input.
        //
        String inData = "";
        String moreData;
        while (true) {
            moreData = waitForInput();
            if (moreData == null)
                break;
            if (moreData.equals("."))
                break;

            inData += moreData + "\n";
        }
        return inData;
    }


    protected String readStdinPipe() throws IOException {

        // Accept data from the input pipe.
        // Build a string from the input data until there's no more.
        String inData = waitForInput();
        String moreData;
        while (true) {
            moreData = pollInput();
            if (moreData == null)
                break;
            inData += moreData;
        }

        return inData;
    }


    protected void readAdvertisement(Advertisement adv) throws Exception {
        Document doc = adv.getDocument(new MimeMediaType("text/xml"));
        readDocument(doc);
    }


    protected void readDocument(Document doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.sendToStream(out);
        wcCount(out.toString());
    }


    protected void wcCount(String inData) {
        int numLines = 0;
        int numWords = 0;
        int numChars = inData.length();
        char c;
        boolean newWord = true;

        for (int i = 0; i < inData.length(); i++) {
            c = inData.charAt(i);
            if (c == '\n') {
                numLines++;
                newWord = true;
            } else if (Character.isWhitespace(c))
                newWord = true;
            else if (newWord) {
                newWord = false;
                numWords++;
            }
        }

        // Display the counts that were requested.
        // To mimic the Unix wc command, we right-justify each count.
        // The first count is justified to the 7th column, the second count to
        // the 15th column, and the third to the 23rd column.
        //
        String lineStr = String.valueOf(numLines);
        String wordStr = String.valueOf(numWords);
        String charStr = String.valueOf(numChars);
        String spaces = "        ";  // Eight spaces.
        String result = "";
        int justOff = 6;           // Justification offset.

        if (showLines) {
            result += spaces.substring(0, justOff - lineStr.length()) + lineStr;
            justOff = 7;
        }
        if (showWords) {
            result += spaces.substring(0, justOff - wordStr.length()) + wordStr;
            justOff = 7;
        }
        if (showChars) {
            result += spaces.substring(0, justOff - charStr.length()) + charStr;
        }

        println(result);
    }


    protected String syntax() {
        return "wc [-c -l -w] [<objectName>]";
    }

    @Override
    public String getDescription() {
        return "Count the number of lines, words, and chars in an object";
    }

    @Override
    public void help() {
        println("NAME");
        println("     wc  - count the number of lines, words, and chars in an object");
        println("");
        println("SYNOPSIS");
        println("");
        println("     " + syntax());
        println("");
        println("DESCRIPTION");
        println("");
        println("'wc' counts the number of newlines, whitespace-separated words, ");
        println("and characters in the given shell object, or in the standard");
        println("input pipe if no object is specified.  It writes one line of");
        println("counts to the output pipe.  The counts are written in the");
        println("order: lines, words, characters.");
        println("");
        println("By default, wc writes all three counts.  Options can specify");
        println("that only certain counts be written.  Options do not undo");
        println("others previously given, so 'wc -c -l' writes both the");
        println("character count and the line count.");
        println("");
        println("If no object is specifed on the command line, wc will read from");
        println("stdin.  If stdin is the console, you may type in lines of text.");
        println("Enter a '.' all by itself at the beginning of a line to finish.");
        println("");
        println("OPTIONS");
        println("");
        println("  -c   Write the character count.");
        println("  -l   Write the line count.");
        println("  -w   Write the word count.");
        println("");
        println("EXAMPLES");
        println("");
        println("    JXTA>wc myfile");
        println("         18      52     675");
        println("");
        println("    JXTA>cat myfile | wc -w");
        println("         52");
        println("");
        println("The first example displays the number of lines, words, and chars");
        println("in myfile.  The second displays only the number of lines.");
        println("");
    }
}
