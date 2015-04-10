package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DImageTextureView;

/**
 * The ChangedReason that is emitted when the {@link GL3DImageTextureView}
 * recaptured the image that was produced by the underlying 2D sub-viewchain.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 */
public class ImageTextureRecapturedReason implements ChangedReason {

    private View sender;

    private Vector2d textureScale;

    private Integer textureId;

    private Region capturedRegion;

    public ImageTextureRecapturedReason(View sender, Integer textureId, Vector2d textureScale, Region capturedRegion) {
        this.sender = sender;
        this.textureId = textureId;
        this.textureScale = textureScale;
        this.capturedRegion = capturedRegion;
    }

    public View getView() {
        return sender;
    }

    public Vector2d getTextureScale() {
        return this.textureScale;
    }

    public Integer getTextureId() {
        return textureId;
    }

    public Region getCapturedRegion() {
        return capturedRegion;
    }
}
