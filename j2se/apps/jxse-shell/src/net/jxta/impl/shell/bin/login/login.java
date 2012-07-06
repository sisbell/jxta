/*
 * Copyright (c) 2002 Sun Microsystems, Inc.  All rights
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
 * $Id: login.java,v 1.15 2007/02/09 23:12:50 hamada Exp $
 */
package net.jxta.impl.shell.bin.login;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.document.StructuredDocument;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.impl.shell.*;
import net.jxta.membership.Authenticator;
import net.jxta.membership.InteractiveAuthenticator;
import net.jxta.membership.MembershipService;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Authenticate with the group's membership service.
 */
public final class login extends ShellApp {

    private ShellEnv env = getEnv();

    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        env = getEnv();
        String credDoc = null;
        String spawnApp = null;
        StructuredDocument creds = null;
        boolean interactive = false;

        GetOpt options = new GetOpt(argv, 0, "ic:s:");

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
                case'c':
                    credDoc = options.getOptionArg();
                    break;

                case's':
                    spawnApp = options.getOptionArg();
                    break;

                case'i':
                    interactive = true;
                    break;

                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }

        if (null != credDoc) {
            ShellObject obj = env.get(credDoc);
            if (!StructuredDocument.class.isAssignableFrom(obj.getObjectClass())) {
                consoleMessage("Provided Credential was not a Structured Document");
                return ShellApp.appMiscError;
            }

            creds = (StructuredDocument) obj.getObject();
        }

        if (interactive) {
            ShellObject obj = env.get("console");

            if ((null == obj) || (!obj.getObjectClass().isAssignableFrom(SwingShellConsole.class))) {
                consoleMessage("Interactive login unavailable");
                return ShellApp.appMiscError;
            }
        }

        int result = authenticate(creds, interactive);

        if (ShellApp.appNoError != result) {
            consoleMessage("Login failed.");
            return result;
        }

        if (null != spawnApp) {
            List<String> passArgs = new ArrayList<String>();

            do {
                String anArg = options.getNextParameter();

                if (null == anArg)
                    break;

                passArgs.add(anArg);
            } while (true);

            String[] args = passArgs.toArray(new String[passArgs.size()]);

            return exec(getReturnVariable(), spawnApp, args, env);
        } else {
            return ShellApp.appNoError;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Authenticate with the group's membership service.";
    }

    private int syntaxError() {
        consoleMessage("Usage: login [-i] [-c <creddoc>] [-s <command>] [-- [args]] ");
        return ShellApp.appParamError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println(" ");
        println("    login - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("    login [-i] [-c <creddoc>] [-s <command>] [-- [args]] ");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("login prompts for authentication. If the authentication is ");
        println("completed then the specified command, if any is started. ");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("     [-i]              Use a UI based authenticator.");
        println("     [-c <creddoc>]    Authentication credential to provide to authenticator.");
        println("     [-s <command>]    The shell command to start.");
        println("     [-- [args]]       Arguments to be passed to <command>.");
        println(" ");
        println("SEE ALSO");
        println("    join leave who rshd rsh ");
    }

    private int authenticate(StructuredDocument creds, boolean interactive) {

        try {
            MembershipService membership = getGroup().getMembershipService();

            AuthenticationCredential authCred = new AuthenticationCredential(
                    getGroup(),
                    interactive ? "InteractiveAuthentication" : "StringAuthentication",
                    creds);

            Authenticator auth;
            try {
                auth = membership.apply(authCred);
            } catch (ProtocolNotSupportedException notSupported) {
                consoleMessage("Authenticator does not support requested authentication type.");
                interactive = false;

                // try the default, why not....
                authCred = new AuthenticationCredential(
                        getGroup(),
                        null,
                        creds);

                auth = membership.apply(authCred);
            }

            consprintln(getGroup().getPeerName() + " - Enter the identity you want to use for group '" + getGroup().getPeerGroupName() + "' :");

            if (interactive) {
                ((InteractiveAuthenticator) auth).interact();
            } else {
                completeAuthenticator(auth);
            }

            if (!auth.isReadyForJoin()) {
                consoleMessage("Failure in authentication. Incomplete authenticator");
                return ShellApp.appMiscError;
            }

            Credential credential = membership.join(auth);

            ShellObject<Credential> newObj = new ShellObject<Credential>("Credential", credential);
            env.add(getReturnVariable(), newObj);

            return ShellApp.appNoError;
        } catch (Throwable ez1) {
            printStackTrace("login failed ", ez1);
            return ShellApp.appMiscError;
        }
    }

    private void completeAuthenticator(Authenticator auth) throws Exception {

        Method[] methods = auth.getClass().getMethods();
        List<Method> authMethods = new ArrayList<Method>();

        // go through the methods of the auth class and insert them in name sorted
        // order into a vector.
        for (Method method : methods) {
            // can only call public methods
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            // only these magic "setAuth" methods.
            if (!method.getName().startsWith("setAuth")) {
                continue;
            }

            // only those with one String parameter
            Class params[] = method.getParameterTypes();

            if ((params.length != 1) || (params[0] != String.class)) {
                continue;
            }

            // sorted insertion.
            for (int doInsert = 0; doInsert <= authMethods.size(); doInsert++) {
                int insertHere = -1;
                if (doInsert == authMethods.size())
                    insertHere = doInsert;
                else {
                    if (method.getName().compareTo((authMethods.get(doInsert)).getName()) <= 0)
                        insertHere = doInsert;
                }

                if (-1 != insertHere) {
                    authMethods.add(insertHere, method);
                    break;
                }
            }
        }

        // get input for each of the methods.
        for (Object authMethod : authMethods) {
            Method doingMethod = (Method) authMethod;

            String authStepName = doingMethod.getName().substring(8);

            // remove the _ for a non-echo field.
            // FIXME The shell can't do no echo.
            if ('_' == authStepName.charAt(0)) {
                authStepName = authStepName.substring(1);
            }

            consprint(authStepName + " : ");
            String userinput = consWaitForInput();

            Object[] params = {userinput};

            doingMethod.invoke(auth, params);
        }
    }
}
