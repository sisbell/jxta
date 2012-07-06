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
 * $Id: version.java,v 1.1015 2007/02/09 23:12:47 hamada Exp $
 */
package net.jxta.impl.shell.bin.version;

import net.jxta.impl.shell.GetOpt;
import net.jxta.impl.shell.ShellApp;

/**
 * version command
 */
public class version extends ShellApp {
    
    /**
     * {@inheritDoc}
     */
    public int startApp(String[] argv) {
        boolean verbose = false;
        
        boolean recursive = false;
        
        GetOpt options = new GetOpt(argv, 0, "vr");
        
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
                case 'v':
                    verbose = true;
                    break;
                case 'r':
                    recursive = true;
                    break;
                default:
                    consoleMessage("Unrecognized option");
                    return syntaxError();
            }
        }
        
        if (null != options.getNextParameter()) {
            consoleMessage("Unsupported parameter");
            return syntaxError();
        }
        
        String currentPackage = "net.jxta.impl.shell";
        
        do {
            Package aPackage = Package.getPackage(currentPackage);
            
            if ((null == aPackage) || (null == aPackage.getImplementationVersion())) {
                println( currentPackage + " : " + "Version information not available");
            } else {
                if (!verbose) {
                    println( currentPackage + " : " + aPackage.getImplementationVersion());
                } else {
                    println("Package : " + currentPackage );
                    println("Specification Title : " + aPackage.getSpecificationTitle());
                    println("Specification Vendor : " + aPackage.getSpecificationVendor());
                    println("Specification Version : " + aPackage.getSpecificationVersion());                    
                    println(" ");
                    
                    println("Implementation Title : " + aPackage.getImplementationTitle());
                    println("Implementation Vendor : " + aPackage.getImplementationVendor());
                    println("Implementation Version : " + aPackage.getImplementationVersion());
                    println(" ");
                }
            }
            
            int lastDot = currentPackage.lastIndexOf('.');
            if(-1 == lastDot) {
                break;
            } else {
                currentPackage = currentPackage.substring(0, lastDot);
            }
        } while( recursive );
        
        return ShellApp.appNoError;
    }
    
    private int syntaxError() {
        consoleMessage("Usage: version [-v] [-r]");
        return ShellApp.appParamError;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Display the version number of this Shell instance.";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void help() {
        println("NAME");
        println("     version - " + getDescription());
        println(" ");
        println("SYNOPSIS");
        println(" ");
        println("     version [-v] [-r]");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("     [-v]  Print verbose information.");
        println("     [-v]  Recurse printing the version numbers of all parent packages as well.");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("The version command displays the version number associated ");
        println("with this Shell instance. In order to work correctly the");
        println("shell must be packaged in a jar file with a complete manifest");
        println("(this is normally the case).");
        println(" ");
    }
}
