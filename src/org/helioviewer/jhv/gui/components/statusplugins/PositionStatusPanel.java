package org.helioviewer.jhv.gui.components.statusplugins;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Formatter;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.components.BasicImagePanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelPlugin;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;
import org.helioviewer.jhv.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Status panel for displaying the current mouse position.
 * 
 * <p>
 * If the the physical dimension of the image are known, the physical position
 * will be shown, otherwise, shows the screen position.
 * 
 * <p>
 * Basically, the information of this panel is independent from the active
 * layer.
 * 
 * <p>
 * If there is no layer present, this panel will be invisible.
 */
public class PositionStatusPanel extends ViewStatusPanelPlugin implements MouseMotionListener, ImagePanelPlugin {

    private static final long serialVersionUID = 1L;

    private View view;
    private RegionView regionView;
    private ViewportView viewportView;
    private MetaDataView metaDataView;
    private BasicImagePanel imagePanel;
    private Point lastPosition;

    private static final char PRIME = '\u2032';

    /**
     * Default constructor.
     * 
     * @param imagePanel
     *            ImagePanel to show mouse position for
     */
    public PositionStatusPanel(BasicImagePanel imagePanel) {
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(170, 20));

        LayersModel.getSingletonInstance().addLayersListener(this);

        imagePanel.addPlugin(this);

        setText("(x, y) = " + "(    0" + PRIME + PRIME + ",    0" + PRIME + PRIME + ")");
    }

    /**
     * Updates the displayed position.
     * 
     * If the physical dimensions are available, translates the screen
     * coordinates to physical coordinates.
     * 
     * @param position
     *            Position on the screen.
     */
    private void updatePosition(Point position) {

        // check region and viewport
        Region r = regionView.getRegion();
        Viewport v = viewportView.getViewport();
        MetaData m = metaDataView.getMetaData();

        if (r == null || v == null || m == null) {
            setText("(x, y) = " + "(" + position.x + "," + position.y + ")");
            return;
        }

        // get viewport image size
        ViewportImageSize vis = ViewHelper.calculateViewportImageSize(v, r);

        // Helioviewer images have there physical lower left corner in a
        // negative area; real pixel based image at 0
        if (m.getPhysicalLowerLeft().x < 0) {

            Vector2i solarcenter = ViewHelper.convertImageToScreenDisplacement(regionView.getRegion().getUpperLeftCorner().negateX(), regionView.getRegion(), vis);

            Vector2d scaling = new Vector2d(Constants.SUN_RADIUS, Constants.SUN_RADIUS);
            Vector2d solarRadius = new Vector2d(ViewHelper.convertImageToScreenDisplacement(scaling, regionView.getRegion(), vis));

            Vector2d pos = new Vector2d(position.x - solarcenter.getX(), -position.y + solarcenter.getY()).invertedScale(solarRadius).scale(959.705);

            Formatter fmt = new Formatter();
            String xStr = fmt.format(" %5d", (int) Math.round(pos.x)).toString();
            fmt.close();
            fmt = new Formatter();
            String yStr = fmt.format(" %5d", (int) Math.round(pos.y)).toString();
            fmt.close();
            setText("(x, y) = " + "(" + xStr + PRIME + PRIME + "," + yStr + PRIME + PRIME + ")");
        } else {

            // computes pixel position for simple images (e.g. jpg and png)
            // where cursor points at

            // compute coordinates in image
            int x = (int) (r.getWidth() * (position.getX() / vis.getWidth()) + r.getCornerX());
            int y = (int) (m.getPhysicalImageHeight() - (r.getCornerY() + r.getHeight()) + position.getY() / (double) vis.getHeight() * r.getHeight() + 0.5);

            // show coordinates
            setText("(x, y) = " + "(" + x + "," + y + ")");
        }

        lastPosition = position;
    }

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
        regionView = ViewHelper.getViewAdapter(newView, RegionView.class);
        viewportView = ViewHelper.getViewAdapter(newView, ViewportView.class);
        metaDataView = ViewHelper.getViewAdapter(newView, MetaDataView.class);
    }

    /**
     * {@inheritDoc}
     */
    public BasicImagePanel getImagePanel() {
        return imagePanel;
    }

    /**
     * {@inheritDoc}
     */
    public void setImagePanel(BasicImagePanel newImagePanel) {
        if (imagePanel != null) {
            imagePanel.removeMouseMotionListener(this);
        }
        imagePanel = newImagePanel;
        imagePanel.addMouseMotionListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(MouseEvent e) {
        updatePosition(e.getPoint());
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(MouseEvent e) {
        updatePosition(e.getPoint());
    }

    /**
     * {@inheritDoc}
     */
    public void activeLayerChanged(int idx) {
        if (LayersModel.getSingletonInstance().isValidIndex(idx)) {
            setVisible(true);
        } else {
            setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     */

    public void viewportGeometryChanged() {
        // a view change (e.g. a zoom) can change the coordinates in the
        // picture,
        // so we have to recalculate the position
        if (lastPosition != null) {
            updatePosition(lastPosition);
        }
    }

    public void detach() {
    }

}
