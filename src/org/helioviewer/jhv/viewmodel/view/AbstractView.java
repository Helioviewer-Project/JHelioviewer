package org.helioviewer.jhv.viewmodel.view;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;

/**
 * Abstract base class implementing View, managing view listeners.
 * 
 * <p>
 * This class provides the functionality to manage and notify all view
 * listeners.
 * 
 * <p>
 * For further information about views, see {@link View}.
 * 
 * @author Markus Langenberg
 */
public abstract class AbstractView implements View {

    private CopyOnWriteArrayList<ViewListener> listeners = new CopyOnWriteArrayList<ViewListener>();

    /**
     * {@inheritDoc}
     */
    public void addViewListener(ViewListener l)
    {
        listeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    public List<ViewListener> getAllViewListener()
    {
        return listeners;
    }

    /**
     * {@inheritDoc}
     */
    public void removeViewListener(ViewListener l)
    {
        listeners.remove(l);
    }

    /**
     * Sends a new ChangeEvent to all registered view listeners.
     * 
     * @param aEvent
     *            ChangeEvent to send
     */
    protected void notifyViewListeners(ChangeEvent aEvent)
    {
        for (ViewListener v : listeners)
            v.viewChanged(this, aEvent);
    }
}
