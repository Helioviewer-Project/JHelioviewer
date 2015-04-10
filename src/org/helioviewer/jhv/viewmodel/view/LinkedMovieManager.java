package org.helioviewer.jhv.viewmodel.view;

import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.view.jp2view.ImmutableDateTime;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;

/**
 * Class managing all linked movies.
 * 
 * <p>
 * This class is responsible for synchronizing all linked movies. Therefore, all
 * linked movies have to call the various functions of this class. Then, all
 * other linked movies are set according to the new values.
 * 
 * <p>
 * When actually playing a movie instead of just scrolling around, a master
 * movie is chosen, based on the average cadence of all linked movies. Only the
 * master movie is actually playing, all other movies are just set to the frame
 * closest to the one from the master movie.
 * 
 * <p>
 * It is possible to manage multiple sets of linked movies. Therefore, the
 * LinkedMovieManager manages multiple sets of itself, one per set of linked
 * movies. The default instance (id = 0) always exists and can not be deleted.
 * 
 * @author Markus Langenberg
 */
public class LinkedMovieManager {

    private static Vector<LinkedMovieManager> instances = new Vector<LinkedMovieManager>();
    private static int activeInstance = 0;
    private LinkedList<JHVJPXView> linkedMovies = new LinkedList<JHVJPXView>();
    private JHVJPXView masterView;
    private Semaphore updateSemaphore = new Semaphore(1);
    private Semaphore isPlayingSemaphore = new Semaphore(1);
    private ReentrantLock isPlayingLock = new ReentrantLock();

    /**
     * Default constructor
     */
    private LinkedMovieManager() {
    }

    /**
     * Returns the active instance of the LinkedMovieManager.
     * 
     * There can only be one instance active at a time, but it is possible to
     * manage multiple groups of linked movies by switching the active instance.
     * 
     * @return The active instance of this class.
     * @see #setActiveInstance(int)
     * @see #createNewInstance()
     * @see #deleteInstance(int)
     */
    public static LinkedMovieManager getActiveInstance() {
        if (instances.isEmpty()) {
            instances.add(new LinkedMovieManager());
        }

        return instances.get(activeInstance);
    }

    /**
     * Sets the active instance of the LinkedMovieManager to use.
     * 
     * @param instance
     *            ID of the new active instance.
     * @see #getActiveInstance()
     * @see #createNewInstance()
     * @see #deleteInstance(int)
     */
    public static void setActiveInstance(int instance) {
        if (instance < instances.size() && instances.get(instance) != null) {
            activeInstance = instance;
        }
    }

    /**
     * Adds the given movie view to the set of linked movies.
     * 
     * @param JHVJPXView
     *            View to add to the set of linked movies.
     */
    public synchronized void linkMovie(JHVJPXView JHVJPXView) {
        if (JHVJPXView.getMaximumFrameNumber() > 0 && !linkedMovies.contains(JHVJPXView)) {
            linkedMovies.add(JHVJPXView);

            updateMaster();
        }
    }

    /**
     * Removes the given movie view from the set of linked movies.
     * 
     * @param JHVJPXView
     *            View to remove from the set of linked movies.
     */
    public synchronized void unlinkMovie(JHVJPXView JHVJPXView) {
        if (linkedMovies.contains(JHVJPXView)) {
            linkedMovies.remove(JHVJPXView);

            updateMaster();

            if (!linkedMovies.isEmpty()) {
                JHVJPXView.pauseMovie();
            }
        }
    }

    /**
     * Returns, whether the given view is the master view.
     * 
     * @param JHVJPXView
     *            View to test
     * @return True, if the given view is the master view, false otherwise.
     */
    public boolean isMaster(JHVJPXView JHVJPXView) {
        if (JHVJPXView == null) {
            return false;
        } else {
            return (JHVJPXView == masterView);
        }
    }

    /**
     * Returns the current master movie
     * 
     * @return current master movie
     */
    public JHVJPXView getMasterMovie() {
        return masterView;
    }

    /**
     * Returns, whether the set of linked movies is playing.
     * 
     * @return True, if the set of linked movies is playing, false otherwise.
     */
    public boolean isPlaying() {

        boolean isPlaying = false;

        try {
            isPlayingLock.lock();

            if (isPlayingSemaphore.tryAcquire()) {
                try {
                    isPlaying = (masterView != null && masterView.isMoviePlaying());
                } finally {
                    isPlayingSemaphore.release();
                }
            }
        } finally {
            isPlayingLock.unlock();
        }

        return isPlaying;
    }

    /**
     * Starts to play the set of linked movies.
     * 
     * This function can be called directly from the movie view in its
     * playMovie()-function, hiding this functionality.
     * 
     * <p>
     * Note, that this function will block recursive calls. The return value
     * indicates whether this function is already called.
     * 
     * @return True, if the function was not called so far and therefore
     *         performed successful, false otherwise.
     */
    public synchronized boolean playLinkedMovies() {
        if (masterView == null)
            return true;

        if (updateSemaphore.tryAcquire()) {

            try {
                for (JHVJPXView movie : linkedMovies) {
                    movie.playMovie();
                }
            } finally {
                updateSemaphore.release();
            }
            return true;
        }
        return false;
    }

    /**
     * Stops to play the set of linked movies.
     * 
     * This function can be called directly from the movie view in its
     * pauseMovie()-function, hiding this functionality.
     * 
     * <p>
     * Note, that this function will block recursive calls. The return value
     * indicates whether this function is already called.
     * 
     * @return True, if the function was not called so far and therefore
     *         performed successful, false otherwise.
     */
    public synchronized boolean pauseLinkedMovies() {
        if (masterView == null)
            return true;

        if (updateSemaphore.tryAcquire()) {

            try {
                masterView.pauseMovie();
            } finally {
                updateSemaphore.release();
            }
            return true;
        }
        return false;
    }

    /**
     * Updates all linked movies according to the current frame of the master
     * frame.
     * 
     * This function can be called directly from the movie view in its
     * rendering-function, hiding this functionality.
     * 
     * <p>
     * Note, that this function will block recursive calls. The return value
     * indicates whether this function is already called.
     * 
     * @param event
     *            ChangeEvent to append new reasons to.
     */
    public synchronized void updateCurrentFrameToMaster(ChangeEvent event) {
        if (masterView == null)
            return;

        if (updateSemaphore.tryAcquire()) {

            try {
                ImmutableDateTime masterTime = masterView.getCurrentFrameDateTime();

                for (JHVJPXView JHVJPXView : linkedMovies) {
                    if (JHVJPXView != masterView) {
                        JHVJPXView.setCurrentFrame(masterTime, new ChangeEvent(event));
                    }
                }
            } finally {
                updateSemaphore.release();
            }
        }
    }

    /**
     * Updates all linked movies according to the given time stamp.
     * 
     * This function can be called directly from the movie view, hiding this
     * functionality.
     * 
     * <p>
     * Note, that this function will block recursive calls. The return value
     * indicates whether this function is already called.
     * 
     * @param dateTime
     *            time which should be matches as close as possible
     * @param event
     *            ChangeEvent to append new reasons to.
     * @param forceSignal
     *            Forces a reader signal and depending on the reader mode a
     *            render signal regardless whether the frame changed
     * @return True, if the function was not called so far and therefore
     *         performed successful, false otherwise.
     */
    public synchronized boolean setCurrentFrame(ImmutableDateTime dateTime, ChangeEvent event, boolean forceSignal) {
        if (updateSemaphore.tryAcquire()) {
            try {
                for (JHVJPXView JHVJPXView : linkedMovies) {
                    JHVJPXView.setCurrentFrame(dateTime, new ChangeEvent(event), forceSignal);
                }
            } finally {
                updateSemaphore.release();
            }
            return true;
        }
        return false;
    }

    /**
     * Recalculates the master view.
     * 
     * The master view is the view, whose movie is actually playing, whereas all
     * other movies just jump to the frame closest to the current frame from the
     * master panel.
     */
    private synchronized void updateMaster() {

        boolean isPlaying = (masterView != null && masterView.isMoviePlaying());
        masterView = null;

        if (linkedMovies.isEmpty()) {
            return;
        } else if (linkedMovies.size() == 1) {
            masterView = linkedMovies.element();
            if (isPlaying) {
                masterView.playMovie();
            }
            return;
        }

        long minimalInterval = Long.MAX_VALUE;
        JHVJPXView minimalIntervalView = null;
        int lastAvailableFrame = 0;

        for (JHVJPXView JHVJPXView : linkedMovies) {

            lastAvailableFrame = 0;

            do {

                if (JHVJPXView instanceof JHVJPXView) {
                    lastAvailableFrame = ((JHVJPXView) JHVJPXView).getDateTimeCache().getMetaStatus();
                } else {
                    lastAvailableFrame = JHVJPXView.getMaximumFrameNumber();
                }

                if (lastAvailableFrame > 0) {
                    break;
                } else {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (true);

            long interval = JHVJPXView.getFrameDateTime(lastAvailableFrame).getMillis() - JHVJPXView.getFrameDateTime(0).getMillis();
            interval /= (lastAvailableFrame + 1);

            if (interval < minimalInterval) {
                minimalInterval = interval;
                minimalIntervalView = JHVJPXView;
            }

            JHVJPXView.pauseMovie();
        }

        masterView = minimalIntervalView;
        if (isPlaying) {
            masterView.playMovie();
        }
    }
}
