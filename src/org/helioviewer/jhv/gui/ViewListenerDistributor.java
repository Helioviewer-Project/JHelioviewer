package org.helioviewer.jhv.gui;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;

/**
 * This class distributes changes from the associated view chain to all
 * listeners which are part of the GUI.
 * 
 * Sometime components or parts of the GUI have to react on changes which
 * belongs to the view chain. The view listener distributer acts as a
 * registration point for these components.
 * <p>
 * It is guaranteed when something changed to the topmost view of the view chain
 * the view listener distributer will recognize it. There is no guaranteed for
 * that a component which registers it as view listener at a view itself
 * recognize changes to the view chain.
 * 
 * @author Markus Langenberg
 */
public class ViewListenerDistributor implements ViewListener {

    private static ViewListenerDistributor singletonObject = new ViewListenerDistributor();
    private View view;
    private CopyOnWriteArrayList<ViewListener> listeners = new CopyOnWriteArrayList<ViewListener>();

    /**
     * Private default constructor to implement the singleton pattern.
     */
    private ViewListenerDistributor() {
    }

    /**
     * Returns the only instance of this class.
     * 
     * @return the only instance of this class.
     */
    public static ViewListenerDistributor getSingletonInstance() {
        return singletonObject;
    }

    /**
     * Sets the view where to listen for changes.
     * 
     * This changes will be distributed to all listeners of the distributer.
     * 
     * @param newView
     *            view where to listen for changes.
     */
    public void setView(View newView) {
        if (view != null) {
            view.removeViewListener(this);
        }

        view = newView;

        if (newView != null) {
            view.addViewListener(this);
        }

        viewChanged(null, new ChangeEvent(new ViewChainChangedReason(null)));
    }

    /**
     * Adds a view listener.
     * 
     * This listener will be called on every change from views deeper in the
     * view chain.
     * 
     * @param l
     *            the listener to add
     * @see #removeViewListener(ViewListener)
     */
    public void addViewListener(ViewListener l) {
        listeners.add(l);
    }

    /**
     * Removes a view listener.
     * 
     * The listener no longer will be informed about changes from views deeper
     * in the view chain.
     * 
     * @param l
     *            the listener to remove
     * @see #addViewListener(ViewListener)
     */
    public void removeViewListener(ViewListener l) {
        listeners.remove(l);
    }

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {
        notifyViewListener(sender, aEvent);
    }

    /**
     * Informs all listeners of changes made to previous views.
     * 
     * For the reason that this is just a distributor it forwards the events and
     * the sender.
     * 
     * @param sender
     *            last view which sent the change event or forwarded it.
     * @param event
     *            event which contains the associated reasons.
     */
    private void notifyViewListener(View sender, ChangeEvent event) {
        try {
            for (ViewListener listener : listeners) {
            	listener.viewChanged(sender, event);
            }
        } catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }
    }
}
