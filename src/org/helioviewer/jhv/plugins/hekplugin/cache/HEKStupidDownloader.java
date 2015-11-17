package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.helioviewer.jhv.plugins.hekplugin.Interval;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;

/**
 * This is the basic class to access the API. It does not deal with the
 * resulting data, but needs to make proper requests.
 * <p>
 * This is class is 'stupid', because it doesn't do any optimizations to reduce
 * the number of request to be done and doesn't use any parallelism.
 */
public class HEKStupidDownloader {

    /**
     * The ExecutorService running the actual downloadRequests
     */
    private ExecutorService threadExecutor = Executors.newFixedThreadPool(HEKSettings.DOWNLOADER_MAX_THREADS,new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable _r)
        {
            Thread t=Executors.defaultThreadFactory().newThread(_r);
            t.setName("HEK-"+(threadNumber++));
            t.setDaemon(true);
            return t;
        }
    });
    private int threadNumber=0;

    /**
     * Store all requests so that they can be canceled later on
     */
    private List<HEKRequest> downloadRequests = new ArrayList<>();

    // the sole instance of this class
    private static final HEKStupidDownloader SINGLETON = new HEKStupidDownloader();

    /**
     * The private constructor to support the singleton pattern.
     * */
    private HEKStupidDownloader() {
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static HEKStupidDownloader getSingletonInstance() {
        return SINGLETON;
    }

    /**
     * Set the downloading state of the whole downloader
     * 
     * @param keepDownloading
     *            - new downloading state
     */
    public void cancelDownloads() {

        // set all "open" requests to canceled
        for (HEKRequest request : downloadRequests) {
            request.cancel();
        }

        // shut down the executor, so that no new requests are started
        threadExecutor.shutdownNow();

        // create a fresh and new executor
        threadExecutor = Executors.newFixedThreadPool(HEKSettings.DOWNLOADER_MAX_THREADS,new ThreadFactory()
        {
            @Override
            public Thread newThread(Runnable _r)
            {
                Thread t=Executors.defaultThreadFactory().newThread(_r);
                t.setName("HEK-"+(threadNumber++));
                t.setDaemon(true);
                return t;
            }
        });

        // clear all downloadRequests
        downloadRequests = new ArrayList<>();

        // update the treeview
        HEKCache.getSingletonInstance().getController().fireEventsChanged(HEKCache.getSingletonInstance().getController().getRootPath());// cacheModel.getFirstVisiblePath(path));
    }

    /**
     * Request the actual events for the given (HEKPath,Intervals) tuples
     * <p>
     * 
     * @param cacheModel
     *            - cacheModel to fill
     * @param request
     *            - (HEKPath,Intervals) tuples
     */
    public void requestEvents(final HEKCacheController cacheController, HashMap<HEKPath, List<Interval<Date>>> request) {

        for(Entry<HEKPath,List<Interval<Date>>> cur:request.entrySet())
        {
            HEKPath key = cur.getKey();

            cacheController.setState(key, HEKCacheLoadingModel.PATH_QUEUED);

            for (Interval<Date> curInterval : cur.getValue()) {
                HEKRequestThread hekRequest = new HEKRequestThread(cacheController, key, curInterval);
                downloadRequests.add(hekRequest);
                threadExecutor.execute(hekRequest);
            }

        }

    }

    /**
     * Request the structure (which types of events are available) for the given
     * intervals
     * <p>
     * This is one of the methods that are 'stupid' at the moment
     * 
     * @param cacheModel
     *            - cacheModel to fill
     * @param needed
     *            - the intervals to request the structure for
     */
    public void requestStructure(HEKCacheController cacheController, Interval<Date> interval) {
        HEKRequestStructureThread hekRequest = new HEKRequestStructureThread(cacheController, interval);
        downloadRequests.add(hekRequest);
        threadExecutor.execute(hekRequest);
    }

}
