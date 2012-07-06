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
 * $Id: GetOpt.java,v 1.11 2007/02/09 23:12:40 hamada Exp $
 */


package net.jxta.impl.shell;

/**
 * This class provides the functionality for parsing command line
 * arguments (similar to getopt(3C)).
 * After constructing an instance of it, getNextOption() can be used
 * to get the next option. getOptionArg() can be used to get the argument for
 * that option. getNextOptionIndex() returns how many arguments are already
 * processed from the arguments list.
 * <p/>
 * This class could be extended to handle the entire command line
 * instead of a pre-processed command line.  The problem with that
 * is handling pipes and redirects, but it would make options
 * parsing easier.
 *
 * @author Maneesha Jain
 */

public class GetOpt {

    private int optind = 0;
    private String optarg;
    private final String argv[];
    private int argc;
    private final String optionString;
    private boolean optionsDone = false;

    static int MINUS_POSITION = 0;
    static int OPTION_POSITION = 1;
    static int AFTER_OPTION_POSITION = 2;

    public GetOpt(String argv[], String optionString) {
        this(argv, 0, optionString);
    }

    /**
     * Constructor
     *
     * @param argv         -- Array of string arguments.
     * @param optionString --  contains the option letters that
     *                     will be recognized;
     *                     if a letter is followed by a colon,
     *                     the option is expected to have  an  argument.
     *                     if a letter is followed by a semi-colon,
     *                     the argument to the letter is optional.
     *                     e.g. abdf:e (legal arguments are a,b,d,f,e. f option requires a argument.
     */
    public GetOpt(String argv[], int start, String optionString) {
        this.argv = argv;
        optind = start;
        this.optionString = optionString;
        if (argv == null)
            this.argc = 0;
        else
            this.argc = argv.length;
    }

    /**
     * Returns the next valid option.
     * Throws an IllegalArgumentException
     * a) if option is not valid or
     * b) an option required an argument and is not provided
     * Returns -1 if no more options left.
     */
    public int getNextOption() throws IllegalArgumentException {

        char currentOption;

        optarg = null;

        // See if there are any more options left, if not return a -1.
        if (optind >= argc || (argv[optind].length() < 2) ||
                argv[optind].charAt(MINUS_POSITION) != '-') {
            optionsDone = true;
            return -1;
        }

        // So see if it is a legal option
        currentOption = argv[optind].charAt(OPTION_POSITION);
        if (!isValidOption(currentOption)) {
            optind++;
            throw new IllegalArgumentException("Illegal Option -- " + currentOption);
        }

        // see if it is the end of options. rest will be params
        if ('-' == currentOption) {
            optionsDone = true;
            optind++;
            return -1;
        }

        if (isOptionArgAllowedByOption(currentOption)) {
            if (argv[optind].length() != 2) {
                // arg is attached
                optarg = argv[optind].substring(AFTER_OPTION_POSITION);
            } else if (isOptionArgMandatoryByOption(currentOption)) {
                if ((optind + 1 >= argc) || "--".equals(argv[optind + 1]))
                    throw new IllegalArgumentException("Option requires an argument:" + currentOption);

                if ((optind + 1 < argc) && !"--".equals(argv[optind + 1])) {
                    optarg = argv[++optind];
                }
            }
            optind++;
        } else if (argv[optind].length() == 2) {
            optind++;

        } else {    // illegal argument supplied for option
            throw new IllegalArgumentException("Option -- " + currentOption + " does not take an argument");
        }

        return currentOption;
    }

    /**
     * Return the next parameter from the options string. When there are no
     * more parameters null is returned.
     */
    public String getNextParameter() {
        if (!optionsDone)
            throw new IllegalStateException("Options must all be processed before parameters");

        if (optind >= argc)
            return null;
        else
            return argv[optind++];
    }

    /**
     * Returns the argument for the option being handled.
     */
    public String getOptionArg() {
        return optarg;
    }

    /**
     * Returns how many arguments are already processed by the getNextOption()
     * function. The other way to look at it is what argument is going to be
     * processed by getNextOption() method next.
     */
    public int getNextOptionIndex() {
        return optind;
    }

    /**
     * Returns true if option is a valid option
     *
     * @param c The character to check.
     * @return boolean true if the option is a valid option otherwise false.
     */
    private boolean isValidOption(char c) {
        return (c == '-') ||
                ((c != ':') && (c != ';') && (optionString.indexOf(c) != -1));
    }

    /**
     * Returns true if option provided allows a argument.
     *
     * @param option The option to check.
     * @return boolean true if the option has an arg otherwise false
     */
    private boolean isOptionArgAllowedByOption(char option) {
        int optionIdx = optionString.indexOf(option);

        return isValidOption(option) &&
                (optionString.length() > optionIdx + 1) &&
                ((optionString.charAt(optionIdx + 1) == ':') || (optionString.charAt(optionIdx + 1) == ';'));
    }

    /**
     * Returns true if option provided needs a argument.
     *
     * @param option The option to check.
     * @return boolean true if the option must have an arg otherwise false.
     */
    private boolean isOptionArgMandatoryByOption(char option) {
        int optionIdx = optionString.indexOf(option);

        return isValidOption(option) &&
                (optionString.length() > optionIdx + 1) &&
                (optionString.charAt(optionIdx + 1) == ':');
    }
}
