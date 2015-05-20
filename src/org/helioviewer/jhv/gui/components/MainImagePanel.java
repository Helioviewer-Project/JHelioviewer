package org.helioviewer.jhv.gui.components;

import java.awt.event.ComponentEvent;
import java.awt.event.MouseMotionListener;
import java.util.AbstractList;
import java.util.LinkedList;

/**
 * This class represents an image component that is used to display the image of
 * all images.
 * 
 * @author caplins
 * @author Alen Agheksanterian
 * @author Benjamin Wamsler
 * @author Stephan Pagel
 * @author Markus Langenberg
 */
public class MainImagePanel extends BasicImagePanel {

    // ///////////////////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////////////////

    // default serialVersionUID
    private static final long serialVersionUID = 1L;

    private boolean noImagePostRendererSet = false;

    //private CenterLoadingScreen centerLoadingScreen;
    private int loadingTasks = 0;

    private AbstractList<MouseMotionListener> mouseMotionListeners = new LinkedList<MouseMotionListener>();

    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * The public constructor
     * */
    public MainImagePanel() {

        // call constructor of super class
        super();

        //if (!JHVGlobals.OLD_RENDER_MODE) centerLoadingScreen = new CenterLoadingScreen();
        // add post render that no image is loaded
        noImagePostRendererSet = true;

        
    }

    /**
     * Shows the image loading animation.
     * 
     * Manages a counter, so that the animation appears on the first loading
     * process and disappears on the last.
     * 
     * @param isLoading
     *            true to start animation, false to stop
     */
    public void setLoading(boolean isLoading) {
        if (isLoading) {
            if (loadingTasks <= 0) {
            }
            loadingTasks++;
        } else if (loadingTasks > 0) {
            loadingTasks--;
            if (loadingTasks == 0) {
            }
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */

    /**
     * {@inheritDoc}
     */

    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        if (l != null)
            mouseMotionListeners.add(l);
    }

    /**
     * {@inheritDoc}
     */

    public synchronized void removeMouseMotionListener(MouseMotionListener l) {
        if (l != null)
            mouseMotionListeners.remove(l);
    }

    /**
     * {@inheritDoc}
     * 
     * Centers the no image loaded image when component was resized.
     */

    public void componentResized(ComponentEvent e) {
    	
        repaint();
        super.componentResized(e);
    }
}