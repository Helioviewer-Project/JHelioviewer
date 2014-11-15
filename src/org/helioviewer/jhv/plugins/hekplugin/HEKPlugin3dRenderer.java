package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Vector;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Vector2dDouble;
import org.helioviewer.jhv.base.math.Vector3dDouble;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DMat4d;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent.GenericTriangle;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.renderer.physical.GLPhysicalRenderGraphics;
import org.helioviewer.jhv.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DLayeredView;

/**
 * The solar event renderer provides a possibility to draw solar events with there associated icons.
 * 
 * @author Malte Nuhn
 */
public class HEKPlugin3dRenderer extends PhysicalRenderer3d
{
    private float scale=1;

    /**
     * Default constructor.
     */
    public HEKPlugin3dRenderer()
    {
    }

    /**
     * The actual rendering routine
     * 
     * @param g
     *            - PhysicalRenderGraphics to render to
     * @param evt
     *            - Event to draw
     * @param now
     *            - Current point in time
     */
    public void drawPolygon(GLPhysicalRenderGraphics g,HEKEvent evt,Date now)
    {

        if(evt!=null&&evt.isVisible(now))
        {

            String type=evt.getString("event_type");
            Color eventColor=HEKConstants.getSingletonInstance().acronymToColor(type,128);

            Vector<HEKEvent.GenericTriangle<Vector3dDouble>> triangles=evt.getTriangulation3D(now);

            if(triangles!=null)
            {
                g.gl.glColor4ub((byte)eventColor.getRed(),(byte)eventColor.getGreen(),(byte)eventColor.getBlue(),(byte)eventColor.getAlpha());

                g.gl.glDisable(GL2.GL_DEPTH_TEST);
                g.gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);

                g.gl.glBegin(GL2.GL_TRIANGLES);
                for(GenericTriangle<Vector3dDouble> triangle:triangles)
                {
                    g.gl.glVertex3d(triangle.A.getX(),-triangle.A.getY(),triangle.A.getZ());
                    g.gl.glVertex3d(triangle.B.getX(),-triangle.B.getY(),triangle.B.getZ());
                    g.gl.glVertex3d(triangle.C.getX(),-triangle.C.getY(),triangle.C.getZ());
                }
                g.gl.glEnd();
            }

            // draw bounds
            g.gl.glColor3f(1,1,1);

            Vector<SphericalCoord> outerBound=evt.getStonyBound(now);

            if(outerBound!=null)
            {
                // sf: shifting depthrange won't work properly, since it's in linear space, instead of inverse linear.
                // --> large shifts far away, almost no shifts near camera. this is exactly the opposite of what we want... :(
                /*
                 * GL gl=g.getGL(); if(gl!=null) { gl.glDepthRange(-0.00012, 0.99988); }
                 */

                g.gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);

                g.gl.glBegin(GL.GL_LINE_LOOP);
                for(SphericalCoord boundaryPoint:outerBound)
                {
                    Vector3dDouble boundaryPoint3d=HEKEvent.convertToSceneCoordinates(boundaryPoint,now,1.005);
                    g.gl.glVertex3d(boundaryPoint3d.getX(),-boundaryPoint3d.getY(),boundaryPoint3d.getZ());
                }
                g.gl.glEnd();

                /*
                 * if(gl!=null) { gl.glDepthRange(0, 1); }
                 */
            }

        }

    }

    /**
     * The actual rendering routine
     * 
     * @param g
     *            - PhysicalRenderGraphics to render to
     * @param evt
     *            - Event to draw
     * @param now
     *            - Current point in time
     */
    public void drawIcon(GLPhysicalRenderGraphics g,HEKEvent evt,Date now)
    {
        if(evt!=null&&evt.isVisible(now))
        {
            boolean large=evt.getShowEventInfo();
            BufferedImage icon=evt.getIcon(large);
            if(icon!=null)
            {
                SphericalCoord stony=evt.getStony(now);
                Vector3dDouble coords=HEKEvent.convertToSceneCoordinates(stony,now);
                double x=coords.getX();
                double y=coords.getY();
                double z=coords.getZ();
                Vector2dDouble imageSize=
                        ViewHelper.convertScreenToImageDisplacement(icon.getWidth(),icon.getHeight(),g.regionView.getRegion(),ViewHelper.calculateViewportImageSize(g.viewportView.getViewport(),g.regionView.getRegion()));
                y=-y;

                g.commonRenderGraphics.bindImage(icon);
                g.gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MAG_FILTER,GL.GL_LINEAR);

                double width2=imageSize.getX()*scale/2.0;
                double height2=imageSize.getY()*scale/2.0;

                GL3DVec3d sourceDir=new GL3DVec3d(0,0,1);
                GL3DVec3d targetDir=new GL3DVec3d(x,y,z);

                double angle=Math.acos(sourceDir.dot(targetDir)/(sourceDir.length()*targetDir.length()));
                GL3DVec3d axis=sourceDir.cross(targetDir);
                GL3DMat4d r=GL3DMat4d.rotation(angle,axis.normalize());
                r.setTranslation(x,y,z);

                GL3DVec3d p0=new GL3DVec3d(-width2,-height2,0);
                GL3DVec3d p1=new GL3DVec3d(-width2,height2,0);
                GL3DVec3d p2=new GL3DVec3d(width2,height2,0);
                GL3DVec3d p3=new GL3DVec3d(width2,-height2,0);
                p0=r.multiply(p0);
                p1=r.multiply(p1);
                p2=r.multiply(p2);
                p3=r.multiply(p3);

                g.gl.glBegin(GL2.GL_QUADS);

                g.commonRenderGraphics.setTexCoord(0.0f,0.0f);
                g.gl.glVertex3d(p0.x,p0.y,p0.z);
                g.commonRenderGraphics.setTexCoord(0.0f,1.0f);
                g.gl.glVertex3d(p1.x,p1.y,p1.z);
                g.commonRenderGraphics.setTexCoord(1.0f,1.0f);
                g.gl.glVertex3d(p2.x,p2.y,p2.z);
                g.commonRenderGraphics.setTexCoord(1.0f,0.0f);
                g.gl.glVertex3d(p3.x,p3.y,p3.z);

                g.gl.glEnd();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Draws all available and visible solar events with there associated icon.
     */
    public void render(GLPhysicalRenderGraphics g)
    {
        JHVJPXView masterView=LinkedMovieManager.getActiveInstance().getMasterMovie();
        if(masterView!=null&&masterView.getCurrentFrameDateTime()!=null)
        {
            Date currentDate=masterView.getCurrentFrameDateTime().getTime();

            if(currentDate!=null)
            {
                Vector<HEKEvent> toDraw=HEKCache.getSingletonInstance().getModel().getActiveEvents(currentDate);

                g.gl.glBindTexture(GL2.GL_TEXTURE_2D,0);
                g.gl.glDisable(GL2.GL_LIGHTING);
                g.gl.glDisable(GL2.GL_TEXTURE_2D);
                g.gl.glEnable(GL2.GL_CULL_FACE);
                g.gl.glEnable(GL2.GL_LINE_SMOOTH);
                g.gl.glEnable(GL2.GL_BLEND);

                for(HEKEvent evt:toDraw)
                {
                    drawPolygon(g,evt,currentDate);
                }

                g.gl.glDisable(GL2.GL_BLEND);
                g.gl.glEnable(GL2.GL_DEPTH_TEST);
                g.gl.glDisable(GL2.GL_CULL_FACE);
                g.gl.glDisable(GL2.GL_LINE_SMOOTH);

                g.gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
                g.gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
                g.gl.glDisable(GL2.GL_LIGHTING);
                g.gl.glEnable(GL2.GL_BLEND);
                g.gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);
                g.gl.glDisable(GL2.GL_DEPTH_TEST);
                g.gl.glEnable(GL2.GL_CULL_FACE);
                g.gl.glEnable(GL2.GL_TEXTURE_2D);
                g.gl.glColor3f(1.0f,1.0f,1.0f);

                for(HEKEvent evt:toDraw)
                {
                    drawIcon(g,evt,currentDate);
                }

                g.gl.glDisable(GL2.GL_TEXTURE_2D);
                g.gl.glEnable(GL2.GL_LIGHTING);
                g.gl.glDisable(GL2.GL_BLEND);
                g.gl.glEnable(GL2.GL_DEPTH_TEST);
                g.gl.glDisable(GL2.GL_CULL_FACE);
                g.gl.glEnable(GL2.GL_BLEND);
                g.gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
                g.gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
            }
            GL3DState.get().checkGLErrors("HEKPlugin3dRenderer.afterRender");
        }
    }

    public void viewChanged(View view)
    {
        GL3DLayeredView layeredView=ViewHelper.getViewAdapter(view,GL3DLayeredView.class);
        if(layeredView!=null)
        {
            double heigth=-1;
            for(int i=0;i<layeredView.getNumLayers();i++)
            {
                if(layeredView.getLayer(i).getAdapter(RegionView.class)!=null&&heigth<layeredView.getLayer(i).getAdapter(RegionView.class).getRegion().getHeight())
                    heigth=layeredView.getLayer(i).getAdapter(RegionView.class).getRegion().getHeight();
            }
            Region region=view.getAdapter(RegionView.class).getRegion();
            if(region!=null)
            {
                scale=(float)(heigth/region.getHeight());
            }
        }

    }

}
