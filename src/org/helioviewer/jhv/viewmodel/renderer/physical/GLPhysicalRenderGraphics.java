package org.helioviewer.jhv.viewmodel.renderer.physical;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.viewmodel.renderer.GLCommonRenderGraphics;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewportView;


/**
 * Implementation of PhyscialRenderGraphics, using OpenGL for drawing.
 * 
 * <p>
 * Maps all methods to corresponding OpenGL methods.
 * 
 * @author Markus Langenberg
 * 
 * */
public class GLPhysicalRenderGraphics
{
    public RegionView regionView;
    public ViewportView viewportView;

    public GL2 gl;
    public GLCommonRenderGraphics commonRenderGraphics;

    /**
     * Default constructor.
     * 
     * <p>
     * The caller has to provide a gl object, which can be used by this
     * renderer.
     * 
     * @param _gl
     *            gl object, that should be used for drawing.
     * @param view
     *            View to access information about the physical coordinate
     *            system.
     */
    public GLPhysicalRenderGraphics(GL2 _gl, View view) {
        regionView = view.getAdapter(RegionView.class);
        viewportView = view.getAdapter(ViewportView.class);

    	gl = _gl;
        commonRenderGraphics = new GLCommonRenderGraphics(_gl);
    }
}
