package org.helioviewer.jhv.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.opengl.CenterLoadingScreen;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.jhv.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.jhv.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.jhv.viewmodel.view.LayeredView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DComponentView;

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

    private NoImagePostRenderer noImagePostRenderer = new NoImagePostRenderer();
    private boolean noImagePostRendererSet = false;

    private LoadingPostRendererSwitch loadingPostRenderer = new LoadingPostRendererSwitch();
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
        noImagePostRenderer.setContainerSize(getWidth(), getHeight());
        addPostRenderer(noImagePostRenderer);
        noImagePostRendererSet = true;

        loadingPostRenderer.setContainerSize(getWidth(), getHeight());
        
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
                if (noImagePostRendererSet) {
                    removePostRenderer(noImagePostRenderer);
                    noImagePostRendererSet = false;
                }
                addPostRenderer(loadingPostRenderer);
                //if (!JHVGlobals.OLD_RENDER_MODE) ((CompenentView) GuiState3DWCS.mainComponentView).addRenderAnimation(centerLoadingScreen);
                loadingPostRenderer.startAnimation();
            }
            loadingTasks++;
        } else if (loadingTasks > 0) {
            loadingTasks--;
            if (loadingTasks == 0) {
                removePostRenderer(loadingPostRenderer);
                //if (!JHVGlobals.OLD_RENDER_MODE) ((CompenentView) GuiState3DWCS.mainComponentView).removeRenderAnimation(centerLoadingScreen);
                loadingPostRenderer.stopAnimation();

                LayeredView layeredView = GuiState3DWCS.mainComponentView.getAdapter(LayeredView.class);
                if (layeredView.getNumberOfVisibleLayer() == 0) {
                    addPostRenderer(noImagePostRenderer);
                    noImagePostRendererSet = true;
                }
            }
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */

    public void setView(GL3DComponentView newView) {

        super.setView(newView);

        if (newView != null) {

            if (renderedImageComponent != null)
                for (MouseMotionListener l : mouseMotionListeners)
                    renderedImageComponent.addMouseMotionListener(l);
            getView().updateMainImagePanelSize(new Vector2i(getWidth(), getHeight()));
            LayeredView layeredView = ViewHelper.getViewAdapter(newView, LayeredView.class);
            if (layeredView != null) {
                if (layeredView.getNumLayers() > 0 || loadingTasks > 0) {
                    // remove
                    if (noImagePostRendererSet) {
                        removePostRenderer(noImagePostRenderer);
                        noImagePostRendererSet = false;
                    }
                } else {
                    // add
                    if (!noImagePostRendererSet) {
                        addPostRenderer(noImagePostRenderer);
                        noImagePostRendererSet = true;
                    }
                }
            }
        }
    }

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
     * Adds and removes a post renderer which displays the no image loaded sign
     * when there is no image available.
     */

    public void viewChanged(View sender, ChangeEvent aEvent) {
        // checks if at least one layer is available - if not the post renderer
        // for displaying that no image was selected will be added
        if (aEvent.reasonOccurred(LayerChangedReason.class)) {
            LayeredView layeredView = ViewHelper.getViewAdapter(sender, LayeredView.class);
            if (layeredView != null) {
                if (layeredView.getNumLayers() > 0 || loadingTasks > 0) {
                    // remove
                    if (noImagePostRendererSet) {
                        removePostRenderer(noImagePostRenderer);
                        noImagePostRendererSet = false;
                    }
                } else {
                    // add
                    if (!noImagePostRendererSet) {
                        addPostRenderer(noImagePostRenderer);
                        noImagePostRendererSet = true;
                    }
                }

                loadingPostRenderer.useCenterRenderer(layeredView.getNumLayers() == 0);
            }
        }
        // call this method in super class
        super.viewChanged(sender, aEvent);
    }

    /**
     * {@inheritDoc}
     * 
     * Centers the no image loaded image when component was resized.
     */

    public void componentResized(ComponentEvent e) {
    	
        noImagePostRenderer.setContainerSize(getWidth(), getHeight());
        loadingPostRenderer.setContainerSize(getWidth(), getHeight());
        synchronized (this) {
            if (getView() != null) {
                getView().updateMainImagePanelSize(new Vector2i(getWidth(), getHeight()));
            }
        }
        repaint();
        super.componentResized(e);
    }

    /**
     * A post renderer which displays an image which shows that no image (layer)
     * is loaded.
     * 
     * @author Stephan Pagel
     * */
    private static class NoImagePostRenderer implements ScreenRenderer {

        private BufferedImage image = null;
        private Dimension size = new Dimension(0, 0);

        /**
         * Default constructor.
         */
        public NoImagePostRenderer() {
            try {
                image = IconBank.getImage(JHVIcon.NOIMAGE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Sets the size of the available image area (viewport).
         * 
         * @param width
         *            width of the available space of the image.
         * @param height
         *            height of the available space of the image.
         */
        public void setContainerSize(int width, int height) {
            size = new Dimension(width, height);
        }

        /**
         * {@inheritDoc}
         * 
         * Draws the no image loaded image.
         */
        public void render(GLScreenRenderGraphics g) {
            if (image != null) {
                g.drawImage(image, (size.width - image.getWidth()) / 2, (size.height - image.getHeight()) / 2, image.getWidth(), image.getHeight());
            }
        }
    }

    /**
     * A post renderer which indicates that something is being loaded right now.
     * 
     * @author Markus Langenberg
     */
    private interface LoadingPostRenderer extends ScreenRenderer {

        /**
         * Sets the size of the available image area (viewport).
         * 
         * @param width
         *            width of the available space of the image.
         * @param height
         *            height of the available space of the image.
         */
        public void setContainerSize(int width, int height);

        /**
         * Starts animating the pearls.
         */
        public void startAnimation();

        /**
         * Stops animating the pearls.
         */
        public void stopAnimation();

        /**
         * Returns, whether the animation is running.
         * 
         * @return True, if the animation is running, false otherwise
         */
        public boolean isAnimating();
    }

    /**
     * Base implementation of LoadingPostRenderer.
     * 
     * This implementation is abstract and does not specify some specific
     * appearance parameters.
     * 
     * @author Markus Langenberg
     */
    private abstract class BaseLoadingPostRenderer implements LoadingPostRenderer {

        // track
        private final int offsetX;
        private final int offsetY;
        private final int radiusTrack;
        private final float[] sinPositions;

        // pearls
        private final int numPearlPositions;
        private final int numPearls;
        private final int radiusPearl;
        private final Color[] pearlColors;
        private int currentPearlPos = 0;

        // image
        protected BufferedImage image;

        // coordinate of the upper left corner of the image
        protected Point position = new Point(0, 0);

        // timer
        Timer timer;

        /**
         * Default constructor.
         */
        public BaseLoadingPostRenderer(JHVIcon icon, int offsetX, int offsetY, int radiusTrack, int radiusPearl, int numPearlPositions, int numPearls) {
            //super(IconBank.JHVIcon.LOADING_BIG, 124, 101, 97, 6, 32, 12);

            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.radiusTrack = radiusTrack;
            this.radiusPearl = radiusPearl;
            this.numPearlPositions = numPearlPositions;
            this.numPearls = numPearls;

            sinPositions = new float[numPearlPositions];
            for (int i = 0; i < numPearlPositions; i++) {
                sinPositions[i] = (float) Math.sin(Math.PI * 2 * i / numPearlPositions);
            }

            pearlColors = new Color[numPearls];
            for (int i = 0; i < numPearls; i++) {
                int alpha = 192 - (int) (192 * ((float) i / numPearls));
                pearlColors[i] = new Color(192, 192, 192, alpha);
            }

            try {
                image = IconBank.getImage(icon);
            } catch (Exception e) {
                image = new BufferedImage(0, 0, BufferedImage.TYPE_BYTE_GRAY);
                e.printStackTrace();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void startAnimation() {
            currentPearlPos = numPearlPositions / 2;

            if (timer == null) {
                timer = new Timer("Loading Animation", true);
                timer.schedule(new TimerTask() {
                    public void run() {
                        currentPearlPos++;
                        repaint();
                    }
                }, 0, 100);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void stopAnimation() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAnimating() {
            return timer != null;
        }

        /**
         * {@inheritDoc}
         * 
         * Draws the loading image and its animation.
         */
        public void render(GLScreenRenderGraphics g) {
            if (image != null) {
                g.drawImage(image, position.x, position.y, image.getWidth(), image.getHeight());
            }

            int centerX = position.x - radiusPearl + offsetX;
            int centerY = position.y - radiusPearl + offsetY;

            for (int i = 0; i < numPearls; i++) {
                Color color=pearlColors[i];
                g.gl.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
                g.fillOval(centerX + (int) (radiusTrack * sinPositions[(i - currentPearlPos) & (numPearlPositions - 1)]), centerY + (int) (radiusTrack * sinPositions[(i - currentPearlPos + (numPearlPositions >> 2)) & (numPearlPositions - 1)]), radiusPearl * 2, radiusPearl * 2);
            }
        }
    }

    /**
     * Extension of BaseLoadingPostRenderer for drawing a big animation in the
     * middle of the screen.
     * 
     * @author Markus Langenberg
     */
    private class CenterLoadingPostRenderer extends BaseLoadingPostRenderer {

        /**
         * Default constructor.
         */
        public CenterLoadingPostRenderer() {
            super(IconBank.JHVIcon.LOADING_BIG, 124, 101, 97, 6, 32, 12);
        }

        /**
         * {@inheritDoc}
         */
        public void setContainerSize(int width, int height) {
            position = new Point((width - image.getWidth()) / 2, (height - image.getHeight()) / 2);
        }
    }

    /**
     * Extension of BaseLoadingPostRenderer for drawing a small animation in the
     * top right corner of the screen.
     * 
     * @author Markus Langenberg
     */
    private class CornerLoadingPostRenderer extends BaseLoadingPostRenderer {

        /**
         * Default constructor.
         */
        public CornerLoadingPostRenderer() {
            super(IconBank.JHVIcon.LOADING_SMALL, 193, 25, 30, 4, 16, 16);
        }

        /**
         * {@inheritDoc}
         */
        public void setContainerSize(int width, int height) {
            // position = new Point(width - 185, 12);
            position = new Point(width - 231, 12);
        }
    }

    /**
     * Implementation of LoadingPostRenderer for switching between multiple
     * other implementations.
     * 
     * This class owns a CenterLoadingPostRenderer and a
     * CornerLoadingPostRenderer. All calls to this class a redirected to one of
     * them. When no image is loaded, the center version is used, otherwise the
     * corner version.
     * 
     * @author Markus Langenberg
     */
    private class LoadingPostRendererSwitch implements LoadingPostRenderer {

        private LoadingPostRenderer centerRenderer = new CenterLoadingPostRenderer();
        private LoadingPostRenderer cornerRenderer = new CornerLoadingPostRenderer();
        private LoadingPostRenderer currentRenderer = centerRenderer;

        /**
         * {@inheritDoc}
         */
        public void render(GLScreenRenderGraphics g) {
            currentRenderer.render(g);
        }

        /**
         * {@inheritDoc}
         */
        public void setContainerSize(int width, int height) {
            centerRenderer.setContainerSize(width, height);
            cornerRenderer.setContainerSize(width, height);
        }

        /**
         * {@inheritDoc}
         */
        public void startAnimation() {
            currentRenderer.startAnimation();
        }

        /**
         * {@inheritDoc}
         */
        public void stopAnimation() {
            currentRenderer.stopAnimation();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAnimating() {
            return currentRenderer.isAnimating();
        }

        /**
         * Switches between the use of the center renderer or the corner
         * renderer.
         * 
         * @param use
         *            If true, the center renderer ist used, otherwise, the
         *            corner renderer is used
         */
        public void useCenterRenderer(boolean use) {
            if (use) {
                if (cornerRenderer.isAnimating()) {
                    cornerRenderer.stopAnimation();
                    centerRenderer.startAnimation();
                }

                currentRenderer = centerRenderer;
            } else {

                if (centerRenderer.isAnimating()) {
                    centerRenderer.stopAnimation();
                    cornerRenderer.startAnimation();
                }

                currentRenderer = cornerRenderer;
            }
        }
    }
}