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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.Element;
import net.jxta.document.TextElement;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;

public class _500_Customized_Advertisement_Example extends Advertisement {
    
    public static final String Name = "Example 500";

    // Advertisement elements, tags and indexables
    public final static String AdvertisementType = "jxta:CustomizedAdvertisement";
    
    private ID AdvertisementID = ID.nullID;

    private String TheName = "";
    private int TheAge = -1;
    
    private final static String IDTag = "MyIDTag";
    private final static String NameTag = "MyNameTag";
    private final static String AgeTag = "MyAgeTag";
    
    private final static String[] IndexableFields = { IDTag, NameTag };

    public _500_Customized_Advertisement_Example() {
        
        // Accepting default values

    }

    public _500_Customized_Advertisement_Example(Element Root) {
        
        // Retrieving the elements
        TextElement MyTextElement = (TextElement) Root;

        Enumeration TheElements = MyTextElement.getChildren();
        
        while (TheElements.hasMoreElements()) {
            
            TextElement TheElement = (TextElement) TheElements.nextElement();
            
            ProcessElement(TheElement);
            
        }        

    }
    
    public void ProcessElement(TextElement TheElement) {
        
        String TheElementName = TheElement.getName();
        String TheTextValue = TheElement.getTextValue();
        
        if (TheElementName.compareTo(IDTag)==0) {
            
            try {
                
                URI ReadID = new URI(TheTextValue);
                AdvertisementID = IDFactory.fromURI(ReadID);
                return;
                
            } catch (URISyntaxException Ex) {
                
                // Issue with ID format
                Ex.printStackTrace();
                
            } catch (ClassCastException Ex) {
                
                // Issue with ID type
                Ex.printStackTrace();
                
            }
            
        }
        
        if (TheElementName.compareTo(NameTag)==0) {
            
            TheName = TheTextValue;
            return;
            
        }
        
        if (TheElementName.compareTo(AgeTag)==0) {
            
            TheAge = Integer.parseInt(TheTextValue);
            return;
            
        }
        
    }
    
    @Override
    public Document getDocument(MimeMediaType TheMimeMediaType) {
        
        // Creating document
        StructuredDocument TheResult = StructuredDocumentFactory.newStructuredDocument(
                TheMimeMediaType, AdvertisementType);
        
        // Adding elements
        Element MyTempElement;
        
        MyTempElement = TheResult.createElement(NameTag, TheName);
        TheResult.appendChild(MyTempElement);
        
        MyTempElement = TheResult.createElement(AgeTag, Integer.toString(TheAge));
        TheResult.appendChild(MyTempElement);
        
        return TheResult;
        
    }

    public void SetID(ID TheID) {
        AdvertisementID = TheID;
    }

    @Override
    public ID getID() {
        return AdvertisementID;
    }

    @Override
    public String[] getIndexFields() {
        return IndexableFields;
    }

    public void SetName(String InName) {
        TheName = InName;
    }

    public void SetAge(int InAge) {
        TheAge = InAge;
    }
    
    public String GetName() {
        return TheName;
    }

    public int GetAge() {
        return TheAge;
    }
    
    @Override
    public _500_Customized_Advertisement_Example clone() throws CloneNotSupportedException {
        
        _500_Customized_Advertisement_Example Result =
                (_500_Customized_Advertisement_Example) super.clone();

        Result.AdvertisementID = this.AdvertisementID;
        Result.TheName = this.TheName;
        Result.TheAge = this.TheAge;
        
        return Result;
        
    }
    
    @Override
    public String getAdvType() {
        
        return _500_Customized_Advertisement_Example.class.getName();
        
    }
    
    public static String getAdvertisementType() {
        return AdvertisementType;
    }    
    
    public static class Instantiator implements AdvertisementFactory.Instantiator {

        public String getAdvertisementType() {
            return _500_Customized_Advertisement_Example.getAdvertisementType();
        }

        public Advertisement newInstance() {
            return new _500_Customized_Advertisement_Example();
        }

        public Advertisement newInstance(net.jxta.document.Element root) {
            return new _500_Customized_Advertisement_Example(root);
        }
        
    }

}
