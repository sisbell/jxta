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

package Examples.K_Service;

import Examples.Z_Tools_And_Others.Tools;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import javax.swing.JFileChooser;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.impl.peergroup.StdPeerGroup;
import net.jxta.impl.protocol.ModuleImplAdv;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.service.Service;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;

public class _710_Astrology_Service_Example implements Service, Runnable {
    
    // Static
    
    public static final String Name = "Astrology Service";
    
    public static final String NameSpace = "AstrologyService";
    
    public static final String CustomerNameElement = "CustomerName";
    public static final String BirthDateElement = "CustomerBirthDate";
    public static final String BirthLocationElement = "CustomerBirthLocation";
    
    public static final String PredictionElement = "Prediction";
    
    public static final String MyModuleClassIDString = "urn:jxta:uuid-F7A712D25D3047B88656FD706AEDE8DB05";
    public static final String MyModuleSpecIDString = "urn:jxta:uuid-F7A712D25D3047B88656FD706AEDE8DBC6A510B2026F4FD59A7DFA4F6712142506";
    
    public static ModuleClassID MyModuleClassID = null;
    public static ModuleSpecID MyModuleSpecID = null;

    static {
        
        try {
            
            MyModuleClassID = ModuleClassID.create(new URI(MyModuleClassIDString));
            MyModuleSpecID = ModuleSpecID.create(new URI(MyModuleSpecIDString));
        
        } catch (Exception Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            
        }
        
    }
    
    // Not static
    private PeerGroup ThePeerGroup = null;
    private ID TheID = null;
    private ModuleImplAdv TheImplementationAdvertisement = null;
    
    private JxtaServerPipe MyBiDiPipeServer = null;

    public static void main(String[] args) {
        
        // Dummy main method for compilation
        
    }
    
    public _710_Astrology_Service_Example() {
        
    }
    
    public static PipeAdvertisement GetPipeAdvertisement() {
        
        // Creating a Pipe Advertisement
        PipeAdvertisement MyPipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID MyPipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, Name.getBytes());

        MyPipeAdvertisement.setPipeID(MyPipeID);
        MyPipeAdvertisement.setType(PipeService.UnicastType);
        MyPipeAdvertisement.setName("Astrology Service Pipe");
        MyPipeAdvertisement.setDescription("Created by " + Name);
        
        return MyPipeAdvertisement;
        
    }
    
    public static ModuleSpecAdvertisement GetModuleSpecificationAdvertisement() {
        
        ModuleSpecAdvertisement Result = (ModuleSpecAdvertisement) AdvertisementFactory.newAdvertisement(ModuleSpecAdvertisement.getAdvertisementType());

        Result.setCreator("The Astrologers");
        Result.setDescription("Astrology Service");
        Result.setModuleSpecID(MyModuleSpecID);
        Result.setVersion("1.0");
        Result.setPipeAdvertisement(GetPipeAdvertisement());
        
        return Result;
                
    }
    
    public static ModuleImplAdvertisement GetModuleImplementationAdvertisement() {
        
        ModuleImplAdvertisement Result = (ModuleImplAdvertisement) AdvertisementFactory.newAdvertisement(ModuleImplAdvertisement.getAdvertisementType());
        
        // Setting parameters
        Result.setDescription("Astrology Service");
        Result.setModuleSpecID(MyModuleSpecID);
        Result.setProvider(Name);
        Result.setCode(_710_Astrology_Service_Example.class.getName());
        
        // Setting compatibility & binding
        Result.setCompat(StdPeerGroup.STD_COMPAT);
        
        // Retrieving the location of the .jar file
        JFileChooser MyFileChooser = new JFileChooser();
        File SelectedFile = null;
        
        Tools.PopInformationMessage(Name, "Retrieving the implementation location of the astrology service");
        
        int TheReturnedValue = MyFileChooser.showOpenDialog(null);
        
        if (TheReturnedValue == JFileChooser.APPROVE_OPTION) {
            SelectedFile = MyFileChooser.getSelectedFile();
        } 
        
        if (SelectedFile==null) {
            
            Tools.PopWarningMessage(Name, "No file selected");
            
        } else {
            Result.setUri(SelectedFile.toURI().toString());
        }
        
        return Result;
        
    }

    public Service getInterface() {
        
        return this;
        
    }

    public Advertisement getImplAdvertisement() {
        
        return TheImplementationAdvertisement;
        
    }
    
    public ID getID() {
        
        return TheID;
        
    }

    public void init(PeerGroup InPG, ID InID, Advertisement InAdvertisement) throws PeerGroupException {

        // Initialization
        ThePeerGroup = InPG;
        TheID = InID;
        TheImplementationAdvertisement = (ModuleImplAdv) InAdvertisement;
        
    }

    public int startApp(String[] arg0) {
        
        try {

            MyBiDiPipeServer = new JxtaServerPipe(ThePeerGroup, GetPipeAdvertisement(), 5000);
            
            Thread thread = new Thread(this);
            thread.start();
            
            Tools.PopInformationMessage(Name, "Start Successful");

            return START_OK;
            
        } catch (IOException Ex) {
            
            Tools.PopErrorMessage(Name, Ex.toString());
            Tools.PopInformationMessage(Name, "Start Unsuccessful");
            
            return START_DISABLED;
            
        }
        
    }
    
    public void run() {
        
        while (MyBiDiPipeServer != null) {
            
            try {

                JxtaBiDiPipe MyBiDiPipe = this.MyBiDiPipeServer.accept();

                if (MyBiDiPipe != null) {

                    // Processing customers
                    Thread thread = new Thread(new CustomerHandler(MyBiDiPipe));
                    thread.start();
                    
                }
                
            } catch (SocketTimeoutException Ex) {
                
                // We don't care if we get a timeout after 5 seconds
                // We try to accept a connection again
                
            } catch (IOException Ex) {
                
                Tools.PopErrorMessage(Name, Ex.toString());
                
            }
            
        } 
        
    }

    public void stopApp() {
        
        // Closing bidipipe server
        if (MyBiDiPipeServer != null) {
            
            try {

                MyBiDiPipeServer.close();
                MyBiDiPipeServer = null;
                Tools.PopInformationMessage(Name, "Stop Successful");
                
            } catch (IOException Ex) {
                
                Tools.PopErrorMessage(Name, Ex.toString());
                
            }
            
        }
        
    }
    
    private static class CustomerHandler implements Runnable, PipeMsgListener {
        
        private JxtaBiDiPipe MyJxtaBiDiPipe = null;
        
        CustomerHandler(JxtaBiDiPipe InPipe) {
            
            MyJxtaBiDiPipe = InPipe;
            MyJxtaBiDiPipe.setMessageListener(this);
            
        }
        
        public static final int ComputeHoroscopeHash(String InString) {
            
            int Result = 0;
            
            if (InString != null) {
                for (int i=0;i<InString.length();i++) {
                    Result = Result + (int) InString.charAt(i);
                }
            }
                
            // Returning result
            return ( Result % 3 );
            
        }
        
        public void pipeMsgEvent(PipeMsgEvent event) {
            
            try {
                
                long PredictionHash = System.currentTimeMillis();

                // Retrieve the message
                Message MyMessage = event.getMessage();

                MessageElement MyMessageElement = MyMessage.getMessageElement(NameSpace, CustomerNameElement);
                PredictionHash = PredictionHash + ComputeHoroscopeHash(MyMessageElement.toString());
                
                MyMessageElement = MyMessage.getMessageElement(NameSpace, BirthDateElement);
                PredictionHash = PredictionHash + ComputeHoroscopeHash(MyMessageElement.toString());

                MyMessageElement = MyMessage.getMessageElement(NameSpace, BirthLocationElement);
                PredictionHash = PredictionHash + ComputeHoroscopeHash(MyMessageElement.toString());
                
                PredictionHash = PredictionHash % 3;
                
                String Prediction = "";
                
                switch ((int)PredictionHash) {
                    
                    case 0: Prediction = "You will be rich!"; break;
                    
                    case 1: Prediction = "You will be famous!"; break;
                    
                    default: Prediction = "You need to make more sacrifices to the Gods!";
                    
                }

                // Sending answer
                MyMessage = new Message();
                StringMessageElement MyStringMessageElement = new StringMessageElement(PredictionElement, Prediction, null);
                MyMessage.addMessageElement(NameSpace, MyStringMessageElement);

                MyJxtaBiDiPipe.sendMessage(MyMessage);
                
                // Closing the connection
                MyJxtaBiDiPipe.close();
                
            } catch (IOException Ex) {
                
                Tools.PopErrorMessage(Name, Ex.toString());
                
            }
            
        }
        
        public void run() {
            
            // The pipeMsgEvent will be called when necessary
            
        }
        
    }

}
