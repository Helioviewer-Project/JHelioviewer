package org.helioviewer.jhv.plugins.hekplugin.controller;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Date;
import java.util.Vector;

import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Vector2dDouble;
import org.helioviewer.jhv.base.math.Vector2dInt;
import org.helioviewer.jhv.base.math.Vector3dDouble;
import org.helioviewer.jhv.gui.components.BasicImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.gui.HEKEventInformationDialog;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.ViewListener;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DComponentView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;
import org.helioviewer.jhv.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Implementation of ImagePanelPlugin for showing event popups.
 * 
 * <p>
 * This plugin provides the capability to open an event popup when clicking on
 * an event icon within the main image. Apart from that, it changes the mouse
 * pointer when hovering over an event icon to indicate that it is clickable.
 * 
 * @author Markus Langenberg
 * @author Malte Nuhn
 * 
 */
public class ImagePanelEventPopupController implements ImagePanelPlugin, MouseListener, MouseMotionListener, ViewListener {

    // ///////////////////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////////////////

    private static final Cursor CURSOR_HELP = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int X_OFFSET = 12;
    private static final int Y_OFFSET = 12;

    private View view;
    private ViewportView viewportView;
    private RegionView regionView;

    private BasicImagePanel imagePanel;

    private HEKEvent mouseOverHEKEvent = null;
    private Point mouseOverPosition = null;
    private Cursor lastCursor;
    private HEKEventInformationDialog hekPopUp = new HEKEventInformationDialog();

    private boolean state3D = false;
    // ///////////////////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * {@inheritDoc}
     */
    public void setView(View newView) {
        view = newView;

        viewportView = view.getAdapter(ViewportView.class);
        regionView = view.getAdapter(RegionView.class);
    }

    /**
     * {@inheritDoc}
     */
    public void setImagePanel(BasicImagePanel newImagePanel) {
        imagePanel = newImagePanel;
        imagePanel.addMouseListener(this);
        imagePanel.addMouseMotionListener(this);

        if (imagePanel.getView() != null)
            imagePanel.getView().addViewListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public BasicImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * Converts physical coordinate to screen coordinates
     * 
     * @param x
     *            Physical x-coordinate
     * @param y
     *            Physical y-coordinate
     * @return Corresponding screen coordinate
     */
    private Vector2dInt convertPhysicalToScreen(double x, double y) {
        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(viewportView.getViewport(), regionView.getRegion());

        Vector2dInt offset = ViewHelper.convertImageToScreenDisplacement(-regionView.getRegion().getUpperLeftCorner().getX(), regionView.getRegion().getUpperLeftCorner().getY(), regionView.getRegion(), viewportImageSize);

        return ViewHelper.convertImageToScreenDisplacement(x, y, regionView.getRegion(), viewportImageSize).add(offset);
    }

    private Point calcWindowPosition(Point p) {
        int yCoord = 0;
        boolean yCoordInMiddle = false;
        if (p.y + hekPopUp.getSize().height + Y_OFFSET < imagePanel.getSize().height) {
            yCoord = p.y + imagePanel.getLocationOnScreen().y + Y_OFFSET;
        } else {
            yCoord = p.y + imagePanel.getLocationOnScreen().y - hekPopUp.getSize().height - Y_OFFSET;
            if (yCoord < imagePanel.getLocationOnScreen().y) {
                yCoord = imagePanel.getLocationOnScreen().y + imagePanel.getSize().height - hekPopUp.getSize().height;
                
                if(yCoord < imagePanel.getLocationOnScreen().y) {
                    yCoord = imagePanel.getLocationOnScreen().y;
                }
                
                yCoordInMiddle = true;
            }
        }

        int xCoord = 0;
        if (p.x + hekPopUp.getSize().width + X_OFFSET < imagePanel.getSize().width) {
            xCoord = p.x + imagePanel.getLocationOnScreen().x + X_OFFSET;
        } else {
            xCoord = p.x + imagePanel.getLocationOnScreen().x - hekPopUp.getSize().width - X_OFFSET;
            if (xCoord < imagePanel.getLocationOnScreen().x && !yCoordInMiddle) {
                xCoord = imagePanel.getLocationOnScreen().x + imagePanel.getSize().width - hekPopUp.getSize().width;
            }
        }

        return new Point(xCoord, yCoord);

    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(final MouseEvent e) {

        if (mouseOverHEKEvent != null) {

            // should never be the case
            if (hekPopUp == null) {
                hekPopUp = new HEKEventInformationDialog();
            }

            hekPopUp.setVisible(false);
            hekPopUp.setEvent(mouseOverHEKEvent);

            Point windowPosition = calcWindowPosition(mouseOverPosition);
            hekPopUp.setLocation(windowPosition);
            hekPopUp.setVisible(true);
            hekPopUp.pack();
            imagePanel.setCursor(CURSOR_HELP);

        }

    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(MouseEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(MouseEvent e) {

        HEKEvent lastHEKEvent = mouseOverHEKEvent;

        Date currentDate = LayersModel.getSingletonInstance().getLastUpdatedTimestamp();

        state3D = false;
        GL3DVec3d hitpoint = null;
        mouseOverHEKEvent = null;
        mouseOverPosition = null;
      	state3D = true;
      	
      	GL3DComponentView gl3dview = (GL3DComponentView)view;
      	GL3DSceneGraphView scenegraphview = (GL3DSceneGraphView)gl3dview.getView();
      	if (GL3DState.get() != null && GL3DState.get().getActiveCamera() != null){
      	GL3DRayTracer rayTracer = new GL3DRayTracer(scenegraphview.getHitReferenceShape(), GL3DState.get().getActiveCamera());
      	GL3DRay ray = null;
      	
      	ray = rayTracer.cast(e.getX(), e.getY());
		
      	if (ray != null){
      	if (ray.getHitPoint() != null){
      		
      		hitpoint = ray.getHitPoint();
      	}}
        
        if (currentDate != null) {
            Vector<HEKEvent> toDraw = HEKCache.getSingletonInstance().getModel().getActiveEvents(currentDate);

            for (HEKEvent evt : toDraw) {
            	if (state3D){
            		SphericalCoord stony = evt.getStony(currentDate);
    				Vector3dDouble coords = HEKEvent.convertToSceneCoordinates(stony, currentDate);
    				
            		if (hitpoint != null){
            			double deltaX = Math.abs(hitpoint.x-coords.getX());
            			double deltaY = Math.abs(hitpoint.y+coords.getY());
            			double deltaZ = Math.abs(hitpoint.z-coords.getZ());
            			if (deltaX < 10000000 && deltaZ < 10000000 && deltaY < 10000000){
            				mouseOverHEKEvent = evt;
            				mouseOverPosition = new Point(e.getX(), e.getY());
            			}
            		}
            	}
            	else{
                Vector2dDouble eventPos = evt.getScreenCoordinates(currentDate);
                Vector2dInt screenPos = convertPhysicalToScreen(eventPos.getX(), eventPos.getY());

                if (e.getPoint().getX() >= screenPos.getX() - 8 && e.getPoint().getX() <= screenPos.getX() + 8 && e.getPoint().getY() >= screenPos.getY() - 8 && e.getPoint().getY() <= screenPos.getY() + 8) {
                    mouseOverHEKEvent = evt;
                    mouseOverPosition = new Point(screenPos.getX(), screenPos.getY());
                }
            	}
            }

            if (lastHEKEvent == null && mouseOverHEKEvent != null) {
                lastCursor = imagePanel.getCursor();
                imagePanel.setCursor(CURSOR_HELP);
            } else if (lastHEKEvent != null && mouseOverHEKEvent == null) {
                imagePanel.setCursor(lastCursor);
            }
        }
      	}
    }

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (hekPopUp != null) {
            if (aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(ViewportChangedReason.class) || aEvent.reasonOccurred(TimestampChangedReason.class)) {
                // remove as soon as event is not visible anymore
                if (hekPopUp != null && hekPopUp.isVisible()) {
                    Date currentDate = LayersModel.getSingletonInstance().getLastUpdatedTimestamp();
                    if (currentDate == null) {
                        hekPopUp.setVisible(false);
                        return;
                    }
                    Vector<HEKEvent> toDraw = HEKCache.getSingletonInstance().getModel().getActiveEvents(currentDate);
                    if (!toDraw.contains(hekPopUp.getEvent())) {
                        // hide popup
                        hekPopUp.setVisible(false);
                        // do not show event in the future anymore
                        if (hekPopUp.getEvent() != null) {
                            hekPopUp.getEvent().setShowEventInfo(false);
                        }
                    }
                }
            }
        }
    }

    public void detach() {
    }

}
