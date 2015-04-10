package org.helioviewer.jhv.opengl.model;

import java.util.List;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;
import org.helioviewer.jhv.viewmodel.view.opengl.GLTextureHelper;

/**
 * Helper Node to visualize the current content of the Framebuffer.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DFramebufferImage extends GL3DMesh implements ViewListener {
    private Integer textureId;
    // private Vector2dDouble textureScale;
    private boolean recreateMesh = true;

    private Region region;

    // private Viewport viewport;
    // private MetaData metaData;
    //
    // private Vector2dDouble textureScale;

    public GL3DFramebufferImage() {
        super("Framebuffer", new Vector4d(1, 1, 1, 1), new Vector4d(0, 0, 0, 0));
    }

    public void shapeDraw(GL3DState state) {
        if (this.region != null) {
            GLTextureHelper th = new GLTextureHelper();
            th.bindTexture(state.gl, textureId);
            super.shapeDraw(state);
            th.bindTexture(state.gl, 0);
        }
    }

    public void shapeUpdate(GL3DState state) {
        if (recreateMesh) {
            this.recreateMesh(state);
            this.recreateMesh = false;
        }
    }

    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(ImageTextureRecapturedReason.class)) {
            ImageTextureRecapturedReason imageTextureRecapturedReason = aEvent.getLastChangedReasonByType(ImageTextureRecapturedReason.class);
            this.textureId = imageTextureRecapturedReason.getTextureId();
            this.region = imageTextureRecapturedReason.getCapturedRegion();
            this.recreateMesh = true;
            markAsChanged();
        }

    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<Vector3d> positions, List<Vector3d> normals, List<Vector2d> textCoords, List<Integer> indices, List<Vector4d> colors) {
        // Log.debug("GL3DFramebufferImage: Create Mesh!");
        if (region != null) {
        	double blx = region.getCornerX();
            double bly = region.getCornerY();
            double tr_x = region.getUpperRightCorner().x;
            double tr_y = region.getUpperRightCorner().y;
            positions.add(new Vector3d(blx, bly, 0));
            positions.add(new Vector3d(tr_x, bly, 0));
            positions.add(new Vector3d(tr_x, tr_y, 0));
            positions.add(new Vector3d(blx, tr_y, 0));

            textCoords.add(new Vector2d(0, 0));
            textCoords.add(new Vector2d(1, 0));
            textCoords.add(new Vector2d(1, 1));
            textCoords.add(new Vector2d(0, 1));

            normals.add(new Vector3d(0, 0, 1));
            normals.add(new Vector3d(0, 0, 1));
            normals.add(new Vector3d(0, 0, 1));
            normals.add(new Vector3d(0, 0, 1));

            indices.add(0);
            indices.add(1);
            indices.add(2);
            indices.add(3);
            
        }

        return GL3DMeshPrimitive.QUADS;
    }

}
