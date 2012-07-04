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

package Examples.C_Peers_And_PeerGroups;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;

public class _200_Creating_IDs_Example {

    public static final String Name = "Example 200";
    
    public static void main(String[] args) {

        // Creating a peer group ID and a sub-peer group ID
        PeerGroupID MyPeerGroupID_A = IDFactory.newPeerGroupID();
        PeerGroupID MyPeerGroupID_B = IDFactory.newPeerGroupID(MyPeerGroupID_A);
        PeerGroupID MyPeerGroupID_C = IDFactory.newPeerGroupID(MyPeerGroupID_B);
        
        // Creating peer IDs
        PeerID MyPeer_A = IDFactory.newPeerID(MyPeerGroupID_A);
        PeerID MyPeer_B = IDFactory.newPeerID(MyPeerGroupID_B);
        PeerID MyPeer_C = IDFactory.newPeerID(MyPeerGroupID_C);
        
        byte[] MySeed = { 0, 1, 2, 3, 4, 5, 6 };
        PeerID MyPeer_Seed = IDFactory.newPeerID(MyPeerGroupID_A, MySeed);
        
        // Creating a Pipe ID
        PipeID MyPipe_A = IDFactory.newPipeID(MyPeerGroupID_A);
        
        // Displaying the IDs
        System.out.println("Peer Group A        : " + MyPeerGroupID_A.toString());
        System.out.println("Peer of A           : " + MyPeer_A.toString());
        System.out.println("Pipe of A           : " + MyPipe_A.toString());
        
        System.out.println("Peer of A with seed : " + MyPeer_Seed.toString());
        
        System.out.println("Peer Group B of A   : " + MyPeerGroupID_B.toString());
        System.out.println("Peer of B           : " + MyPeer_B.toString());
        
        System.out.println("Peer Group C of B   : " + MyPeerGroupID_C.toString());
        System.out.println("Peer of C           : " + MyPeer_C.toString());

    }
        
}
