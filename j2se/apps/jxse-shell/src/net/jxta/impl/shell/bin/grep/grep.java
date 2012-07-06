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
 * $Id: grep.java,v 1.6 2007/02/09 23:12:50 hamada Exp $
 */
package net.jxta.impl.shell.bin.grep;

import net.jxta.document.Advertisement;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * grep
 *
 * @version $Revision: 1.6 $
 * @since JXTA 1.0
 */

public class grep extends ShellApp {

    protected ShellEnv env;
    protected boolean countOnly;
    protected boolean ignoreCase;
    protected boolean showLineNums;
    protected boolean invertMatch;
    protected String pattern;


    public grep() {
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
                return syntaxError();
            } else {
                // Check for command-line option flags.
                GetOpt getopt = new GetOpt(args, "cinv");
                int c;
                try {
                    while ((c = getopt.getNextOption()) != -1) {
                        switch (c) {
                            case'c':
                                countOnly = true;
                                break;
                            case'i':
                                ignoreCase = true;
                                break;
                            case'n':
                                showLineNums = true;
                                break;
                            case'v':
                                invertMatch = true;
                                break;
                            default:
                                println("Error: option not supported.");
                                return ShellApp.appParamError;
                        }
                    }
                } catch (Exception ex) {
                    return syntaxError();
                }

                // Get the searchPattern, which should be the next command-line
                // arg after the option flags.
                int pos = getopt.getNextOptionIndex();
                if (pos < args.length)
                    pattern = args[pos];
                else
                    return syntaxError();

                // Get the shellObject specified on the command-line, if any.
                if (++pos < args.length) {
                    name = args[pos];
                    obj = env.get(name);
                    if (obj == null) {
                        println("grep: cannot access " + name);
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
                        println("grep: exception reading Advertisement. " + e.toString());
                        return ShellApp.appMiscError;
                    }
                }

                // Is it a Document?
                if (Document.class.isInstance(objContent)) {
                    try {
                        readDocument((Document) objContent);
                        return ShellApp.appNoError;
                    } catch (Exception e) {
                        println("grep: exception reading Document. " + e.toString());
                        return ShellApp.appMiscError;
                    }
                }

                println("grep: cannot read this kind of object.");
                return ShellApp.appMiscError;
            }
        } catch (Exception ex) {
            println("grep: exception " + ex.toString());
            ex.printStackTrace();
            return ShellApp.appMiscError;
        }
    }


    protected int syntaxError() {

        println("Usage: " + syntax());
        return ShellApp.appParamError;
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
        grepData(inData);
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
        grepData(out.toString());
    }


    protected void grepData(String inData) {

        boolean match;
        int matchCount = 0;
        int lineNum = 0;

        // If ignoring case, we'll do our searches in lower case.
        if (ignoreCase)
            pattern = pattern.toLowerCase();

        // Break up the input data into lines.  We tell the tokenizer to give
        // us the delimiters, otherwise it passes over blank lines.
        String delim = "\n";
        StringTokenizer tokens = new StringTokenizer(inData, delim, true);
        String line;
        String prevLine = delim;
        String compLine;

        try {
            while (tokens.hasMoreTokens()) {
                match = false;
                line = tokens.nextToken();

                // We include blank lines, to keep our line numbers correct.
                // Plus, we may need to print it, if "invertMatch" is selected.
                // If this token and the previous one were newline chars, then
                // we've found a blank line, and we'll process it.
                // Otherwise, it's just the newline at the end of a text line.
                //
                if (line.equals(delim) && !prevLine.equals(delim)) {
                    prevLine = line;
                    continue;
                }

                lineNum++;
                prevLine = line;

                if (ignoreCase)
                    compLine = line.toLowerCase();
                else
                    compLine = line;

                if (compLine.indexOf(pattern) >= 0) {
                    match = true;
                    matchCount++;
                }

                if (!countOnly) {
                    if ((match && !invertMatch) ||
                            (!match && invertMatch)) {
                        if (showLineNums)
                            print(String.valueOf(lineNum) + ":");

                        if (line.equals(delim))
                            println("");
                        else
                            println(line);
                    }
                }
            }
        } catch (Exception ex) {
            //ignored
        }

        if (countOnly) {
            if (invertMatch)
                println(String.valueOf(lineNum - matchCount));
            else
                println(String.valueOf(matchCount));
        }
    }


    protected String syntax() {
        return "grep [-c -i -n -v] searchPattern [<objectName>]";
    }

    @Override
    public String getDescription() {
        return "Search for matching patterns";
    }

    @Override
    public void help() {
        println("NAME");
        println("     grep  - search for matching patterns.");
        println("");
        println("SYNOPSIS");
        println("");
        println("     " + syntax());
        println("");
        println("DESCRIPTION");
        println("");
        println("'grep' searches the named shell object for lines containing a");
        println("match to the given search pattern.  Matching lines are written");
        println("to output.  Regular expressions are not currently supported.");
        println("");
        println("If no object is specifed on the command line, grep will read from");
        println("stdin.  If stdin is the console, you may type in lines of text.");
        println("Enter a '.' all by itself at the beginning of a line to finish.");
        println("");
        println("OPTIONS");
        println("");
        println("  -c   Just print count of matching lines. With -v, count non-matching.");
        println("  -i   Ignore case when comparing text with search pattern.");
        println("  -n   Show line numbers of matches found.");
        println("  -v   Invert the sense of matching, to select non-matching lines.");
        println("");
        println("EXAMPLES");
        println("");
        println("    JXTA>grep -i -n dog GroceryList");
        println("    3:Hot dogs");
        println("    7:Dog food");
        println("");
        println("    JXTA>cat GroceryList | grep -c dog");
        println("    2");
        println("");
    }
}
