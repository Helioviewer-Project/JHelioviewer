package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Vector;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent.GenericTriangle;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
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
        if(evt==null || !evt.isVisible(now))
            return;
        
        Vector<HEKEvent.GenericTriangle<Vector3d>> triangles=evt.getTriangulation3D(now);
        Vector<SphericalCoord> outerBound=evt.getStonyBound(now);
        if(outerBound==null && triangles==null)
            return;
            
        String type=evt.getString("event_type");
        Color eventColor=HEKConstants.getSingletonInstance().acronymToColor(type,128);

        SphericalCoord stony=evt.getStony(now);
        double latitude = stony.theta / 180.0 * Math.PI;
        
        g.gl.glPushMatrix();
        g.gl.glRotated(DifferentialRotation.calculateRotationInDegrees(latitude,(now.getTime()-evt.getStart().getTime())/1000d),0,1,0);
        
        if(triangles!=null)
        {
            g.gl.glColor4ub((byte)eventColor.getRed(),(byte)eventColor.getGreen(),(byte)eventColor.getBlue(),(byte)eventColor.getAlpha());

            g.gl.glEnable(GL2.GL_CULL_FACE);
            g.gl.glDisable(GL2.GL_DEPTH_TEST);
            g.gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);

            g.gl.glBegin(GL2.GL_TRIANGLES);
            for(GenericTriangle<Vector3d> triangle:triangles)
            {
                //g.gl.glColor3d(Math.random(),Math.random(),Math.random());
                g.gl.glVertex3d(triangle.A.x,triangle.A.y,triangle.A.z);
                g.gl.glVertex3d(triangle.B.x,triangle.B.y,triangle.B.z);
                g.gl.glVertex3d(triangle.C.x,triangle.C.y,triangle.C.z);
            }
            g.gl.glEnd();
        }

        // draw bounds
        g.gl.glColor3f(1,1,1);
        if(outerBound!=null)
        {
            g.gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);
            g.gl.glEnable(GL2.GL_DEPTH_TEST);

            g.gl.glBegin(GL.GL_LINE_LOOP);
            for(SphericalCoord boundaryPoint:outerBound)
            {
                Vector3d boundaryPoint3d=HEKEvent.convertToSceneCoordinates(boundaryPoint,now).scale(1.005);
                g.gl.glVertex3d(boundaryPoint3d.x,boundaryPoint3d.y,boundaryPoint3d.z);
            }
            g.gl.glEnd();
        }

        g.gl.glPopMatrix();
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
        if(evt==null || !evt.isVisible(now))
            return;

        boolean large=evt.getShowEventInfo();
        BufferedImage icon=evt.getIcon(large);
        if(icon!=null)
        {
            SphericalCoord stony=evt.getStony(now);
            Vector3d coords=HEKEvent.convertToSceneCoordinates(stony,now);
            double x=coords.x;
            double y=coords.y;
            double z=coords.z;
            Vector2d imageSize=
                    ViewHelper.convertScreenToImageDisplacement(icon.getWidth(),icon.getHeight(),g.regionView.getLastDecodedRegion(),ViewHelper.calculateViewportImageSize(g.viewportView.getViewport(),g.regionView.getLastDecodedRegion()));

            g.commonRenderGraphics.bindImage(icon);
            g.gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MAG_FILTER,GL.GL_LINEAR);

            double width2=imageSize.x*scale/2.0;
            double height2=imageSize.y*scale/2.0;

            Vector3d sourceDir=new Vector3d(0,0,1);
            Vector3d targetDir=new Vector3d(x,y,z);

            double angle=Math.acos(sourceDir.dot(targetDir)/(sourceDir.length()*targetDir.length()));
            Vector3d axis=sourceDir.cross(targetDir);
            Matrix4d r=Matrix4d.rotation(angle,axis.normalize());
            r.setTranslation(x,y,z);

            Vector3d p0=new Vector3d(-width2,-height2,0);
            Vector3d p1=new Vector3d(-width2,height2,0);
            Vector3d p2=new Vector3d(width2,height2,0);
            Vector3d p3=new Vector3d(width2,-height2,0);
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

    /**
     * {@inheritDoc}
     * 
     * Draws all available and visible solar events with there associated icon.
     */
    public void render(GLPhysicalRenderGraphics g)
    {
        JHVJPXView masterView=LinkedMovieManager.getActiveInstance().getMasterMovie();
        if(masterView==null || masterView.getCurrentFrameDateTime()==null)
            return;
        
        Date currentDate=masterView.getCurrentFrameDateTime().getTime();
        if(currentDate==null)
            return;

        Vector<HEKEvent> toDraw=HEKCache.getSingletonInstance().getModel().getActiveEvents(currentDate);

        g.gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
        g.gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
        g.gl.glBindTexture(GL2.GL_TEXTURE_2D,0);
        g.gl.glDisable(GL2.GL_LIGHTING);
        g.gl.glDisable(GL2.GL_TEXTURE_2D);
        g.gl.glEnable(GL2.GL_CULL_FACE);
        g.gl.glEnable(GL2.GL_LINE_SMOOTH);
        g.gl.glEnable(GL2.GL_BLEND);

        for(HEKEvent evt:toDraw)
            drawPolygon(g,evt,currentDate);

        g.gl.glDisable(GL2.GL_LINE_SMOOTH);

        g.gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);
        g.gl.glDisable(GL2.GL_DEPTH_TEST);
        g.gl.glEnable(GL2.GL_TEXTURE_2D);
        g.gl.glColor3f(1.0f,1.0f,1.0f);

        for(HEKEvent evt:toDraw)
            drawIcon(g,evt,currentDate);

        g.gl.glDisable(GL2.GL_TEXTURE_2D);
        g.gl.glEnable(GL2.GL_LIGHTING);
        g.gl.glDisable(GL2.GL_BLEND);
        g.gl.glEnable(GL2.GL_DEPTH_TEST);
        g.gl.glDisable(GL2.GL_CULL_FACE);

        GL3DState.get().checkGLErrors("HEKPlugin3dRenderer.afterRender");
    }

    public void viewChanged(View view)
    {
        GL3DLayeredView layeredView=ViewHelper.getViewAdapter(view,GL3DLayeredView.class);
        if(layeredView==null)
            return;

        double heigth=-1;
        for(int i=0;i<layeredView.getNumLayers();i++)
        {
            if(layeredView.getLayer(i).getAdapter(RegionView.class)!=null && layeredView.getLayer(i).getAdapter(RegionView.class).getLastDecodedRegion() != null &&heigth<layeredView.getLayer(i).getAdapter(RegionView.class).getLastDecodedRegion().getHeight())
                heigth=layeredView.getLayer(i).getAdapter(RegionView.class).getLastDecodedRegion().getHeight();
        }
        
        PhysicalRegion region=view.getAdapter(RegionView.class).getLastDecodedRegion();
        if(region!=null)
            scale=(float)(heigth/region.getHeight());
    }
}
