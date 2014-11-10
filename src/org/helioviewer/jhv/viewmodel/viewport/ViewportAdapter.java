package org.helioviewer.jhv.viewmodel.viewport;

import org.helioviewer.jhv.base.math.Vector2dInt;

/**
 * Implementation of {@link Viewport}.
 * 
 * @author Ludwig Schmidt
 * */
public class ViewportAdapter implements Viewport {

    private final Viewport viewport;

    /**
     * Default constructor.
     * 
     * @param newViewport
     *            BasicViewport object which holds the viewport description.
     * */
    public ViewportAdapter(final Viewport newViewport) {
        viewport = newViewport;
    }

    /**
     * {@inheritDoc}
     * */
    public int getHeight() {
        return viewport.getSize().getY();
    }

    /**
     * {@inheritDoc}
     * */
    public int getWidth() {
        return viewport.getSize().getX();
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2dInt getSize() {
        return viewport.getSize();
    }

    /**
     * {@inheritDoc}
     * */
    public double getAspectRatio() {
        return ((double) viewport.getSize().getX()) / ((double) viewport.getSize().getY());
    }

    public boolean equals(Viewport v) {
        return v.getHeight() == getHeight() && v.getWidth() == getWidth();
    }

    public String toString() {
        return "[ViewportAdapter: Viewport " + this.viewport + "]";
    }
}
