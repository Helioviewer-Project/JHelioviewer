package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.helioviewer.jhv.gui.components.BasicImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.gui.HEKEventInformationDialog;

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
public class ImagePanelEventPopupController implements ImagePanelPlugin,
		MouseListener, MouseMotionListener{

	// ///////////////////////////////////////////////////////////////////////////
	// Definitions
	// ///////////////////////////////////////////////////////////////////////////

	private static final Cursor CURSOR_HELP = Cursor
			.getPredefinedCursor(Cursor.HAND_CURSOR);
	private static final int X_OFFSET = 12;
	private static final int Y_OFFSET = 12;

	private BasicImagePanel imagePanel;

	private HEKEvent mouseOverHEKEvent = null;
	private Point mouseOverPosition = null;
	private Cursor lastCursor;
	private HEKEventInformationDialog hekPopUp = new HEKEventInformationDialog();

	// ///////////////////////////////////////////////////////////////////////////
	// Methods
	// ///////////////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 */
	public void setImagePanel(BasicImagePanel newImagePanel) {
        imagePanel = newImagePanel;
        if (imagePanel != null) {
            imagePanel.removeMouseMotionListener(this);
            imagePanel.removeMouseListener(this);
        }
        imagePanel = newImagePanel;
        imagePanel.addMouseMotionListener(this);
		imagePanel.addMouseListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public BasicImagePanel getImagePanel() {
		return imagePanel;
	}

	private Point calcWindowPosition(Point p) {
		int yCoord = 0;
		boolean yCoordInMiddle = false;
		if (p.y + hekPopUp.getSize().height + Y_OFFSET < imagePanel.getSize().height) {
			yCoord = p.y + imagePanel.getLocationOnScreen().y + Y_OFFSET;
		} else {
			yCoord = p.y + imagePanel.getLocationOnScreen().y
					- hekPopUp.getSize().height - Y_OFFSET;
			if (yCoord < imagePanel.getLocationOnScreen().y) {
				yCoord = imagePanel.getLocationOnScreen().y
						+ imagePanel.getSize().height
						- hekPopUp.getSize().height;

				if (yCoord < imagePanel.getLocationOnScreen().y) {
					yCoord = imagePanel.getLocationOnScreen().y;
				}

				yCoordInMiddle = true;
			}
		}

		int xCoord = 0;
		if (p.x + hekPopUp.getSize().width + X_OFFSET < imagePanel.getSize().width) {
			xCoord = p.x + imagePanel.getLocationOnScreen().x + X_OFFSET;
		} else {
			xCoord = p.x + imagePanel.getLocationOnScreen().x
					- hekPopUp.getSize().width - X_OFFSET;
			if (xCoord < imagePanel.getLocationOnScreen().x && !yCoordInMiddle) {
				xCoord = imagePanel.getLocationOnScreen().x
						+ imagePanel.getSize().width - hekPopUp.getSize().width;
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
		/*
		HEKEvent lastHEKEvent = mouseOverHEKEvent;
		if (LinkedMovieManager.getActiveInstance().getMasterMovie() != null){
		JHVJPXView masterView = LinkedMovieManager.getActiveInstance()
				.getMasterMovie();
		Date currentDate = masterView.getCurrentFrameDateTime().getTime();

		Vector3d hitpoint = null;
		mouseOverHEKEvent = null;
		mouseOverPosition = null;

		GL3DSceneGraphView scenegraphview = (GL3DSceneGraphView) GuiState3DWCS.mainComponentView
				.getView();
		if (GL3DState.get() != null
				&& GL3DState.get().activeCamera != null) {
			GL3DRayTracer rayTracer = new GL3DRayTracer(
					scenegraphview.getHitReferenceShape(), GL3DState.get().activeCamera);
			GL3DRay ray = null;
			double x = e.getX() / GuiState3DWCS.mainComponentView.getComponent().getSize().getWidth() * GuiState3DWCS.mainComponentView.getCanavasSize().getWidth();
			double y = e.getY() / GuiState3DWCS.mainComponentView.getComponent().getSize().getHeight() * GuiState3DWCS.mainComponentView.getCanavasSize().getHeight();
			ray = rayTracer.cast((int)x, (int)y);

			if (ray != null) {
				if (ray.getHitPoint() != null) {

					hitpoint = ray.getHitPoint();
				}
			}

			if (currentDate != null) {
				Vector<HEKEvent> toDraw = HEKCache.getSingletonInstance()
						.getModel().getActiveEvents(currentDate);

				for (HEKEvent evt : toDraw) {
					SphericalCoord stony = evt.getStony(currentDate);
					Vector3d coords = HEKEvent.convertToSceneCoordinates(
							stony, currentDate);
					
					if (hitpoint != null) {
						double deltaX = Math.abs(hitpoint.x - coords.x);
						double deltaY = Math.abs(hitpoint.y - coords.y);
						double deltaZ = Math.abs(hitpoint.z - coords.z);
						if (deltaX < 10000000 && deltaZ < 10000000
								&& deltaY < 10000000) {
							mouseOverHEKEvent = evt;
							mouseOverPosition = new Point(e.getX(), e.getY());
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
		}}*/
	}

	public void detach() {
	}

}
