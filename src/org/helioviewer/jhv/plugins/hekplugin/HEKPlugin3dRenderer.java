package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.Vector;

import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Vector3dDouble;
import org.helioviewer.jhv.base.wcs.conversion.SphericalToSolarSphereConversion;
import org.helioviewer.jhv.base.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.jhv.base.wcs.impl.SphericalCoordinateSystem;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent.GenericTriangle;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.renderer.physical.PhysicalRenderGraphics;
import org.helioviewer.jhv.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.TimedMovieView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DLayeredView;

/**
 * The solar event renderer provides a possibility to draw solar events with
 * there associated icons.
 * 
 * @author Malte Nuhn
 */
public class HEKPlugin3dRenderer extends PhysicalRenderer3d {
	private SphericalCoordinateSystem sphericalCS = new SphericalCoordinateSystem();
    private SolarSphereCoordinateSystem solarSphereCS = new SolarSphereCoordinateSystem();
    private float scale = 1;
    SphericalToSolarSphereConversion conversion = (SphericalToSolarSphereConversion) sphericalCS.getConversion(solarSphereCS);

	/**
	 * Default constructor.
	 */
	public HEKPlugin3dRenderer() {
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
	public void drawPolygon(PhysicalRenderGraphics g, HEKEvent evt, Date now) {

		if (evt != null && evt.isVisible(now)) {

			String type = evt.getString("event_type");
			Color eventColor = HEKConstants.getSingletonInstance()
					.acronymToColor(type, 128);

			Vector<HEKEvent.GenericTriangle<Vector3dDouble>> triangles = evt.getTriangulation3D(now);

			if (triangles != null) {
				g.setColor(eventColor);
				for (GenericTriangle<Vector3dDouble> triangle : triangles) {
					Vector3dDouble tri[] = { triangle.A, triangle.B, triangle.C };
					g.fillPolygon(tri);
				}
			}

			// draw bounds
			g.setColor(new Color(255, 255, 255, 255));
			

			Vector<SphericalCoord> outerBound = evt.getStonyBound(now);
			Vector3dDouble oldBoundaryPoint3d = null;

			if (outerBound != null) {
			{
	      //sf: shifting depthrange won't work properly, since it's in linear space, instead of inverse linear.
	      //--> large shifts far away, almost no shifts near camera. this is exactly the opposite of whate we want... :(
	      /*GL gl=g.getGL();
	      if(gl!=null)
	      {
	        gl.glDepthRange(-0.00012, 0.99988);
	      }*/
	      
				for (SphericalCoord boundaryPoint : outerBound) {
					Vector3dDouble boundaryPoint3d = HEKEvent.convertToSceneCoordinates(boundaryPoint, now, 1.005);
					
					if (oldBoundaryPoint3d != null) {
						g.drawLine3d(oldBoundaryPoint3d.getX(), oldBoundaryPoint3d.getY(),oldBoundaryPoint3d.getZ(), boundaryPoint3d.getX(), boundaryPoint3d.getY(), boundaryPoint3d.getZ());
						
					}

					oldBoundaryPoint3d = boundaryPoint3d;
				}
        /*if(gl!=null)
        {
          gl.glDepthRange(0, 1);
        }*/
			}
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
	public void drawIcon(PhysicalRenderGraphics g, HEKEvent evt, Date now) {
		if (evt != null && evt.isVisible(now)) {
			boolean large = evt.getShowEventInfo();
			BufferedImage icon = evt.getIcon(large);
			if (icon != null) {
				SphericalCoord stony = evt.getStony(now);
				Vector3dDouble coords = HEKEvent.convertToSceneCoordinates(stony, now);
				g.drawImage3d(icon, coords.getX(), coords.getY(), coords.getZ(), scale);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Draws all available and visible solar events with there associated icon.
	 */
	public void render(PhysicalRenderGraphics g) {
		TimedMovieView masterView = LinkedMovieManager.getActiveInstance()
				.getMasterMovie();
		if (masterView != null && masterView.getCurrentFrameDateTime() != null) {
			Date currentDate = masterView.getCurrentFrameDateTime().getTime();

			if (currentDate != null) {
				Vector<HEKEvent> toDraw = HEKCache.getSingletonInstance()
						.getModel().getActiveEvents(currentDate);
				
				for (HEKEvent evt : toDraw) {
					drawPolygon(g, evt, currentDate);
				}

				for (HEKEvent evt : toDraw) {
					drawIcon(g, evt, currentDate);
				}
			}
			GL3DState.get().checkGLErrors("HEKPlugin3dRenderer.afterRender");
		}
	}
	
	public void viewChanged(View view){
		GL3DLayeredView layeredView = ViewHelper.getViewAdapter(view, GL3DLayeredView.class);
		if (layeredView != null){
			double heigth = -1;
			for (int i = 0; i < layeredView.getNumLayers(); i++){
				if (layeredView.getLayer(i).getAdapter(RegionView.class) != null && heigth < layeredView.getLayer(i).getAdapter(RegionView.class).getRegion().getHeight())
					heigth = layeredView.getLayer(i).getAdapter(RegionView.class).getRegion().getHeight();
			}
			Region region = view.getAdapter(RegionView.class).getRegion();
			if (region != null){
				scale = (float) (heigth / region.getHeight());
			}
		}

    }
	
}
