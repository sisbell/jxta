package net.jxta.impl.cm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.XMLDocument;
import net.jxta.impl.util.JxtaHash;
import net.jxta.impl.util.TimeUtils;
import net.jxta.impl.util.threads.TaskManager;
import net.jxta.impl.xindice.core.DBException;
import net.jxta.impl.xindice.core.data.Key;
import net.jxta.impl.xindice.core.data.Record;
import net.jxta.impl.xindice.core.data.Value;
import net.jxta.impl.xindice.core.filer.BTreeCallback;
import net.jxta.impl.xindice.core.filer.BTreeFiler;
import net.jxta.impl.xindice.core.indexer.IndexQuery;
import net.jxta.impl.xindice.core.indexer.NameIndexer;
import net.jxta.logging.Logging;
import net.jxta.protocol.PeerAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.SrdiMessage;

/**
 * This is the original Cm implementation.
 */
public class XIndiceAdvertisementCache extends AbstractAdvertisementCache implements  AdvertisementCache {

    /**
     * Logger.
     */
    final static Logger LOG = Logger.getLogger(Cm.class.getName());
    /**
     * adv types
     */
    private final static String[] DIRNAME = {"Peers", "Groups", "Adv", "Raw"};
    /**
     * Default period in milliseconds at which expired record GC will occur.
     */
    public final static long DEFAULT_GC_MAX_INTERVAL = TimeUtils.ANHOUR;
    /**
     * Period in milliseconds at which we will check to see if it is time to GC.
     */
    private final static long GC_CHECK_PERIOD = TimeUtils.AMINUTE;
    /**
     * Alternative to GC. If we accumulate this many changes without a GC then
     * we start the GC early.
     */
    private final static int MAX_INCONVENIENCE_LEVEL = 1000;
    private final static String DATABASE_FILE_NAME = "advertisements";
    /**
     * Shared timer for scheduling GC tasks.
     */
    private final static Timer GC_TIMER = new Timer("CM GC Timer", true);
    /**
     * the name we will use for the base directory
     */
    private final File ROOTDIRBASE;
    /**
     * The Executor we use for our tasks.
     */
    private final ScheduledExecutorService executor;
    /*
     *  record db
     */
    private final BTreeFiler cacheDB;
    /**
     * Record indexer.
     */
    private final Indexer indexer;
    /**
     * If {@code true} then we will track changes to the indexes.
     */
    private boolean trackDeltas;
    /**
     * The current set of database changes we have accumulated.
     */
    private final Map<String, List<SrdiMessage.Entry>> deltaMap = new HashMap<String, List<SrdiMessage.Entry>>(3);

    /**
     * file descriptor for the root of the cm
     */
    protected final File rootDir;
    /**
     * If {@code true} then this cache has been stopped.
     */
    private boolean stop = false;
    /**
     * The scheduler for our GC operations.
     */
    private final ScheduledFuture<?> gcTaskHandle;

    /**
     * The absolute time in milliseconds after which the next GC operation will
     * begin.
     */
    private long gcTime = 0;
    /**
     * The maximum period between GC operations.
     */
    private final long gcMaxInterval;
    /**
     * Measure of accumulated number of record changes since the last GC. If
     * this reaches {@link #MAX_INCONVENIENCE_LEVEL} then the GC operation will
     * be started early.
     */
    private AtomicInteger inconvenienceLevel = new AtomicInteger(0);

    /**
     * Constructor for cm
     *
     * @param areaName  the name of the cm sub-dir to create
     *                  <p/>
     *                  NOTE: Default garbage interval once an hour
     * @param storeRoot store root dir
     * @throws IOException 
     */
	public XIndiceAdvertisementCache(URI storeRoot, String areaName) throws IOException {
		// XXX I'm not sure how to support an arbitrary executor in this context.
		// Granted, it's only used in tests right now, but clearly we dont need
		// yet another pool floating around.  SingleThreadExecutor seems safest...
		// however, the current implementation uses CachedThreadPool.
		this(storeRoot, areaName, DEFAULT_GC_MAX_INTERVAL, false);
	}

    /**
     * Constructor for cm
     *
     * @param storeRoot   persistence location
     * @param areaName    storage area name
     * @param gcInterval  garbage collect max interval in milliseconds or &lt;= 0 to use default value.
     * @param trackDeltas when true deltas are tracked
     * @throws IOException thrown for failures initilzing the CM store.
     */
    public XIndiceAdvertisementCache(URI storeRoot, String areaName, long gcInterval, boolean trackDeltas) throws IOException {
        this.executor = TaskManager.getTaskManager().getScheduledExecutorService();
        this.trackDeltas = trackDeltas;
        this.gcMaxInterval = (0 >= gcInterval) ? DEFAULT_GC_MAX_INTERVAL : gcInterval;

        ROOTDIRBASE = new File(new File(storeRoot), "cm");

        try {
            rootDir = new File(new File(ROOTDIRBASE, areaName).getAbsolutePath());
            if (!rootDir.exists()) {
                // We need to create the directory
                if (!rootDir.mkdirs()) {
                    throw new IOException("Cm cannot create directory " + rootDir);
                }
            }

            /*
             * to avoid inconsistent database state, it is highly recommended that
             * checkpoint is true by default, which causes fd.sync() on every write
             * operation.  In transitory caches such as SrdiCache it makes perfect sense
             */
            boolean chkPoint = true;
            ResourceBundle jxtaRsrcs = ResourceBundle.getBundle("net.jxta.user");
            String checkpointStr = jxtaRsrcs.getString("impl.cm.defferedcheckpoint");

            if (checkpointStr != null) {
                chkPoint = !(checkpointStr.equalsIgnoreCase("true"));
            }

            // Storage
            cacheDB = new BTreeFiler();
            // no deffered checkpoint
            cacheDB.setSync(chkPoint);
            cacheDB.setLocation(rootDir.getAbsolutePath(), DATABASE_FILE_NAME);

            if (!cacheDB.open()) {
                cacheDB.create();
                // now open it
                cacheDB.open();
            }

            // Index
            indexer = new Indexer(chkPoint);
            indexer.setLocation(rootDir.getAbsolutePath(), DATABASE_FILE_NAME);

            if (!indexer.open()) {
                indexer.create();
                // now open it
                indexer.open();
            }

            if (System.getProperty("net.jxta.impl.cm.index.rebuild") != null) {
                rebuildIndex();
            }

            // Install Record GC task.
            gcTime = TimeUtils.toAbsoluteTimeMillis(gcMaxInterval);
            gcTaskHandle = executor.scheduleAtFixedRate(new GC_Task(), GC_CHECK_PERIOD, GC_CHECK_PERIOD, TimeUnit.SECONDS);

            if (Logging.SHOW_CONFIG && LOG.isLoggable(Level.CONFIG)) {
                LOG.config("Instantiated Cm for: " + rootDir.getAbsolutePath());
            }
        } catch (DBException de) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Unable to Initialize databases", de);
            }
            IOException failure = new IOException("Unable to Initialize databases");
            failure.initCause(de);

            throw failure;
        }
    }

    @Override
    public String toString() {
        return "CM for " + rootDir.getAbsolutePath() + "[" + super.toString() + "]";
    }

    private static String getDirName(Advertisement adv) {
        if (adv instanceof PeerAdvertisement) {
            return DIRNAME[DiscoveryService.PEER];
        } else if (adv instanceof PeerGroupAdvertisement) {
            return DIRNAME[DiscoveryService.GROUP];
        }
        return DIRNAME[DiscoveryService.ADV];
    }

    /**
     * Generates a random file name using doc hashcode
     *
     * @param doc to hash to generate a unique name
     * @return String a random file name
     */
    public static String createTmpName(StructuredTextDocument doc) {
        try {
            StringWriter out = new StringWriter();

            doc.sendToWriter(out);
            out.close();

            JxtaHash digester = new JxtaHash(out.toString());
            BigInteger hash = digester.getDigestInteger();

            if (hash.compareTo(BigInteger.ZERO) < 0) {
                hash = hash.negate();
            }
            return "cm" + hash.toString(16);
        } catch (IOException ex) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Exception creating tmp name: ", ex);
            }
            throw new IllegalStateException("Could not generate name from document");
        }
    }

    /**
     * Gets the list of all the files into the given folder
     *
     * @param dn          contains the name of the folder
     * @param threshold   the max number of results
     * @param expirations List to contain expirations
     * @return List<InputStream> containing the files
     */
    public List<InputStream> getRecords(String dn, int threshold, List<Long> expirations) {
        return getRecords(dn, threshold, expirations, false);
    }

    public synchronized List<InputStream> getRecords(String dn, int threshold, List<Long> expirations, boolean purge) {
    	ArrayList<InputStream> res = new ArrayList<InputStream>();

    	if (dn == null) {
    		if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
    			LOG.fine("null directory name");
    		}
    		return res;
    	} else {
    		IndexQuery iq = new IndexQuery(IndexQuery.SW, new Value(dn+'/'));
    		try {
    			SearchCallback callback = new SearchCallback(cacheDB, indexer, threshold, purge);
    			cacheDB.query(iq, callback);

    			Collection<SearchResult> searchResults = callback.results;

    			res.ensureCapacity(searchResults.size());
    			if (null != expirations) {
    				expirations.clear();
    			}

    			for (SearchResult aResult : searchResults) {
    				res.add(aResult.value.getInputStream());
    				if (null != expirations) {
    					expirations.add(aResult.expiration);
    				}

    			}
    		} catch (DBException dbe) {
    			if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
    				LOG.log(Level.FINE, "Exception during getRecords(): ", dbe);
    			}
    		} catch (IOException ie) {
    			if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
    				LOG.log(Level.FINE, "Exception during getRecords(): ", ie);
    			}
    		}

    		return res;
    	}
    }
    public synchronized void garbageCollect() {
        // calling getRecords() is good enough since it removes expired entries
        Map<String, NameIndexer> map = indexer.getIndexers();

        for (String indexName : map.keySet()) {
            long t0 = TimeUtils.timeNow();
            getRecords(indexName, Integer.MAX_VALUE, null, true);
            if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                LOG.finer("Cm garbageCollect :" + indexName + " in :" + (TimeUtils.timeNow() - t0));
            }
        }
    }

    /**
     * Returns the relative time in milliseconds at which the file
     * will expire.
     *
     * @param dn contains the name of the folder
     * @param fn contains the name of the file
     * @return the absolute time in milliseconds at which this
     *         document will expire. -1 is returned if the file is not
     *         recognized or already expired.
     */
    public synchronized long getLifetime(String dn, String fn) {
        try {
            Key key = new Key(dn + "/" + fn);
            Record record = cacheDB.readRecord(key);

            if (record == null) {
                return -1;
            }
            Long life = (Long) record.getMetaData(Record.LIFETIME);

            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Lifetime for :" + fn + "  " + life.toString());
            }
            if (life < TimeUtils.timeNow()) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Removing expired record :" + fn);
                }
                try {
                    remove(dn, fn);
                } catch (IOException e) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Failed to remove record", e);
                    }
                }
            }
            return TimeUtils.toRelativeTimeMillis(life);
        } catch (DBException de) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "failed to remove " + dn + "/" + fn, de);
            }
            return -1;
        }
    }

    /**
     * Returns the maximum duration in milliseconds for which this
     * document should cached by those other than the publisher. This
     * value is either the cache lifetime or the remaining lifetime
     * of the document, whichever is less.
     *
     * @param dn contains the name of the folder
     * @param fn contains the name of the file
     * @return number of milliseconds until the file expires or -1 if the
     *         file is not recognized or already expired.
     */
    public synchronized long getExpirationtime(String dn, String fn) {
        try {
            Key key = new Key(dn + "/" + fn);
            Record record = cacheDB.readRecord(key);
            long expiration = calcExpiration(record);

            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Expiration for :" + fn + "  " + expiration);
            }
            if (expiration < 0) {
                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Removing expired record :" + fn);
                }
                try {
                    remove(dn, fn);
                } catch (IOException e) {
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Failed to remove record", e);
                    }
                }
            }
            return expiration;
        } catch (DBException de) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "failed to get " + dn + "/" + fn, de);
            }
            return -1;
        }
    }

    /**
     * Figures out expiration
     *
     * @param record record
     * @return expiration in ms
     */
    private static long calcExpiration(Record record) {
        if (record == null) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Record is null returning expiration of -1");
            }
            return -1;
        }
        Long exp = (Long) record.getMetaData(Record.EXPIRATION);
        Long life = (Long) record.getMetaData(Record.LIFETIME);
        
        long expiresin = TimeUtils.toRelativeTimeMillis(life);

        if (expiresin <= 0) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(
                        MessageFormat.format("Record expired lifetime   : {0} expiration: {1} expires in: {2}", life, exp, expiresin));
                LOG.fine(MessageFormat.format("Record expired on :{0}", new Date(life)));
            }
            return -1;
        } else {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(MessageFormat.format("Record lifetime: {0} expiration: {1} expires in: {2}", life, exp, expiresin));
                LOG.fine(MessageFormat.format("Record expires on :{0}", new Date(life)));
            }
            return Math.min(expiresin, exp);
        }
    }

    /**
     * Returns the inputStream of a specified file, in a specified dir
     *
     * @param dn directory name
     * @param fn file name
     * @return The inputStream value
     * @throws IOException if an I/O error occurs
     */
    public InputStream getInputStream(String dn, String fn) throws IOException {
        Key key = new Key(dn + "/" + fn);
        try {
            Record record = cacheDB.readRecord(key);
            if (record == null) {
                return null;
            }
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Restored record for " + key);
            }
            Value val = record.getValue();

            if (val != null) {
                return val.getInputStream();
            } else {
                return null;
            }
        } catch (DBException de) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Failed to restore record for " + key, de);
            }
            IOException failure = new IOException("Failed to restore record for " + key);
            failure.initCause(de);
            throw failure;
        }
    }

    /**
     * Remove a file
     *
     * @param dn directory name
     * @param fn file name
     * @throws IOException if an I/O error occurs
     */
    public synchronized void remove(String dn, String fn) throws IOException {

        try {
            if (fn == null) {
                return;
            }
            Key key = new Key(dn + "/" + fn);
            Record record = cacheDB.readRecord(key);
            long removePos = cacheDB.findValue(key);

            cacheDB.deleteRecord(key);
            if (record != null) {
                try {
                    if (calcExpiration(record) > 0) {
                        InputStream is = record.getValue().getInputStream();
                        XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, is);
                        Advertisement adv = AdvertisementFactory.newAdvertisement(asDoc);
                        Map<String, String> indexables = CacheUtils.getIndexfields(adv.getIndexFields(), asDoc);

                        indexer.removeFromIndex(addKey(dn, indexables), removePos);
                        // add it to deltas to expire it in srdi
                        addDelta(dn, indexables, 0);
                        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                            LOG.fine("removed " + record);
                        }
                    }
                }catch (Exception e) {
                    // bad bits we are done
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "failed to remove " + dn + "/" + fn, e);
                    }
                }
            }
        } catch (DBException de) {
            // entry does not exist
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("failed to remove " + dn + "/" + fn);
            }
        }
    }

    /** NEW
     * Restore a saved StructuredDocument.
     *
     * @param dn directory name
     * @param fn file name
     * @return StructuredDocument containing the file
     * @throws IOException if an I/O error occurs
     *                     was not possible.
     */
    public StructuredDocument restore(String dn, String fn) throws IOException {
        InputStream is = getInputStream(dn, fn);
        return StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, is);
    }

    /**
     * Restore an advertisement into a byte array.
     *
     * @param dn directory name
     * @param fn file name
     * @return byte [] containing the file
     * @throws IOException if an I/O error occurs
     */
    public synchronized byte[] restoreBytes(String dn, String fn) throws IOException {

        try {
            Key key = new Key(dn + "/" + fn);
            Record record = cacheDB.readRecord(key);

            if (record == null) {
                return null;
            }
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("restored " + record);
            }
            Value val = record.getValue();

            if (val != null) {
                return val.getData();
            } else {
                return null;
            }
        } catch (DBException de) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "failed to restore " + dn + "/" + fn, de);
            }
            IOException failure = new IOException("failed to restore " + dn + "/" + fn);
            failure.initCause(de);
            throw failure;
        }
    }

    /** NEW
     * Stores a StructuredDocument in specified dir, and file name
     *
     * @param dn  directory name
     * @param fn  file name
     * @param adv Advertisement to store
     * @throws IOException if an I/O error occurs
     */
    public void save(String dn, String fn, Advertisement adv) throws IOException {
        save(dn, fn, adv, DiscoveryService.INFINITE_LIFETIME, DiscoveryService.NO_EXPIRATION);
    }

    /**
     * Stores a StructuredDocument in specified dir, and file name, and
     * associated doc timeouts
     *
     * @param dn         directory name
     * @param fn         file name
     * @param adv        Advertisement to save
     * @param lifetime   Document (local) lifetime in relative ms
     * @param expiration Document (global) expiration time in relative ms
     * @throws IOException Thrown if there is a problem saving the document.
     */
    public synchronized void save(String dn, String fn, Advertisement adv, long lifetime, long expiration) throws IOException {

        try {
            if (expiration < 0 || lifetime <= 0) {
                throw new IllegalArgumentException("Bad expiration or lifetime.");
            }
            XMLDocument doc;

            try {
                doc = (XMLDocument) adv.getDocument(MimeMediaType.XMLUTF8);
            } catch (RuntimeException e) {
                IOException failure = new IOException("Advertisement couldn't be saved");
                failure.initCause(e);
                throw failure;
            }

            Key key = new Key(dn + "/" + fn);
            // save the new version
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            doc.sendToStream(baos);
            baos.close();
            Value value = new Value(baos.toByteArray());
            Long oldLife = null;
            Record record = cacheDB.readRecord(key);

            if (record != null) {
                // grab the old lifetime
                oldLife = (Long) record.getMetaData(Record.LIFETIME);
            }

            long absoluteLifetime = TimeUtils.toAbsoluteTimeMillis(lifetime);


            if (oldLife != null) {
                if (absoluteLifetime < oldLife) {
                    // make sure we don't override the original value
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine(MessageFormat.format("Overriding attempt to decrease adv lifetime from : {0} to :{1}",
                                new Date(oldLife), new Date(absoluteLifetime)));
                    }
                    absoluteLifetime = oldLife;
                }
            }
            // make sure expiration does not exceed lifetime
            if (expiration > lifetime) {
                expiration = lifetime;
            }

            long pos = cacheDB.writeRecord(key, value, absoluteLifetime, expiration);
            Map<String, String> indexables = CacheUtils.getIndexfields(adv.getIndexFields(), doc);
            Map<String, String> keyedIdx = addKey(dn, indexables);

            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Indexing " + keyedIdx + " at " + pos);
            }
            indexer.addToIndex(keyedIdx, pos);

            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                // too noisy
                // LOG.debug("Wrote " + key + " = " + value);
                LOG.fine("Stored " + indexables + " at " + pos);
            }

            if (expiration > 0) {
                // Update for SRDI with our caches lifetime only if we are prepared to share the advertisement with others.
                addDelta(dn, indexables, TimeUtils.toRelativeTimeMillis(absoluteLifetime));
            }

        } catch (DBException de) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, MessageFormat.format("Failed to write {0}/{1} {2} {3}", dn, fn, lifetime, expiration), de);
            }
            IOException failure = new IOException("Failed to write " + dn + "/" + fn + " " + lifetime + " " + expiration);
            failure.initCause(de);
            throw failure;
        }
    }

    /**
     * Store some bytes in specified dir, and file name, and
     * associated doc timeouts
     *
     * @param dn         directory name
     * @param fn         file name
     * @param data       byte array to save
     * @param lifetime   Document (local) lifetime in relative ms
     * @param expiration Document (global) expiration time in relative ms
     * @throws IOException Thrown if there is a problem saving the document.
     */
    public synchronized void save(String dn, String fn, byte[] data, long lifetime, long expiration) throws IOException {

        try {
            if (expiration < 0 || lifetime <= 0) {
                throw new IllegalArgumentException("Bad expiration or lifetime.");
            }

            Key key = new Key(dn + "/" + fn);
            Value value = new Value(data);
            Long oldLife = null;
            Record record = cacheDB.readRecord(key);

            if (record != null) {
                // grab the old lifetime
                oldLife = (Long) record.getMetaData(Record.LIFETIME);
            }

            // save the new version

            long absoluteLifetime = TimeUtils.toAbsoluteTimeMillis(lifetime);

            if (oldLife != null) {
                if (absoluteLifetime < oldLife) {
                    // make sure we don't override the original value
                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine(MessageFormat.format("Overriding attempt to decrease adv lifetime from : {0} to :{1}",
                                new Date(oldLife), new Date(absoluteLifetime)));
                    }
                    absoluteLifetime = oldLife;
                }
            }

            // make sure expiration does not exceed lifetime
            if (expiration > lifetime) {
                expiration = lifetime;
            }
            cacheDB.writeRecord(key, value, absoluteLifetime, expiration);
        } catch (DBException de) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Failed to write " + dn + "/" + fn + " " + lifetime + " " + expiration, de);
            }

            IOException failure = new IOException("Failed to write " + dn + "/" + fn + " " + lifetime + " " + expiration);
            failure.initCause(de);
            throw failure;
        }
    }

    /* adds a primary index 'dn' to indexables */
    private static Map<String, String> addKey(String dn, Map<String, String> map) {
        if (map == null) {
            return null;
        }

        Map<String, String> tmp = new HashMap<String, String>();
        if (map.size() > 0) {
            Iterator<String> it = map.keySet().iterator();

            while (it != null && it.hasNext()) {
                String name = it.next();

                tmp.put(dn + name, map.get(name));
            }
        }
        return tmp;
    }

    private static final class EntriesCallback implements BTreeCallback {

        private BTreeFiler cacheDB = null;
        private int threshold;
        private List<SrdiMessage.Entry> results;
        private String key;

        EntriesCallback(BTreeFiler cacheDB, List<SrdiMessage.Entry> results, String key, int threshold) {
            this.cacheDB = cacheDB;
            this.results = results;
            this.key = key;
            this.threshold = threshold;
        }

        /**
         * {@inheritDoc}
         */
        public boolean indexInfo(Value val, long pos) {
            if (results.size() >= threshold) {
                return false;
            }
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Found " + val.toString() + " at " + pos);
            }

            Record record;
            try {
                record = cacheDB.readRecord(pos);
            } catch (DBException ex) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Exception while reading indexed", ex);
                }
                return false;
            }
            if (record == null) {
                return true;
            }
            long exp = calcExpiration(record);

            if (exp <= 0) {
                // skip expired and private entries
                return true;
            }
            Long life = (Long) record.getMetaData(Record.LIFETIME);
            SrdiMessage.Entry entry = new SrdiMessage.Entry(key, val.toString(), life - TimeUtils.timeNow());

            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(" key [" + entry.key + "] value [" + entry.value + "] exp [" + entry.expiration + "]");
            }
            results.add(entry);
            return true;
        }
    }

    private final static class SearchResult {

        final Value value;
        final long expiration;

        SearchResult(Value value, long expiration) {
            this.value = value;
            this.expiration = expiration;
        }
    }

    private final class SearchCallback implements BTreeCallback {

        private final BTreeFiler cacheDB;
        private final Indexer indexer;
        private final int threshold;
        private final Collection<SearchResult> results;
        private final boolean purge;

        SearchCallback(BTreeFiler cacheDB, Indexer indexer, int threshold) {
            this(cacheDB, indexer, threshold, false);
        }

        SearchCallback(BTreeFiler cacheDB, Indexer indexer, int threshold, boolean purge) {
            this.cacheDB = cacheDB;
            this.indexer = indexer;
//            this.results = results;
            this.threshold = threshold;
            this.results = new ArrayList((threshold < 200) ? threshold : 200);
            this.purge = purge;
        }

        /**
         * {@inheritDoc}
         */
        public boolean indexInfo(Value val, long pos) {
            if (results.size() >= threshold) {
                if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                    LOG.finer("SearchCallback.indexInfo reached Threshold :" + threshold);
                }
                return false;
            }
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine("Found " + val.toString() + " at " + pos);
            }

            Record record;
            try {
                record = cacheDB.readRecord(pos);
            } catch (DBException ex) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Exception while reading indexed", ex);
                }
                return false;
            }

            if (record == null) {
                return true;
            }

            if (Logging.SHOW_FINEST && LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Search callback record " + record.toString());
            }
            long exp = calcExpiration(record);
            if (exp < 0) {
                if (purge) {
                    try {
                        indexer.purge(pos);
                        cacheDB.deleteRecord(record.getKey());
                    } catch (DBException ex) {
                        if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, "Exception while reading indexed", ex);
                        }
                    } catch (IOException ie) {
                        if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                            LOG.log(Level.WARNING, "Exception while reading indexed", ie);
                        }
                    }
                } else {
                    inconvenienceLevel.incrementAndGet();
                }
                return true;
            }
            results.add(new SearchResult(record.getValue(), exp));

            return true;
        }
    }

    protected static IndexQuery getIndexQuery(String value) {

        int operator;

        if (value == null) {
            return null;
        } else if (value.length() == 0 || "*".equals(value)) {
            return null;
        } else if (value.indexOf("*") < 0) {
            operator = IndexQuery.EQ;
        } else if (value.charAt(0) == '*' && value.charAt(value.length() - 1) != '*') {
            operator = IndexQuery.EW;
            value = value.substring(1, value.length());
        } else if (value.charAt(value.length() - 1) == '*' && value.charAt(0) != '*') {
            operator = IndexQuery.SW;
            value = value.substring(0, value.length() - 1);
        }
        else {
            operator = IndexQuery.BWX;
            value = value.substring(1, value.length() - 1);
        }
        if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Index query operator :" + operator);
        }
        return new IndexQuery(operator, new Value(value));
    }

    /**
     * Search and recovers documents that contains at least
     * a macthing pair of tag/value.
     *
     * @param dn          contains the name of the folder on which to
     *                    perform the search
     * @param value       contains the value to search on.
     * @param attribute   attribute to search on
     * @param threshold   threshold
     * @param expirations List to contain expirations
     * @return Enumeration containing of all the documents as InputStreams
     */
    public synchronized List<InputStream> search(String dn, String attribute, String value, int threshold, List<Long> expirations) {

        try {
            IndexQuery iq = getIndexQuery(value);

            SearchCallback callback = new SearchCallback(cacheDB, indexer, threshold);
            indexer.search(iq, dn + attribute, callback);
            Collection<SearchResult> searchResults = callback.results;

            List<InputStream> res = new ArrayList<InputStream>(searchResults.size());
            if (null != expirations) {
                expirations.clear();
            }

            for (SearchResult aResult : searchResults) {
                res.add(aResult.value.getInputStream());
                if (null != expirations) {
                    expirations.add(aResult.expiration);
                }
            }

            return res;
        } catch (Exception ex) {
            if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, "Failure while searching in index", ex);
            }

            return Collections.emptyList();
        }
    }

    /**
     * returns all entries that are cached
     *
     * @param dn          the relative dir name
     * @param clearDeltas if true clears the delta cache
     * @return SrdiMessage.Entries
     */
    public synchronized List<SrdiMessage.Entry> getEntries(String dn, boolean clearDeltas) {
        List<SrdiMessage.Entry> res = new ArrayList<SrdiMessage.Entry>();
        try {
            Map<String, NameIndexer> map = indexer.getIndexers();
            BTreeFiler listDB = indexer.getListDB();
//            Iterator it = map.keySet().iterator();

            for (String indexName : map.keySet()) {

                // seperate the index name from attribute
                if (indexName.startsWith(dn)) {
                    String attr = indexName.substring(dn.length());
                    NameIndexer idxr = map.get(indexName);
                	System.err.println("Checking indexname " + indexName + " with dn " + dn + " with attr " + attr );
                    idxr.query(null, new Indexer.SearchCallback(listDB, new EntriesCallback(cacheDB, res, attr, Integer.MAX_VALUE)));
                }
            }
        } catch (Exception ex) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Exception while searching in index", ex);
            }
        }

        if (clearDeltas) {
            clearDeltas(dn);
        }
        return res;
    }

    /**
     * returns all entries that are added since this method was last called
     *
     * @param dn the relative dir name
     * @return SrdiMessage.Entries
     */
    public List<SrdiMessage.Entry> getDeltas(String dn) {
        List<SrdiMessage.Entry> result = new ArrayList<SrdiMessage.Entry>();
//        List<SrdiMessage.Entry> deltas = deltaMap.get(dn);

        synchronized (deltaMap) {
            List<SrdiMessage.Entry> deltas = deltaMap.get(dn);

            if (deltas != null) {
                result.addAll(deltas);
                deltas.clear();
            }
        }
        
        return result;
    }

    /**
     * Clear all the SRDI message entries for the specified directory.
     *
     * @param dn the relative dir name
     */
    private void clearDeltas(String dn) {
        synchronized (deltaMap) {
            List<SrdiMessage.Entry> deltas = deltaMap.get(dn);

            if (deltas != null) {
                deltas.clear();
            }
        }
    }

    private void addDelta(String dn, Map<String, String> indexables, long exp) {
        if (trackDeltas) {
            List<SrdiMessage.Entry> newDeltas = new ArrayList<SrdiMessage.Entry>(indexables.size());

            for (Map.Entry<String, String> anEntry : indexables.entrySet()) {
                SrdiMessage.Entry entry = new SrdiMessage.Entry(anEntry.getKey(), anEntry.getValue(), exp);
                newDeltas.add(entry);
            }

            if (Logging.SHOW_FINER && LOG.isLoggable(Level.FINER)) {
                LOG.finer("Adding " + newDeltas.size() + "entires to '" + dn + "' deltas");
            }

            synchronized (deltaMap) {
                List<SrdiMessage.Entry> deltas = deltaMap.get(dn);

                if (deltas == null) {
                    deltaMap.put(dn, newDeltas);
                } else {
                    deltas.addAll(newDeltas);
                }
            }
        }
    }

    public void setTrackDeltas(boolean trackDeltas) {
        this.trackDeltas = trackDeltas;
        synchronized (deltaMap) {
            if (!trackDeltas) {
                deltaMap.clear();
            }
        }
    }

    /**
     * stop the cm
     */
    public synchronized void stop() {
        try {
            cacheDB.close();
            indexer.close();
            stop = true;
            gcTaskHandle.cancel(false);
            GC_TIMER.purge();
        } catch (DBException ex) {
            if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Unable to close advertisments.tbl", ex);
            }
        }
    }

    private final class GC_Task implements Runnable {

        /**
         * {@inheritDoc}
         * <p/>
         * Responsible for initiating GC operations.
         */
        public void run() {
            try {
                if (stop) {
                    // if asked to stop, exit
                    return;
                }

                // Decide if it's time to run the GC operation.
                if ((inconvenienceLevel.get() > MAX_INCONVENIENCE_LEVEL) || (TimeUtils.timeNow() > gcTime)) {
                    inconvenienceLevel.set(0);
                    gcTime = TimeUtils.toAbsoluteTimeMillis(gcMaxInterval);

                    executor.execute(new RecordGC());
                }
            } catch (Throwable all) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
                }
            }
        }
    }

    /**
     * An Executor task which performs the record garbage collection operation.
     */
    private final class RecordGC implements Runnable {

        /**
         * {@inheritDoc}
         * <p/>
         * Responsible for exuting record GC operations.
         */
        public void run() {
            try {
                long gcStart = TimeUtils.timeNow();

                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Starting Garbage collection");
                }

                garbageCollect();

                long gcStop = TimeUtils.timeNow();

                if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Garbage collection completed in " + (gcStop - gcStart) + "ms.");
                }
            } catch (Throwable all) {
                if (Logging.SHOW_SEVERE && LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE, "Uncaught Throwable in thread :" + Thread.currentThread().getName(), all);
                }
            }
        }
    }

    /**
     * Rebuilds record indexes by reading every record and generating
     * new/replacement index entries.
     *
     * @throws net.jxta.impl.xindice.core.DBException
     *
     * @throws java.io.IOException
     */
    private synchronized void rebuildIndex() throws DBException, IOException {
        if (Logging.SHOW_INFO && LOG.isLoggable(Level.INFO)) {
            LOG.info("Rebuilding indices");
        }

        String pattern = "*";
        IndexQuery any = new IndexQuery(IndexQuery.ANY, pattern);

        cacheDB.query(any, new RebuildIndexCallback(cacheDB, indexer));
    }

    private static final class RebuildIndexCallback implements BTreeCallback {

        private BTreeFiler database = null;
        private Indexer index = null;

        RebuildIndexCallback(BTreeFiler database, Indexer index) {
            this.database = database;
            this.index = index;
        }

        /**
         * {@inheritDoc}
         */
        public boolean indexInfo(Value val, long pos) {
            try {
                Record record = database.readRecord(pos);

                if (record == null) {
                    return true;
                }

                long exp = calcExpiration(record);
                if (exp < 0) {
                    database.deleteRecord(record.getKey());
                } else {
                    InputStream is = record.getValue().getInputStream();
                    XMLDocument asDoc = (XMLDocument) StructuredDocumentFactory.newStructuredDocument(MimeMediaType.XMLUTF8, is);
                    Advertisement adv = AdvertisementFactory.newAdvertisement(asDoc);
                    Map<String, String> indexables = CacheUtils.getIndexfields(adv.getIndexFields(), asDoc);

                    String dn = getDirName(adv);
                    Map<String, String> keyedIdx = addKey(dn, indexables);

                    if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Restoring index " + keyedIdx + " at " + pos);
                    }
                    index.addToIndex(keyedIdx, pos);
                }
            } catch (Exception ex) {
                if (Logging.SHOW_WARNING && LOG.isLoggable(Level.WARNING)) {
                    LOG.log(Level.WARNING, "Exception rebuilding index  at " + pos, ex);
                }
            }

            return true;
        }
    }
}
