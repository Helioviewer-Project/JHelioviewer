package org.helioviewer.jhv.viewmodel.viewport;

import org.helioviewer.jhv.base.math.Vector2i;

/**
 * Implementation of {@link BasicViewport}.
 * 
 * @author Ludwig Schmidt
 * */
public class StaticViewport implements Viewport {

    private final Vector2i sizeVector;

    /**
     * Constructor where to pass the viewport information as int values.
     * 
     * @param newViewportWidth
     *            width of the viewport.
     * @param newViewportHeight
     *            height of the viewport.
     * */
    public StaticViewport(final int newViewportWidth, final int newViewportHeight) {
        sizeVector = new Vector2i(newViewportWidth, newViewportHeight);
    }

    /**
     * Constructor where to pass the viewport information as a vector.
     * 
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the
     *            viewport.
     * */
    public StaticViewport(final Vector2i newSizeVector) {
        sizeVector = newSizeVector;
    }

    /**
     * Creates a ViewportAdapter object by using the passed viewport
     * information.
     * 
     * @param newViewportWidth
     *            width of the viewport.
     * @param newViewportHeight
     *            height of the viewport.
     * */
    public static Viewport createAdaptedViewport(final int newViewportWidth, final int newViewportHeight) {
        return new ViewportAdapter(new StaticViewport(newViewportWidth, newViewportHeight));
    }

    /**
     * Creates a ViewportAdapter object by using the passed viewport
     * information.
     * 
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the
     *            viewport.
     * */
    public static Viewport createAdaptedViewport(final Vector2i newSizeVector) {
        return new ViewportAdapter(new StaticViewport(newSizeVector));
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2i getSize() {
        return sizeVector;
    }

    public String toString() {
        return "[Viewport: Size: " + sizeVector + "]";
    }

    @Override
    public int getWidth()
    {
        return sizeVector.getX();
    }

    @Override
    public int getHeight()
    {
        return sizeVector.getY();
    }

    @Override
    public double getAspectRatio()
    {
        return sizeVector.getX()/(double)sizeVector.getY();
    }

    @Override
    public boolean equals(Viewport _v)
    {
        if(!(_v instanceof StaticViewport))
            return false;
        
        return ((StaticViewport)_v).sizeVector.equals(sizeVector);
    }
}
