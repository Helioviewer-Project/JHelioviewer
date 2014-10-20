package org.helioviewer.viewmodel.view;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.renderer.screen.ScreenRenderer;

public abstract class AbstractComponentView extends AbstractBasicView implements ComponentView {

    protected volatile Vector2dInt mainImagePanelSize;

    protected CopyOnWriteArrayList<ScreenRenderer> postRenderers = new CopyOnWriteArrayList<ScreenRenderer>();

    public void updateMainImagePanelSize(Vector2dInt size) {
        mainImagePanelSize = size;
    }

    /**
     * {@inheritDoc}
     */
    public void addPostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
            if (!containsPostRenderer(postRenderer)) {
            	postRenderers.add(postRenderer);
                if (postRenderer instanceof ViewListener) {
                	addViewListener((ViewListener) postRenderer);
                
                }
            }
        }
    }

    private boolean containsPostRenderer(ScreenRenderer postrenderer) {
        return postRenderers.contains(postrenderer);
    }

    /**
     * {@inheritDoc}
     */
    public void removePostRenderer(ScreenRenderer postRenderer) {
        if (postRenderer != null) {
        	postRenderers.remove(postRenderer);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CopyOnWriteArrayList<ScreenRenderer> getAllPostRenderer() {
        return postRenderers;
    }
}
