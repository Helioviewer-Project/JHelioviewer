package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Date;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;

import com.jogamp.common.nio.Buffers;

/**
 * Represents a frame of PFSS Data
 * 
 * it is possible that an instance of the PfssFrame does not yet contain the data. If this is the case, display() won't do anything.
 * 
 * this class is threadsafe
 * @author Jonas Schwammberger
 *
 */
public class PfssDecompressed
{
	private volatile boolean isDataAssigned = false;
	private volatile boolean uploadedVBOs = false;
	private final FileDescriptor descriptor;
	
	private FloatBuffer vertices;
	private IntBuffer indicesSunToOutside = null;
	private IntBuffer indicesSunToSun = null;
	private IntBuffer indicesOutsideToSun = null;
	
	private int[] buffers = null;
	private int VBOVertices;
	private int VBOIndicesSunToOutside;
	private int VBOIndicesSunToSun;
	private int VBOIndicesOutsideToSun;
	
	private float l0;
	private float b0;
	
	public PfssDecompressed(FileDescriptor descriptor)
	{
		this.descriptor = descriptor;
	}
	
	/**
	 * The FrameManager sets the loaded data to this frame
	 * 
	 * @param vertices
	 * @param indicesSunToOutside
	 * @param indicesSunToSun
	 * @param indicesOutsideToSun
	 */
	public synchronized void setLoadedData(FloatBuffer vertices, IntBuffer indicesSunToOutside, IntBuffer indicesSunToSun, IntBuffer indicesOutsideToSun, float _l0, float _b0)
	{
		if(isDataAssigned)
		    return;
		
		this.vertices = vertices;
		this.indicesSunToOutside = indicesSunToOutside;
		this.indicesSunToSun = indicesSunToSun;
		this.indicesOutsideToSun = indicesOutsideToSun;
		l0 = _l0;
		b0 = _b0;
		isDataAssigned = true;
	}
	
	/**
	 * Initializes data on the videocard
	 * @param gl2
	 */
	private void uploadVBOs(GL gl)
	{
        if(!isDataAssigned)
            return;
        
	    if(uploadedVBOs)
	        return;
	    
		GL2 gl2 = gl.getGL2();
		buffers = new int[4];
		gl2.glGenBuffers(4, buffers, 0);

		VBOVertices = buffers[0];
		gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, VBOVertices);
		gl2.glBufferData(GL2.GL_ARRAY_BUFFER, vertices.limit()
				* Buffers.SIZEOF_FLOAT, vertices, GL.GL_STATIC_DRAW);
		gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

		// color
		if (indicesSunToSun != null && indicesSunToSun.limit() > 0) {
			VBOIndicesSunToSun = buffers[1];
			gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,
					VBOIndicesSunToSun);
			gl2.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,
					indicesSunToSun.limit() * Buffers.SIZEOF_INT,
					indicesSunToSun, GL.GL_STATIC_DRAW);
			gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

		if (indicesSunToOutside != null
				&& indicesSunToOutside.limit() > 0) {
			VBOIndicesSunToOutside = buffers[2];
			gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,
					VBOIndicesSunToOutside);
			gl2.glBufferData(
					GL.GL_ELEMENT_ARRAY_BUFFER,
					indicesSunToOutside.limit() * Buffers.SIZEOF_INT,
					indicesSunToOutside, GL.GL_STATIC_DRAW);
			gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

		if (indicesOutsideToSun != null
				&& indicesOutsideToSun.limit() > 0) {
			VBOIndicesOutsideToSun = buffers[3];
			gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,
					VBOIndicesOutsideToSun);
			gl2.glBufferData(
					GL.GL_ELEMENT_ARRAY_BUFFER,
					indicesOutsideToSun.limit() * Buffers.SIZEOF_INT,
					indicesOutsideToSun, GL.GL_STATIC_DRAW);
			gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

		uploadedVBOs = true;
	}
	
	/**
	 * Removes data from the Videocard
	 * @param gl
	 */
	public void dispose(GL gl)
	{
		if (uploadedVBOs && gl != null)
		{
			gl.glDeleteBuffers(4, buffers, 0);
			uploadedVBOs = false;
		}
	}
	
	/**
	 * Displays the Fieldlines at the exact time
	 * 
	 * @param gl
	 * @param time
	 */
	public void display(GL gl, Date time)
	{
	    if(!isDataAssigned)
	        return;
	    
	    if(!uploadedVBOs)
	        uploadVBOs(gl);

		JHVJPXView masterView=(JHVJPXView) LinkedMovieManager.getActiveInstance().getMasterMovie();
        if(masterView==null || masterView.getCurrentFrameDateTime()==null)
            return;
        
        Date currentDate=masterView.getCurrentFrameDateTime().getTime();
        if(currentDate==null)
            return;
        
		GL2 gl2 = gl.getGL2();
		gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl2.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl2.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glEnable(GL2.GL_BLEND);
		gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);
		gl2.glBlendEquation(GL2.GL_FUNC_ADD);
		gl2.glEnable(GL2.GL_LINE_SMOOTH);
		gl2.glDepthMask(false);
		gl2.glBindBuffer(GL2.GL_ARRAY_BUFFER, VBOVertices);
		gl2.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);
		Vector3d color;

        //see http://jgiesen.de/sunrot/index.html and http://www.petermeadows.com/stonyhurst/sdisk6in7.gif
        gl2.glRotated(b0,1,0,0);
		gl2.glRotated(DifferentialRotation.calculateRotationInDegrees(0,(currentDate.getTime()-descriptor.getStartDate().getTime())/1000d)-l0,0,1,0);
		
		gl2.glLineWidth(PfssSettings.LINE_WIDTH);
		if (indicesSunToSun != null && indicesSunToSun.limit() > 0)
		{
			color = PfssSettings.SUN_SUN_LINE_COLOR;
			gl2.glColor4d(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
			gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesSunToSun);
			gl2.glDrawElements(GL2.GL_LINES, indicesSunToSun.limit(),
					GL2.GL_UNSIGNED_INT, 0);
		}
		
		if (indicesSunToOutside != null && indicesSunToOutside.limit() > 0)
		{
			color = PfssSettings.SUN_OUT_LINE_COLOR;
			gl2.glColor4d(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
			gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesSunToOutside);
			gl2.glDrawElements(GL2.GL_LINES, indicesSunToOutside.limit(),
					GL2.GL_UNSIGNED_INT, 0);
		}

		if (indicesOutsideToSun != null && indicesOutsideToSun.limit() > 0)
		{
			color = PfssSettings.OUT_SUN_LINE_COLOR;
			gl2.glColor4d(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
			gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesOutsideToSun);
			gl2.glDrawElements(GL2.GL_LINES, indicesOutsideToSun.limit(),
					GL2.GL_UNSIGNED_INT, 0);
		}
		gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl2.glDisable(GL2.GL_LINE_SMOOTH);
		gl2.glDisable(GL2.GL_BLEND);
		gl2.glDepthMask(true);
		gl2.glLineWidth(1f);
	}
	
	/**
	 * @return true if all the data has been loaded into memory
	 */
	public boolean isDataAssigned()
	{
		return isDataAssigned;
	}
	
	public FileDescriptor getDescriptor()
	{
		return descriptor;
	}
}
