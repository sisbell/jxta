/*
 *  Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 *  reserved.
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
 *  $Id: logging.java,v 1.3 2007/02/09 23:12:47 hamada Exp $
 */
package net.jxta.impl.shell.bin.logging;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;

import java.util.logging.Level;

/**
 * Adjust logging levels
 */
public class logging extends ShellApp {

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] args) {
        boolean severe = false;
        boolean warn = false;
        boolean config = false;
        boolean info = false;
        boolean fine = false;
        boolean finer = false;
        boolean finest = false;
        boolean disabled = false;

        Level level = null;
        GetOpt options = new GetOpt(args, 0, "swciftvd");

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
                case's':
                    severe = true;
                    break;
                case'w':
                    warn = true;
                    break;
                case'c':
                    config = true;
                    break;
                case'i':
                    info = true;
                    break;
                case'f':
                    fine = true;
                    break;
                case't':
                    finer = true;
                    break;
                case'v':
                    finest = true;
                    break;
                case'd':
                    disabled = true;
                    break;
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }
        
        if (severe && (warn || config || info || fine || finer || finest || disabled)) {
            consoleMessage("Cannot set more than one level");
            return syntaxError();
        }

        if (warn && (severe || config || info || fine || finer || finest || disabled)) {
            consoleMessage("Cannot set more than one level");
            return syntaxError();
        }

        if (config && (severe || warn || info || fine || finer || finest || disabled)) {
            consoleMessage("Cannot set more than one level");
            return syntaxError();
        }

        if (info && (severe || warn || config || fine || finer || finest || disabled)) {
            consoleMessage("Cannot set more than one level");
            return syntaxError();
        }

        if (fine && (severe || warn || config || info || finer || finest || disabled)) {
            consoleMessage("Cannot set more than one level");
            return syntaxError();
        }

        if (finer && (severe || warn || config || info || fine || finest || disabled)) {
            consoleMessage("Cannot set more than one level");
            return syntaxError();
        }

        if (finest && (severe || warn || config || info || fine || finer || disabled)) {
            consoleMessage("Cannot set more than one level");
            return syntaxError();
        }

       if (disabled && (severe || warn || config || info || fine || finer || finest)) {
            consoleMessage("Cannot set more than one level");
            return syntaxError();
        }

        if (severe) {
            level = Level.SEVERE;
        }

        if (warn) {
            level = Level.WARNING;
        }

        if (config) {
            level = Level.CONFIG;
        }

        if (info) {
            level = Level.INFO;
        }

        if (fine) {
            level = Level.FINE;
        }

        if (finer) {
            level = Level.FINER;
        }

        if (finest) {
            level = Level.FINEST;
        }

        if (disabled) {
            level = Level.OFF;
        }

        String name = options.getNextParameter();

        if (null == name) {
            consoleMessage("Missing <logger> parameter.");
            return syntaxError();
        }
        
        do {
            java.util.logging.Logger jxtaLogger = java.util.logging.Logger.getLogger(name);

            if (null == jxtaLogger) {
                consoleMessage("Invalid logger name : " + name);
                return ShellApp.appMiscError;
            }
            
            Level currentLevel = null;

            while ((null == currentLevel) && (null != jxtaLogger)) {
                currentLevel = jxtaLogger.getLevel();

                jxtaLogger = jxtaLogger.getParent();
            }

            if (null == currentLevel) {
                currentLevel = Level.OFF;
            }

            jxtaLogger = java.util.logging.Logger.getLogger(name);

            print(currentLevel.toString());
            if (null != level) {
                jxtaLogger.setLevel(level);
                print("->" + level);
            }
            println(" : " + name);
            name = options.getNextParameter();
        } while (null != name);
        
        return ShellApp.appNoError;
    }

    private int syntaxError() {
        consoleMessage("usage : logging [-s | -w | -c | -i | -f | -t | -v | -d ] <logger> ... ");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display and optionally adjust logging levels";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     logging - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println("    logging [-s | -w | -c | -i | -f | -t | -v | -d ] <logger> ... ");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("Use 'logging' to adjust logging levels.");
        println(" ");
        println("OPTIONS");
        println("    -s           Severe logging level.");
        println("    -w           Warning logging level.");
        println("    -c           Config logging level.");
        println("    -i           Info logging level.");
        println("    -f           Fine logging level.");
        println("    -t           Finer (trace) logging level.");
        println("    -v           Finest (verbose) logging level.");        
        println("    -d           Disabled logging level.");
        println(" ");
        println("PARAMETERS");
        println("    <env>    One or more logger names.");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>logging -f net.jxta.endpoint net.jxta.impl.resolver ");
        println("    INFO->FINE : net.jxta.endpoint ");
        println("    WARNING->FINE : net.jxta.impl.resolver ");
        println(" ");
        println("    Set the packages 'net.jxta.endpoint' and ");
        println("    'net.jxta.impl.resolver' to \"FINE\" logging level.");
        println(" ");
    }
}
