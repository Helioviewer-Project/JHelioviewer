package org.helioviewer.jhv.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.viewport.StaticViewport;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;
import org.helioviewer.jhv.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * This view is responsible for setting the current region of interest and
 * converting incoming constraints to the 2D rectangle that needs to be
 * requested from the image source and sent through the 2D view chain part.
 * 
 * @author Simon Sp���rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageRegionView extends AbstractGL3DView implements GL3DView, RegionView, ViewportView {
    private RegionView underlyingRegionView;
    private ViewportView viewportView;
    private MetaDataView metaDataView;

    private Vector2i renderOffset;

    private Viewport maximalViewport;
    private Viewport innerViewport;
    private Region detectedRegion;
    private Region actualImageRegion;

    public void render3D(GL3DState state) {
        GL2 gl = state.gl.getGL2();

        if (this.actualImageRegion == null || this.innerViewport == null) {
            return;
        }
        // The child node will render a rect to a rect(lowerLeft, lowerRight,
        // regionWidth, regionHeight).
        // make sure that this will be in the lower left corner.
        double regionWidth = this.actualImageRegion.getWidth();
        double regionHeight = this.actualImageRegion.getHeight();
        double regionWidthOfViewport = regionWidth / this.innerViewport.getWidth() * maximalViewport.getWidth();
        double regionHeightOfViewport = regionHeight / this.innerViewport.getHeight() * maximalViewport.getHeight();
        
        double left = this.actualImageRegion.getCornerX();
        double right = left + regionWidthOfViewport;
        double bottom = this.actualImageRegion.getCornerY();
        double top = bottom + regionHeightOfViewport;
        
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrtho(left, right, bottom, top, -1, 10000);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        this.renderChild(gl);

        // Resume Previous Projection
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        RegionView regionView = newView.getAdapter(RegionView.class);
        if (regionView != null) {
            this.underlyingRegionView = regionView;
        }

        ViewportView viewportView = newView.getAdapter(ViewportView.class);
        if (viewportView != null) {
            this.viewportView = viewportView;
        }

        MetaDataView metaDataView = newView.getAdapter(MetaDataView.class);
        if (metaDataView != null) {
            this.metaDataView = metaDataView;
            if (this.detectedRegion == null) {
                this.detectedRegion = new Region(metaDataView.getMetaData().getPhysicalRegion());
            }
        }
    }

    public boolean setRegion(Region r, ChangeEvent event) {
        if (event == null) {
            event = new ChangeEvent(new RegionUpdatedReason(this, r));
        } else {
            event.addReason(new RegionUpdatedReason(this, r));
        }

        this.detectedRegion = r;
        boolean hasChanged = this.updateRegionAndViewport(event);

        // Log.debug("GL3DImageRegionView: set Region!: "+this.actualImageRegion.getSize());
        hasChanged |= this.viewportView.setViewport(innerViewport, event);
        hasChanged |= this.underlyingRegionView.setRegion(this.actualImageRegion, event);

        event.addReason(new RegionChangedReason(this, r));

        return hasChanged;
    }

    protected boolean updateRegionAndViewport(ChangeEvent event) {
        MetaData metaData = this.metaDataView.getMetaData();
        Region region = ViewHelper.cropRegionToImage(detectedRegion, metaData);
        ViewportImageSize requiredViewportSize = ViewHelper.calculateViewportImageSize(this.maximalViewport, region);
        // Log.debug("GL3DImageRegionView: requiredViewportSize: "+requiredViewportSize.getSizeVector());
        this.innerViewport = StaticViewport.createAdaptedViewport(requiredViewportSize.getSizeVector());
        // Log.debug("GL3DImageRegionView: Inner Viewport: "+innerViewport);

        this.renderOffset = new Vector2i(0, 0);
        // Log.debug("GL3DImageRegionView: Offset: "+renderOffset);
        this.actualImageRegion = region;

        return true;
    }

    public Region getLastDecodedRegion() {
        return this.actualImageRegion;
    }
    
    public boolean setViewport(Viewport v, ChangeEvent event) {
        System.out.println("GL3DImageRegionView:setViewport() Viewport = " + v);
        this.maximalViewport = v;

        this.updateRegionAndViewport(event);

        boolean hasChanged = this.viewportView.setViewport(innerViewport, event);
        hasChanged |= this.underlyingRegionView.setRegion(this.actualImageRegion, event);

        return hasChanged;
    }

    public Viewport getViewport() {
        return this.innerViewport;
    }

    public Vector2i getRenderOffset() {
        return renderOffset;
    }
}
