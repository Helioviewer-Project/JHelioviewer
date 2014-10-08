package org.helioviewer.viewmodel.filter;

import java.util.concurrent.CopyOnWriteArrayList;


public abstract class AbstractFilter implements ObservableFilter {
	private CopyOnWriteArrayList<FilterListener> listeners = new CopyOnWriteArrayList<FilterListener>();

    /**
     * {@inheritDoc}
     */
    public void addFilterListener(FilterListener l) {
        listeners.add(l);
    }

    /**
     * {@inheritDoc}
     */
    public void removeFilterListener(FilterListener l) {
        listeners.remove(l);
    }

    /**
     * Notifies all registered listeners, that something has changed.
     */
    protected void notifyAllListeners() {
       for (FilterListener f : listeners) {
    	   f.filterChanged(this);
       }
    }
}
