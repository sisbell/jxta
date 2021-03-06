/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights reserved.
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
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xindice" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
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
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999-2001, The dbXML
 * Group, L.L.C., http://www.dbxmlgroup.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *

 */
package net.jxta.impl.xindice;

import net.jxta.impl.xindice.util.Named;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Debug is a debugging class for the Xindice Server.  Because the class
 * and the Debugging field are final, the Java compiler should always
 * inline the methods and optimize them away if Debugging is set to false.
 */

public final class Debug {
   
    /**
     *   Log4J Logger. Since this is the logger for the whole of xindice, we
     *   compose the name a little differently than normal.
     **/
    private final static Logger LOG = Logger.getLogger(Debug.class.getName());
    
    public static final boolean Debugging = true;
   
    public static void SetPrintStream(PrintStream out) {
        ;
    }

    public static void println(Object obj, Object message) {
        if (Debugging) {
            if (obj instanceof Named) {
                LOG.fine(((Named) obj).getName() + ": " + message);
            } else {
                LOG.fine(message + "\n\t@ " + obj);
            }
        }
    }

    public static void println(Object message) {
        if (Debugging) {
            LOG.fine(message.toString());
        }
    }

    public static void println() {
        if (Debugging) {
            LOG.fine("");
        }
    }
   
    public static void printStackTrace(Throwable t) {
        if (Debugging) {
            LOG.log(Level.WARNING, t.getMessage(), t);
        }
    }
   
    public static void setPrintStream(PrintStream outStream) {
        ;
    }
   
    public static void setPrefix(String debugPrefix) {
        ;
    }
}
