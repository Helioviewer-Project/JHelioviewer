package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Color;
import java.util.Date;
import java.util.Vector;

import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent.GenericTriangle;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

/**
 * The solar event renderer provides a possibility to draw solar events with there associated icons.
 * 
 * @author Malte Nuhn
 */
public class HEKPlugin3dRenderer
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
    public void drawPolygon(GL2 gl,HEKEvent evt,Date now)
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
        
        gl.glPushMatrix();
        gl.glRotated(DifferentialRotation.calculateRotationInDegrees(latitude,(now.getTime()-evt.getStart().getTime())/1000d),0,1,0);
        
        if(triangles!=null)
        {
            gl.glColor4ub((byte)eventColor.getRed(),(byte)eventColor.getGreen(),(byte)eventColor.getBlue(),(byte)eventColor.getAlpha());

            gl.glEnable(GL2.GL_CULL_FACE);
            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);

            gl.glBegin(GL2.GL_TRIANGLES);
            for(GenericTriangle<Vector3d> triangle:triangles)
            {
                //gl.glColor3d(Math.random(),Math.random(),Math.random());
                gl.glVertex3d(triangle.A.x,triangle.A.y,triangle.A.z);
                gl.glVertex3d(triangle.B.x,triangle.B.y,triangle.B.z);
                gl.glVertex3d(triangle.C.x,triangle.C.y,triangle.C.z);
            }
            gl.glEnd();
        }

        // draw bounds
        gl.glColor3f(1,1,1);
        if(outerBound!=null)
        {
            gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL2.GL_DEPTH_TEST);

            gl.glBegin(GL.GL_LINE_LOOP);
            for(SphericalCoord boundaryPoint:outerBound)
            {
                Vector3d boundaryPoint3d=HEKEvent.convertToSceneCoordinates(boundaryPoint,now).scale(1.005);
                gl.glVertex3d(boundaryPoint3d.x,boundaryPoint3d.y,boundaryPoint3d.z);
            }
            gl.glEnd();
        }

        gl.glPopMatrix();
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
    public void drawIcon(GL2 gl,HEKEvent evt,Date now)
    {
        /*
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
            
            //gl.commonRenderGraphics.bindImage(icon);
            gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MAG_FILTER,GL.GL_LINEAR);

            /*double width2=imageSize.x*scale/2.0;
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

            gl.glBegin(GL2.GL_QUADS);

            g.commonRenderGraphics.setTexCoord(0.0f,0.0f);
            gl.glVertex3d(p0.x,p0.y,p0.z);
            g.commonRenderGraphics.setTexCoord(0.0f,1.0f);
            gl.glVertex3d(p1.x,p1.y,p1.z);
            g.commonRenderGraphics.setTexCoord(1.0f,1.0f);
            gl.glVertex3d(p2.x,p2.y,p2.z);
            g.commonRenderGraphics.setTexCoord(1.0f,0.0f);
            gl.glVertex3d(p3.x,p3.y,p3.z);

            gl.glEnd();
        }*/
    }

    /**
     * {@inheritDoc}
     * 
     * Draws all available and visible solar events with there associated icon.
     */
    public void render(GL2 gl)
    {
    	/*
        //Vector<HEKEvent> toDraw=HEKCache.getSingletonInstance().getModel().getActiveEvents(currentDate);

        gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
        gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
        gl.glBindTexture(GL2.GL_TEXTURE_2D,0);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_BLEND);

        for(HEKEvent evt:toDraw)
            drawPolygon(gl,evt,currentDate);

        gl.glDisable(GL2.GL_LINE_SMOOTH);

        gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glDisable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glColor3f(1.0f,1.0f,1.0f);

        for(HEKEvent evt:toDraw)
            drawIcon(gl,evt,currentDate);

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDisable(GL2.GL_CULL_FACE);
	*/
    }

    
}
