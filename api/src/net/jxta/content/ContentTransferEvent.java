/*
 *  The Sun Project JXTA(TM) Software License
 *  
 *  Copyright (c) 2001-2007 Sun Microsystems, Inc. All rights reserved.
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
 *     developed by Sun Microsystems, Inc. for JXTA(TM) technology." 
 *     Alternately, this acknowledgment may appear in the software itself, if 
 *     and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must 
 *     not be used to endorse or promote products derived from this software 
 *     without prior written permission. For written permission, please contact 
 *     Project JXTA at http://www.jxta.org.
 *  
 *  5. Products derived from this software may not be called "JXTA", nor may 
 *     "JXTA" appear in their name, without prior written permission of Sun.
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND 
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL SUN 
 *  MICROSYSTEMS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  JXTA is a registered trademark of Sun Microsystems, Inc. in the United 
 *  States and other countries.
 *  
 *  Please see the license information page at :
 *  <http://www.jxta.org/project/www/license.html> for instructions on use of 
 *  the license in source files.
 *  
 *  ====================================================================

 *  This software consists of voluntary contributions made by many individuals 
 *  on behalf of Project JXTA. For more information on Project JXTA, please see 
 *  http://www.jxta.org.
 *  
 *  This license is based on the BSD license adopted by the Apache Foundation. 
 */

package net.jxta.content;

import java.util.EventObject;

/**
 * ContentTransfer event object, containing information related to the event
 * being published.  Accessor methods are provided for fields which are
 * optionally supplied by the ContentProvider implementation.
 */
public class ContentTransferEvent extends EventObject {

    /**
     * The current source location state.
     */
    private final ContentSourceLocationState locationState;

    /**
     * The current transfer state.
     */
    private final ContentTransferState transferState;

    /**
     * Total number of known remote potential data sources for this transfer.
     */
    private final Integer locationCount;
    
    /**
     * Total number of bytes received this far, if provided.
     */
    private Long bytesReceived;
    
    /**
     * Total number of bytes to be transferred, if provided.
     */
    private Long bytesTotal;

    /**
     * Creates a new instance of ContentTransferEvent.
     *
     * @param source ContentTransfer issueing this event
     */
    public ContentTransferEvent(ContentTransfer source) {
        this(source, null, null, null);
    }

    /**
     * Creates a new instance of ContentTransferEvent.
     *
     * @param source ContentTransfer issueing this event
     * @param knownLocations current number of discovered remote data sources,
     *  if known
     */
    public ContentTransferEvent(
            ContentTransfer source, Integer knownLocations) {
        this(source, knownLocations, null, null);
    }

    /**
     * Creates a new instance of ContentTransferEvent.
     *
     *
     * @param source ContentTransfer issueing this event
     * @param knownLocations current number of discovered remote data sources,
     *  if known
     * @param contentLocationState current source location state, if known
     * @param contentTransferState current transfer state, if known
     */
    public ContentTransferEvent(
            ContentTransfer source, Integer knownLocations,
            ContentSourceLocationState contentLocationState,
            ContentTransferState contentTransferState) {
        super(source);
        locationCount = knownLocations;
        locationState = contentLocationState;
        transferState = contentTransferState;
    }

    /**
     * Get the ContentTransfer which produced this event.
     *
     * @return the originator of this event
     */
    public ContentTransfer getContentTransfer() {
        return (ContentTransfer) getSource();
    }

    /**
     * Gets the source location state.
     *
     * @return source location state, or <tt>null</tt> if not provided
     */
    public ContentSourceLocationState getSourceLocationState() {
        return locationState;
    }

    /**
     * Gets the number of remote sources located by the provider thus
     * far.
     *
     * @return number of remote sources, or <tt>null</tt> if not provided
     */
    public Integer getSourceLocationCount() {
        return locationCount;
    }

    /**
     * Gets the transfer state.
     *
     * @return transfer state, or <tt>null</tt> if not provided
     */
    public ContentTransferState getTransferState() {
        return transferState;
    }
    
    /**
     * Sets the number of bytes received thus far for the transfer
     * to which this event pertains.  This method is intended to be
     * called by ContentProvider implementations during event creation
     * and should not be called by listeners of these events.
     * 
     * @param count number of bytes
     */
    public void setBytesReceived(long count) {
        bytesReceived = count;
    }
    
    /**
     * Gets the number of bytes received thus far for the transfer to
     * which this event pertains, if provided.
     * 
     * @return number of bytes, or null if not provided
     */
    public Long getBytesReceived() {
        return bytesReceived;
    }

    /**
     * Sets the total of bytes to be received for the transfer
     * to which this event pertains.  This method is intended to be
     * called by ContentProvider implementations during event creation
     * and should not be called by listeners of these events.
     * 
     * @param count number of bytes
     */
    public void setBytesTotal(long count) {
        bytesTotal = count;
    }
    
    /**
     * Gets the number of bytes to be received for the transfer to
     * which this event pertains, if provided.
     * 
     * @return number of bytes, or null if not provided
     */
    public Long getBytesTotal() {
        return bytesTotal;
    }

}
