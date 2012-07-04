/*
 * Copyright (c) 2010 DawningStreams, Inc.  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions are met:
 *  
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright notice, 
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  
 *  3. The end-user documentation included with the redistribution, if any, must 
 *     include the following acknowledgment: "This product includes software 
 *     developed by DawningStreams, Inc." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The name "DawningStreams,Inc." must not be used to endorse or promote
 *     products derived from this software without prior written permission.
 *     For written permission, please contact DawningStreams,Inc. at 
 *     http://www.dawningstreams.com.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 *  DAWNINGSTREAMS, INC OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  DawningStreams is a registered trademark of DawningStreams, Inc. in the United 
 *  States and other countries.
 *  
 */

package Examples.E_Messages_And_Advertisements;

import Examples.Z_Tools_And_Others.Tools;
import java.io.IOException;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.XMLDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;

public class _440_Adding_An_Advertisement_In_Message_Example {

    public static final String Name = "Example 440";
    
    public static void main(String[] args) {
            
        // Creating a customized advertisement
        _500_Customized_Advertisement_Example MyAdvertisement = new _500_Customized_Advertisement_Example();
        MyAdvertisement.SetName("John");
        MyAdvertisement.SetAge(33);

        // Creating the message
        Message MyMessage = new Message();

        // Creating the message element and adding it
        TextDocumentMessageElement MyTextDocumentMessageElement = new TextDocumentMessageElement(
                "CUSTOMIZED_ADVERTISEMENT", 
                (XMLDocument) MyAdvertisement.getDocument(MimeMediaType.XMLUTF8),
                null);

        MyMessage.addMessageElement("CUSTOMIZED_ADVERTISEMENT",MyTextDocumentMessageElement);

        // Retrieving the advertisement from the message
        MessageElement MyMessageElement = MyMessage.getMessageElement("CUSTOMIZED_ADVERTISEMENT","CUSTOMIZED_ADVERTISEMENT");

        try {

            XMLDocument TheDocument = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(
                MyMessageElement.getMimeType(),
                MyMessageElement.getStream());

            _500_Customized_Advertisement_Example MyCustomizedAdvertisement =
                    new _500_Customized_Advertisement_Example(TheDocument.getRoot());

            // Displaying advertisement
            Tools.PopInformationMessage(Name, MyCustomizedAdvertisement.toString());

        } catch (IOException Ex) {

            // Thrown when message element cannot be read.
            Tools.PopErrorMessage(Name, Ex.toString());

        } catch (IllegalArgumentException Ex) {

            // Thrown when the document or advertisement has an invalid
            // structure (illegal values or missing tags...)
            Tools.PopErrorMessage(Name, Ex.toString());

        }

    }

}
