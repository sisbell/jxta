/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights reserved.
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
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA"
 *    must not be used to endorse or promote products derived from this
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
 */

package net.jxta.impl.content;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import junit.framework.TestCase;
import net.jxta.content.Content;
import net.jxta.content.ContentID;
import net.jxta.content.ContentProviderSPI;
import net.jxta.content.ContentSourceLocationState;
import net.jxta.content.ContentTransfer;
import net.jxta.content.ContentTransferAggregatorEvent;
import net.jxta.content.ContentTransferAggregatorListener;
import net.jxta.content.ContentTransferEvent;
import net.jxta.content.ContentTransferListener;
import net.jxta.content.ContentTransferState;
import net.jxta.content.TransferException;
import net.jxta.document.Document;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.protocol.ContentShareAdvertisement;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the workings of the TransferAggregator class.
 */
@RunWith(JMock.class)
public class TransferAggregatorTest extends TestCase {
    private static Logger LOG =
            Logger.getLogger(TransferAggregatorTest.class.getName());
    private TransferAggregator aggregator;
    private ContentTransferAggregatorListener aggListener;
    private ContentTransferListener listener;
    private ContentProviderSPI provider1;
    private ContentProviderSPI provider2;
    private ContentProviderSPI provider3;
    private ContentProviderSPI provider4;
    private List<ContentProviderSPI> providers =
            new CopyOnWriteArrayList<ContentProviderSPI>();
    private ContentTransfer transfer1;
    private ContentTransfer transfer2;
    private ContentTransfer transfer3;
    private ContentTransfer transfer4;
    private List<ContentTransfer> transfers =
            new CopyOnWriteArrayList<ContentTransfer>();

    private ContentTransfer selected;
    private ContentTransfer standby;

    private Content content;

    private Mockery context = new Mockery();

    /**
     * Default constructor.
     */
    public TransferAggregatorTest() {
    }

    @Before
    @Override
    public void setUp() throws Exception {
        LOG.info("===========================================================");
        listener = context.mock(ContentTransferListener.class);
        aggListener = context.mock(ContentTransferAggregatorListener.class);
        transfer1 = context.mock(ContentTransfer.class, "transfer1");
        transfer2 = context.mock(ContentTransfer.class, "transfer2");
        transfer3 = context.mock(ContentTransfer.class, "transfer3");
        transfer4 = context.mock(ContentTransfer.class, "transfer4");
        transfers.add(transfer1);
        transfers.add(transfer2);
        transfers.add(transfer3);
        transfers.add(transfer4);
        provider1 = context.mock(ContentProviderSPI.class, "provider1");
        provider2 = context.mock(ContentProviderSPI.class, "provider2");
        provider3 = context.mock(ContentProviderSPI.class, "provider3");
        provider4 = context.mock(ContentProviderSPI.class, "provider4");
        providers.add(provider1);
        providers.add(provider2);
        providers.add(provider3);
        providers.add(provider4);

        PeerGroupID peerGroupID = IDFactory.newPeerGroupID();
        ContentID contentID = IDFactory.newContentID(peerGroupID, true);
        Document document = StructuredDocumentFactory.newStructuredDocument(
                MimeMediaType.TEXTUTF8, "foo", "bar");
        content = new  Content(contentID, null, document);
    }

    @After
    @Override
    public void tearDown() {
        Thread.yield();
        System.out.flush();
        System.err.flush();
    }

    @Test
    public void testConstructionWithNoProviders() throws TransferException {
        context.checking(new Expectations() {{
            one(provider1).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(null));

            one(provider2).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(null));

            one(provider3).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(null));

            one(provider4).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(null));
        }});

        try {
            aggregator = new TransferAggregator(null,
                providers, (ContentShareAdvertisement) null);
            fail("TransferException was not thrown");
        } catch (TransferException transx) {
            /*
             * For some reason @Test(expected=TransferException.class)
             * is not working...
             */
        }
    }

    @Test
    public void testConstruction() throws Exception {
        context.checking(new Expectations() {{
            one(provider1).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(transfer1));

            one(provider2).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(transfer2));

            one(provider3).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(transfer3));

            one(provider4).retrieveContent((ContentShareAdvertisement)null);
            will(returnValue(transfer4));

            one(transfer1).addContentTransferListener(
                    with(any(TransferAggregator.class)));
            one(transfer2).addContentTransferListener(
                    with(any(TransferAggregator.class)));
            one(transfer3).addContentTransferListener(
                    with(any(TransferAggregator.class)));
            one(transfer4).addContentTransferListener(
                    with(any(TransferAggregator.class)));
        }});

        aggregator = new TransferAggregator(null,
                providers, (ContentShareAdvertisement) null);
        aggregator.addContentTransferAggregatorListener(aggListener);
        aggregator.addContentTransferAggregatorListener(
                new ContentTransferAggregatorListener() {
            public void selectedContentTransfer(
                    ContentTransferAggregatorEvent ctaEvent) {
                selected = ctaEvent.getDelegateContentTransfer();
            }

            public void updatedContentTransferList(
                    ContentTransferAggregatorEvent ctaEvent) {
                // Ignore
            }

        });
        aggregator.addContentTransferListener(listener);

        context.assertIsSatisfied();
    }

    @Test
    public void testRandomization() throws Exception {
        int last = -1;
        int same = 0;
        int total = 0;

        for (int i=0; i<10; i++) {
            testConstruction();
            List<ContentTransfer> list = new ArrayList<ContentTransfer>(
                    aggregator.getContentTransferList());

            assertTrue(list.contains(transfer1));
            assertTrue(list.contains(transfer2));
            assertTrue(list.contains(transfer3));
            assertTrue(list.contains(transfer4));
            assertEquals(4, list.size());

            int value = 0;
            for (ContentTransfer transfer : list) {
                value *= 10;
                if (transfer == transfer1) {
                    value += 1;
                } else if (transfer == transfer2) {
                    value += 2;
                } else if (transfer == transfer3) {
                    value += 3;
                } else {
                    value += 4;
                }
            }
            LOG.info("Last : " + last);
            LOG.info("Value: " + value);

            if (last > 0) {
                total++;
                if (last == value) {
                    same++;
                }
            }
            last = value;
        }

        assertTrue("Element ordering was not sufficiently random (same=" +
                same + ", total=" + total + ")", ((same / total) < 0.5F));

        context.assertIsSatisfied();
    }

    @Test
    public void testStartSourceLocation() throws Exception {
        testConstruction();
        List<ContentTransfer> xfers = new ArrayList<ContentTransfer>(
                aggregator.getContentTransferList());
        final ContentTransfer first = xfers.remove(0);
        final ContentTransfer second = xfers.remove(0);
        final ContentTransfer third = xfers.remove(0);
        final Sequence firstSeq = context.sequence("selected transfer");
        final Sequence secondSeq = context.sequence("standby1 transfer");
        final Sequence thirdSeq = context.sequence("standby2 transfer");
        standby = second;


        context.checking(new Expectations() {{
            one(first).getTransferState();
            will(returnValue(ContentTransferState.PENDING));
            inSequence(firstSeq);

            one(aggListener).selectedContentTransfer(
                    with(any(ContentTransferAggregatorEvent.class)));
            inSequence(firstSeq);

            one(first).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.NOT_LOCATING));
            inSequence(firstSeq);

            one(first).startSourceLocation();
            inSequence(firstSeq);

            one(second).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.NOT_LOCATING));
            inSequence(secondSeq);

            one(second).startSourceLocation();
            inSequence(secondSeq);

            one(third).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.NOT_LOCATING));
            inSequence(thirdSeq);

            one(third).startSourceLocation();
            inSequence(thirdSeq);

        }});

        aggregator.startSourceLocation();
        assertSame("selected",
                first, selected);
        assertSame("getCurrentContentTransfer",
                first, aggregator.getCurrentContentTransfer());

        context.assertIsSatisfied();
    }

    @Test
    public void testSelectedLocationStateHasEnough() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent = new ContentTransferEvent(
                selected, 100, ContentSourceLocationState.LOCATING_HAS_ENOUGH,
                ContentTransferState.PENDING);

        context.checking(new Expectations() {{
            one(listener).contentLocationStateUpdated(
                    with(any(ContentTransferEvent.class)));
            one(selected).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_ENOUGH));
            // Location keeps going...
        }});

        aggregator.contentLocationStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testSelectedLocationStateHasMany() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent = new ContentTransferEvent(
                selected, 100, ContentSourceLocationState.LOCATING_HAS_MANY,
                ContentTransferState.PENDING);

        context.checking(new Expectations() {{
            one(listener).contentLocationStateUpdated(
                    with(any(ContentTransferEvent.class)));
            one(selected).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_MANY));
            one(selected).stopSourceLocation();
        }});

        aggregator.contentLocationStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testStandbyLocationStateHasEnough() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent = new ContentTransferEvent(
                standby, 100, ContentSourceLocationState.LOCATING_HAS_ENOUGH,
                ContentTransferState.PENDING);

        context.checking(new Expectations() {{
            one(standby).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_ENOUGH));
            one(standby).stopSourceLocation();
        }});

        aggregator.contentLocationStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testStandbyLocationStateHasMany() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent = new ContentTransferEvent(
                standby, 100, ContentSourceLocationState.LOCATING_HAS_MANY,
                ContentTransferState.PENDING);

        context.checking(new Expectations() {{
            one(standby).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_MANY));
            one(standby).stopSourceLocation();
        }});

        aggregator.contentLocationStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

    @Test
    public void testStandbyTransferCompletion() throws Exception {
        testStartSourceLocation();
        final ContentTransferEvent ctEvent = new ContentTransferEvent(
                standby, 100, ContentSourceLocationState.LOCATING_HAS_MANY,
                ContentTransferState.COMPLETED);

        context.checking(new Expectations() {{
            for (ContentTransfer transfer : transfers) {
                if (transfer != selected && transfer != standby) {
                    ignoring(transfer);
                }
            }

            ignoring(listener);

            one(standby).getContent();
            will(returnValue(content));

            one(standby).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.LOCATING_HAS_MANY));

            one(standby).stopSourceLocation();
            one(standby).removeContentTransferListener(
                    with(any(ContentTransferListener.class)));

            one(selected).getSourceLocationState();
            will(returnValue(ContentSourceLocationState.NOT_LOCATING_HAS_MANY));

            one(selected).stopSourceLocation();
            one(selected).removeContentTransferListener(
                    with(any(ContentTransferListener.class)));
            one(selected).cancel();
        }});

        aggregator.contentTransferStateUpdated(ctEvent);

        context.assertIsSatisfied();
    }

}
