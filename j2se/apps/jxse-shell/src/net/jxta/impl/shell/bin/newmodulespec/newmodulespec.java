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
 * $Id: newmodulespec.java,v 1.5 2007/02/09 23:12:41 hamada Exp $
 */
package net.jxta.impl.shell.bin.newmodulespec;

import java.net.URI;
import java.net.URISyntaxException;
import net.jxta.document.AdvertisementFactory;
import net.jxta.id.IDFactory;
import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleSpecAdvertisement;

/**
 * Create a new module spec advertisment
 */
public class newmodulespec extends ShellApp {

    public newmodulespec() {
    }

    /**
     *  {@inheritDoc}
     */
    public int startApp(String[] argv) {

        ShellEnv env = getEnv();
        String name = null;
        ModuleSpecID proxySpec = null;
        ModuleSpecID authSpec = null;
        ModuleClassID base;
        ModuleSpecAdvertisement adv;

        GetOpt options = new GetOpt(argv, 0, "n:");

        while (true) {
            int option;
            try {
                option = options.getNextOption();
            } catch (IllegalArgumentException badopt) {
                consoleMessage("Illegal arguement :" + badopt);
                return syntaxError();
            }

            if (-1 == option) {
                break;
            }

            switch (option) {
                case'n':
                    name = options.getOptionArg();
                    break;


                case'p':
                    try {
                        URI moduleSpecID = new URI(options.getOptionArg());
                        proxySpec = (ModuleSpecID) IDFactory.fromURI(moduleSpecID);
                    } catch (ClassCastException badID) {
                        printStackTrace("ID is not a module spec ID", badID);
                        return syntaxError();
                    } catch (URISyntaxException badID) {
                        printStackTrace("bad module spec id ID", badID);
                        return syntaxError();
                    }
                    break;

                case'a':
                    try {
                        URI moduleSpecID = new URI(options.getOptionArg());
                        authSpec = (ModuleSpecID) IDFactory.fromURI(moduleSpecID);
                    } catch (ClassCastException badID) {
                        printStackTrace("ID is not a module spec ID", badID);
                        return syntaxError();
                    } catch (URISyntaxException badID) {
                        printStackTrace("bad module spec id ID", badID);
                        return syntaxError();
                    }
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        try {
            String baseMCIDparam = options.getNextParameter();
            if(null == baseMCIDparam) {
                consoleMessage("Missing required base module class id.");
                return syntaxError();
            }
            URI moduleClassID = new URI(baseMCIDparam);
            base = (ModuleClassID) IDFactory.fromURI(moduleClassID);
        } catch (ClassCastException badID) {
            printStackTrace("ID is not a module class ID", badID);
            return syntaxError();
        } catch (URISyntaxException badID) {
            printStackTrace("bad module class id ID", badID);
            return syntaxError();
        }

        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }
        
        try {
            adv = (ModuleSpecAdvertisement) AdvertisementFactory.newAdvertisement(
                ModuleSpecAdvertisement.getAdvertisementType());

            adv.setModuleSpecID(IDFactory.newModuleSpecID(base));
            adv.setName(name);            
//          adv.setDescription("created by newmodulespec");
            adv.setProxySpecID(proxySpec);
            adv.setAuthSpecID(authSpec);
            
            ShellObject<ModuleSpecAdvertisement> newAdv = new ShellObject<ModuleSpecAdvertisement>("Module Spec Advertisement", adv);
            env.add(getReturnVariable(), newAdv);

            return ShellApp.appNoError;
        } catch (Throwable all) {
            printStackTrace("Failure creating pipe advertisment", all);
            return ShellApp.appMiscError;
        }
    }

    public int syntaxError() {
        consoleMessage("Usage: newmodulespec [-n <name>] [-p <proxySpecID>] [-a <authSpecID>] <basemcid>");
        return ShellApp.appParamError;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Create a new Module Class advertisment";
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     newmodulespec - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     newmodulespec [-n <name>] [-p <proxySpecID>] [-a <authSpecID>] <basemcid>");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("      [-n <name>]         Optional name for the module spec");
        println("      [-p <proxySpecID>]  Optional proxy spec ID for this module spec");
        println("      [-a <authSpecID>]   Optional auth spec ID for this module spec");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("newmodulespec creates a new module spec advertisement with a random module class id. ");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("          JXTA>mymcadv = newmodulespec -n mymodule urn:jxta:uuid:");
        println(" ");
        println(" This creates a new module spec advertisement. The new ");
        println(" module class is given the name 'mymodule'. ");
        println(" ");
        println("SEE ALSO");
        println("    newmoduleclass newmoduleimpl");
    }
}
