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
 * $Id: share.java,v 1.17 2007/02/09 23:12:51 hamada Exp $
 */
package net.jxta.impl.shell.bin.share;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.impl.shell.ShellApp;
import net.jxta.impl.shell.ShellEnv;
import net.jxta.impl.shell.ShellObject;
import net.jxta.peergroup.PeerGroup;

/**
 * share Shell command
 * <p/>
 * XXX:
 * This command should be a Codat Sharing Service (CMS). Since at this time
 * there is no available CMS, it uses the internal Core's Codat Sharing mechanism.
 * This must be changed as soon as there is an available CMS.
 * lomax@jxta.org
 */
public class share extends ShellApp {

    private DiscoveryService disco = null;

    ShellEnv env;

    public share() {
    }

    @Override
    public void stopApp() {
    }

    private int syntaxError() {
        println("Usage: share <StructuredDocument>");
        return ShellApp.appParamError;
    }

    public int startApp(String[] args) {

        if ((args == null) || (args.length != 1)) {
            return syntaxError();
        }

        env = getEnv();

        String name = args[0];

        /*
       *  get the std group
       */

        ShellObject obj = env.get("stdgroup");
        PeerGroup group = (PeerGroup) obj.getObject();

        disco = group.getDiscoveryService();

        obj = env.get(name);
        if (obj == null) {
            println("share: cannot access " + name);
            return ShellApp.appMiscError;
        }

        Advertisement adv;

        try { // only publish in group, may need to revisit this
            adv = AdvertisementFactory.newAdvertisement((StructuredTextDocument) obj.getObject());
            publishAdv(adv);
        } catch (Exception e) {
            println("share: " + name + " is not a proper Document");
            return ShellApp.appMiscError;
        }
        return ShellApp.appNoError;
    }


    private void publishAdv(Advertisement adv) {
        try {
            disco.publish(adv);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getDescription() {
        return "Share an advertisement";
    }

    @Override
    public void help() {
        println("NAME");
        println("     share - share an advertisement");
        println(" ");
        println("SYNOPSIS");
        println("     share <advertisement>");
        println(" ");
        println("DESCRIPTION");
        println(" ");
        println("share an advertisement document in the current peer group.");
        println("The document is made visible to all the members of the peer group.");
        println("Advertisements are XML documents that can represent any JXTA objects");
        println("advertisement, environment variables). Documents are searched");
        println("either in the local peer cache or remotely via the 'search' command.");
        println(" ");
        println("OPTIONS");
        println(" ");
        println("EXAMPLE");
        println(" ");
        println("    JXTA>share mydoc ");
        println(" ");
        println("This example shares the document 'mydoc' into the current peer group.");
        println(" ");
        println("SEE ALSO");
        println("    peers search");
    }
}
