package org.helioviewer.gl3d.plugin.pfss.data;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3f;

import com.jogamp.common.nio.Buffers;

/**
 * Represents a frame of PFSS Data
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssFrame {
	private volatile boolean isLoaded = false;
	private volatile boolean isInit = false;
	private final DateAndTimeRange date;
	
	private FloatBuffer vertices;
	private IntBuffer indicesSunToOutside = null;
	private IntBuffer indicesSunToSun = null;
	private IntBuffer indicesOutsideToSun = null;
	
	private int[] buffers = null;
	private int VBOVertices;
	private int VBOIndicesSunToOutside;
	private int VBOIndicesSunToSun;
	private int VBOIndicesOutsideToSun;
	
	public PfssFrame(DateAndTimeRange date) {
		this.date = date;
		
	}
	
	/**
	 * The FrameManager sets the loaded data to this frame
	 * 
	 * @param vertices
	 * @param indicesSunToOutside
	 * @param indicesSunToSun
	 * @param indicesOutsideToSun
	 */
	public void setLoadedData(FloatBuffer vertices, IntBuffer indicesSunToOutside, IntBuffer indicesSunToSun, IntBuffer indicesOutsideToSun) {
		if(!isLoaded) {
			this.vertices = vertices;
			this.indicesSunToOutside = indicesSunToOutside;
			this.indicesSunToSun = indicesSunToSun;
			this.indicesOutsideToSun = indicesOutsideToSun;
			isLoaded = true;
		}
	}
	
	/**
	 * Initializes data on the videocard
	 * @param gl
	 */
	public void init(GL2 gl) {
		if (!isInit && isLoaded && gl != null) {
			buffers = new int[4];
			gl.glGenBuffers(4, buffers, 0);

			VBOVertices = buffers[0];
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, VBOVertices);
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertices.limit()
					* Buffers.SIZEOF_FLOAT, vertices, GL.GL_STATIC_DRAW);
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

			// color
			if (indicesSunToSun != null && indicesSunToSun.limit() > 0) {
				VBOIndicesSunToSun = buffers[1];
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,
						VBOIndicesSunToSun);
				gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER,
						indicesSunToSun.limit() * Buffers.SIZEOF_INT,
						indicesSunToSun, GL.GL_STATIC_DRAW);
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
			}

			if (indicesSunToOutside != null
					&& indicesSunToOutside.limit() > 0) {
				VBOIndicesSunToOutside = buffers[2];
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,
						VBOIndicesSunToOutside);
				gl.glBufferData(
						GL.GL_ELEMENT_ARRAY_BUFFER,
						indicesSunToOutside.limit() * Buffers.SIZEOF_INT,
						indicesSunToOutside, GL.GL_STATIC_DRAW);
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
			}

			if (indicesOutsideToSun != null
					&& indicesOutsideToSun.limit() > 0) {
				VBOIndicesOutsideToSun = buffers[3];
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,
						VBOIndicesOutsideToSun);
				gl.glBufferData(
						GL.GL_ELEMENT_ARRAY_BUFFER,
						indicesOutsideToSun.limit() * Buffers.SIZEOF_INT,
						indicesOutsideToSun, GL.GL_STATIC_DRAW);
				gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
			}

			isInit = true;
		}
	}
	
	/**
	 * Removes data from the Videocard
	 * @param gl
	 */
	public void clear(GL gl) {
		if (isInit) {
			gl.glDeleteBuffers(4, buffers, 0);
			isInit = false;
		}
	}
	
	public void display(GL gl) {
		if(isInit) {
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
			GL3DVec3f color;
	
			gl2.glLineWidth(PfssSettings.LINE_WIDTH);
			// gl.glPrimitiveRestartIndexNV(0);
	
			if (indicesSunToSun != null && indicesSunToSun.limit() > 0) {
				color = PfssSettings.SUN_SUN_LINE_COLOR;
				gl2.glColor4f(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
				gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesSunToSun);
				gl2.glDrawElements(GL2.GL_LINES, indicesSunToSun.limit(),
						GL2.GL_UNSIGNED_INT, 0);
			}
			if (indicesSunToOutside != null && indicesSunToOutside.limit() > 0) {
				color = PfssSettings.SUN_OUT_LINE_COLOR;
				gl2.glColor4f(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
				gl2.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, VBOIndicesSunToOutside);
				gl2.glDrawElements(GL2.GL_LINES, indicesSunToOutside.limit(),
						GL2.GL_UNSIGNED_INT, 0);
			}
	
			if (indicesOutsideToSun != null && indicesOutsideToSun.limit() > 0) {
				color = PfssSettings.OUT_SUN_LINE_COLOR;
				gl2.glColor4f(color.x, color.y, color.z, PfssSettings.LINE_ALPHA);
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
	}
	
	/**
	 * 
	 * @return true if it has been initialised and is ready to be displayed
	 */
	public boolean isInit() {
		return isInit;
	}
	
	/**
	 * @return true if all the data has been loaded into memory
	 */
	public boolean isLoaded() {
		return isLoaded;
	}
	
	public DateAndTimeRange getDate() {
		return this.date;
	}
}
