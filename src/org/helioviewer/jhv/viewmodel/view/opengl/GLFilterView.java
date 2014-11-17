package org.helioviewer.jhv.viewmodel.view.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.FilterChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.filter.GLFilter;
import org.helioviewer.jhv.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.jhv.viewmodel.filter.GLImageSizeFilter;
import org.helioviewer.jhv.viewmodel.filter.GLPostFilter;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.view.StandardFilterView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;

/**
 * Implementation of FilterView for rendering in OpenGL mode.
 * 
 * <p>
 * Since filters in OpenGL are implemented as shaders, it is not possible to use
 * every filter in OpenGL mode. In particular, only GLFilters should be used.
 * Never the less, the view chain still works, when a filter does not support
 * OpenGL, but OpenGL will not be used to accelerate views beneath that filter.
 * Instead, it switches to standard mode for the remaining views. This behavior
 * is implemented in this class.
 * 
 * <p>
 * For further information on how to use filters, see
 * {@link org.helioviewer.viewmodel.filter} and
 * {@link org.helioviewer.jhv.viewmodel.view.StandardFilterView}
 * 
 * <p>
 * For further information about how to build shaders, see
 * {@link org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder} as well
 * as the Cg User Manual.
 * 
 * @author Markus Langenberg
 * 
 */
public class GLFilterView extends StandardFilterView implements
		GLFragmentShaderView {

	protected static GLTextureHelper textureHelper = new GLTextureHelper();
	protected ViewportView viewportView;

	protected boolean filteredDataIsUpToDate = false;

	/**
	 * {@inheritDoc} This function also sets the image size.
	 */
	protected void refilterPrepare() {
		super.refilterPrepare();
		if (filter instanceof GLImageSizeFilter && viewportView != null) {
			Viewport viewport = viewportView.getViewport();
			if (viewport != null) {
				((GLImageSizeFilter) filter).setImageSize(viewport.getWidth(),
						viewport.getHeight());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderGL(GL2 gl, boolean nextView) {
		if (filter instanceof GLFilter) {
			refilterPrepare();
			this.checkGLErrors(gl, this + ".afterRefilter");

			if (filter instanceof GLFragmentShaderFilter) {
				gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);
			}

			this.checkGLErrors(gl, this + ".beforeApplyGL");
			((GLFilter) filter).applyGL(gl);
			this.checkGLErrors(gl, this + ".afterApplyGL");

			if (view instanceof GLView) {
				((GLView) view).renderGL(gl, true);
				this.checkGLErrors(gl, view + ".afterRenderGL --> "
						+ this.filter);

			} else {
				if (subimageDataView != null) {
					this.checkGLErrors(gl, view + ".beforeSubimageData");
					textureHelper.renderImageDataToScreen(gl,
							regionView.getLastDecodedRegion(),
							subimageDataView.getImageData());
					this.checkGLErrors(gl, view + ".afterSubimageData");
				}
				
			}

			if (filter instanceof GLPostFilter) {
				((GLPostFilter) filter).postApplyGL(gl);
				this.checkGLErrors(gl, view + ".afterGLPostFilter");
			}

			gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);

		} else {
			textureHelper.renderImageDataToScreen(gl, regionView.getLastDecodedRegion(),
					getImageData());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ImageData getImageData() {
		if (!filteredDataIsUpToDate) {
			refilter();
		}

		return super.getImageData();
	}

	/**
	 * {@inheritDoc}
	 */
	public void viewChanged(View sender, ChangeEvent aEvent) {
		if (!(filter instanceof GLFilter)) {
			super.viewChanged(sender, aEvent);
		} else {
			if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
				updatePrecomputedViews();
				refilter();
			}

			if (aEvent.reasonOccurred(RegionChangedReason.class)
					|| aEvent.reasonOccurred(SubImageDataChangedReason.class)) {
				filteredDataIsUpToDate = false;
			}

			notifyViewListeners(aEvent);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void filterChanged(Filter f) {
		if (!(filter instanceof GLFilter)) {
			super.filterChanged(f);
		} else {
			filteredDataIsUpToDate = false;

			ChangeEvent event = new ChangeEvent();

			event.addReason(new FilterChangedReason(this, filter));
			event.addReason(new SubImageDataChangedReason(this));

			notifyViewListeners(event);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
		if (!(filter instanceof GLFilter)) {
			return shaderBuilder;
		}

		GLFragmentShaderView nextView = view
				.getAdapter(GLFragmentShaderView.class);
		if (nextView != null) {
			shaderBuilder = nextView.buildFragmentShader(shaderBuilder);
		}

		if (filter instanceof GLFragmentShaderFilter) {
			shaderBuilder = ((GLFragmentShaderFilter) filter)
					.buildFragmentShader(shaderBuilder);
		}

		return shaderBuilder;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void updatePrecomputedViews() {
		super.updatePrecomputedViews();
		viewportView = ViewHelper.getViewAdapter(view, ViewportView.class);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void refilter() {
		super.refilter();
		filteredDataIsUpToDate = (filteredData != null);
	}

	public boolean checkGLErrors(GL gl, String message) {
		if (gl == null) {
			Log.warn("OpenGL not yet Initialised!");
			return true;
		}
		int glErrorCode = gl.glGetError();

		if (glErrorCode != GL.GL_NO_ERROR) {
			GLU glu = new GLU();
			Log.error("GL Error (" + glErrorCode + "): "
					+ glu.gluErrorString(glErrorCode) + " - @" + message);
			if (glErrorCode == GL.GL_INVALID_OPERATION) {
				// Find the error position
				int[] err = new int[1];
				gl.glGetIntegerv(GL2.GL_PROGRAM_ERROR_POSITION_ARB, err, 0);
				if (err[0] >= 0) {
					String error = gl
							.glGetString(GL2.GL_PROGRAM_ERROR_STRING_ARB);
					Log.error("GL error at " + err[0] + ":\n" + error);
				}
			}
			return true;
		} else {
			return false;
		}
	}
}