package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.base.coordinates.HeliographicCoordinate;
import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.layers.PluginLayer;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKCache;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent;
import org.helioviewer.jhv.plugins.hekplugin.cache.HEKEvent.GenericTriangle;
import org.helioviewer.jhv.plugins.hekplugin.cache.gui.HEKEventInformationDialog;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.json.JSONObject;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class HEKPlugin extends Plugin
{
	/*private static final String JSON_NAME = "hek";
	private static final String JSON_EVENTS = "events";*/

	private static final Cursor CURSOR_HELP = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

	private Cursor lastCursor;
	private HEKEventInformationDialog hekPopUp = new HEKEventInformationDialog();

	private HEKEvent mouseOverHEKEvent = null;
	private Point mouseOverPosition = null;

	private static final int X_OFFSET = 12;
	private static final int Y_OFFSET = 12;

	private HEKPluginPanel hekPluginPanel;

	public HEKPlugin()
	{
		super("HEK", RenderMode.MAIN_PANEL);
		hekPluginPanel = new HEKPluginPanel(HEKCache.getSingletonInstance());
		
		Plugins.addPanelToLeftControllPanel("HEK", hekPluginPanel, false);
	}
	
	@Override
	public void render(GL2 gl, PluginLayer _imageParams)
	{
		//FIXME: hek icons broken in os x
		if (isVisible())
		{
			LocalDateTime in = Plugins.SINGLETON.getCurrentDateTime();
			if (in != null)
			{
				Date currentDate = Date.from(in.atZone(ZoneId.systemDefault()).toInstant());
				List<HEKEvent> toDraw = HEKCache.getSingletonInstance().getModel().getActiveEvents(currentDate);
				if (toDraw != null && toDraw.size() > 0)
				{

					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glEnable(GL2.GL_CULL_FACE);
					gl.glEnable(GL2.GL_LINE_SMOOTH);
					gl.glEnable(GL2.GL_BLEND);

					for (HEKEvent evt : toDraw)
						drawPolygon(gl, evt, currentDate);

					gl.glDisable(GL2.GL_LINE_SMOOTH);

					gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
					gl.glDisable(GL2.GL_DEPTH_TEST);
					gl.glEnable(GL2.GL_TEXTURE_2D);
					gl.glColor4f(1.0f, 1.0f, 1.0f, 1);

					for (HEKEvent evt : toDraw)
						drawIcon(gl, evt, currentDate);

					gl.glDisable(GL2.GL_TEXTURE_2D);
					gl.glDisable(GL2.GL_BLEND);
					gl.glEnable(GL2.GL_DEPTH_TEST);
					gl.glDisable(GL2.GL_CULL_FACE);
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
	public void drawIcon(GL2 gl, HEKEvent evt, Date now)
	{
		if (evt == null || !evt.isVisible(now))
			return;

		boolean large = evt.getShowEventInfo();
		String type = evt.getString("event_type");
		int offSetFactor = -1;
		for (HEKIcon.HEKIcons hekIcon : HEKIcon.HEKIcons.values())
		{
			if (hekIcon.name().startsWith(type))
			{
				offSetFactor = hekIcon.ordinal();
				break;
			}
		}
		if (offSetFactor >= 0)
		{
			SphericalCoord heliographicCoordinate = evt.getStony(now);
			Vector3d coords = HEKEvent.convertToSceneCoordinates(heliographicCoordinate, now);
			double x = coords.x;
			double y = coords.y;
			double z = coords.z;

			gl.glEnable(GL2.GL_TEXTURE_2D);
			gl.glBindTexture(GL2.GL_TEXTURE_2D, HEKIcon.getOpenGLTextureId());
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

			float imageScaleFactorH = HEKIcon.getImageScaleFactorHeight();

			double scale = large ? 0.0004 : 0.0002;
			double width2 = Plugins.getViewPortSize() * scale;
			double height2 = Plugins.getViewPortSize() * scale;

			Vector3d sourceDir = new Vector3d(0, 0, -1);
			Vector3d targetDir = new Vector3d(x, y, z);

			double angle = Math.acos(sourceDir.dot(targetDir) / (sourceDir.length() * targetDir.length()));
			Vector3d axis = sourceDir.cross(targetDir);
			Matrix4d r = Matrix4d.createRotationMatrix(angle, axis.normalized()).translatedAbsolute(x, y, z);

			Vector3d p0 = new Vector3d(-width2, -height2, 0);
			Vector3d p1 = new Vector3d(-width2, height2, 0);
			Vector3d p2 = new Vector3d(width2, height2, 0);
			Vector3d p3 = new Vector3d(width2, -height2, 0);

			p0 = r.multiply(p0);
			p1 = r.multiply(p1);
			p2 = r.multiply(p2);
			p3 = r.multiply(p3);

			gl.glColor4f(1, 1, 1, 1);

			gl.glBegin(GL2.GL_QUADS);

			gl.glTexCoord2f(0.0f, offSetFactor * imageScaleFactorH);
			gl.glVertex3d(p0.x, p0.y, p0.z);
			gl.glTexCoord2f(0.0f, (offSetFactor + 1) * imageScaleFactorH);
			gl.glVertex3d(p1.x, p1.y, p1.z);
			gl.glTexCoord2f(1.0f, (offSetFactor + 1) * imageScaleFactorH);
			gl.glVertex3d(p2.x, p2.y, p2.z);
			gl.glTexCoord2f(1.0f, offSetFactor * imageScaleFactorH);
			gl.glVertex3d(p3.x, p3.y, p3.z);

			gl.glEnd();
			gl.glDisable(GL2.GL_TEXTURE_2D);
		}
	}

	public void drawPolygon(GL2 gl, HEKEvent evt, Date now)
	{
		if (evt == null || !evt.isVisible(now))
			return;

		List<HEKEvent.GenericTriangle<Vector3d>> triangles = evt.getTriangulation3D(now);
		List<SphericalCoord> outerBound = evt.getStonyBound(now);
		if (outerBound == null && triangles == null)
			return;

		String type = evt.getString("event_type");
		Color eventColor = HEKConstants.getSingletonInstance().acronymToColor(type, 128);

		HeliographicCoordinate heliographicCoordinate = evt.getHeliographicCoordinate(now);
		if (heliographicCoordinate == null)
			return;

		gl.glPushMatrix();
		gl.glRotated(DifferentialRotation.calculateRotationInDegrees(heliographicCoordinate.latitude,
				(now.getTime() - evt.getStart().getTime()) / 1000d), 0, 1, 0);

		if (triangles != null)
		{
			gl.glColor4ub((byte) eventColor.getRed(), (byte) eventColor.getGreen(), (byte) eventColor.getBlue(),
					(byte) eventColor.getAlpha());

			gl.glEnable(GL2.GL_CULL_FACE);
			gl.glDisable(GL2.GL_DEPTH_TEST);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

			gl.glBegin(GL2.GL_TRIANGLES);
			for (GenericTriangle<Vector3d> triangle : triangles)
			{
				gl.glVertex3d(triangle.A.x, triangle.A.y, triangle.A.z);
				gl.glVertex3d(triangle.B.x, triangle.B.y, triangle.B.z);
				gl.glVertex3d(triangle.C.x, triangle.C.y, triangle.C.z);
			}
			gl.glEnd();
		}

		// draw bounds
		gl.glColor4f(1, 1, 1, 1);
		if (outerBound != null)
		{
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(GL2.GL_DEPTH_TEST);

			gl.glBegin(GL.GL_LINE_LOOP);
			for (SphericalCoord boundaryPoint : outerBound)
			{
				Vector3d boundaryPoint3d = HEKEvent.convertToSceneCoordinates(boundaryPoint, now).scaled(1.005);
				gl.glVertex3d(boundaryPoint3d.x, boundaryPoint3d.y, boundaryPoint3d.z);
			}
			gl.glEnd();
		}

		gl.glPopMatrix();
	}

	@Override
	public void mouseClicked(MouseEvent e, Vector3d point)
	{
		if (mouseOverHEKEvent != null)
		{

			// should never be the case
			if (hekPopUp == null)
			{
				hekPopUp = new HEKEventInformationDialog();
			}

			hekPopUp.setVisible(false);
			hekPopUp.setEvent(mouseOverHEKEvent);

			Point windowPosition = calcWindowPosition(mouseOverPosition);
			hekPopUp.setLocation(windowPosition);
			hekPopUp.setVisible(true);
			hekPopUp.pack();
			Plugins.setCursor(CURSOR_HELP);

			Plugins.repaintMainPanel();
		}
	}

	private Point calcWindowPosition(Point p)
	{
		int yCoord;
		boolean yCoordInMiddle = false;
		if (p.y + hekPopUp.getSize().height + Y_OFFSET < Plugins.mainPanelGetSize().height)
		{
			yCoord = p.y + Plugins.mainPanelGetLocationOnScreen().y + Y_OFFSET;
		}
		else
		{
			yCoord = p.y + Plugins.mainPanelGetLocationOnScreen().y - hekPopUp.getSize().height - Y_OFFSET;
			if (yCoord < Plugins.mainPanelGetLocationOnScreen().y)
			{
				yCoord = Plugins.mainPanelGetLocationOnScreen().y + Plugins.mainPanelGetSize().height
						- hekPopUp.getSize().height;

				if (yCoord < Plugins.mainPanelGetLocationOnScreen().y)
				{
					yCoord = Plugins.mainPanelGetLocationOnScreen().y;
				}

				yCoordInMiddle = true;
			}
		}

		int xCoord;
		if (p.x + hekPopUp.getSize().width + X_OFFSET < Plugins.mainPanelGetSize().width)
		{
			xCoord = p.x + Plugins.mainPanelGetLocationOnScreen().x + X_OFFSET;
		}
		else
		{
			xCoord = p.x + Plugins.mainPanelGetLocationOnScreen().x - hekPopUp.getSize().width - X_OFFSET;
			if (xCoord < Plugins.mainPanelGetLocationOnScreen().x && !yCoordInMiddle)
			{
				xCoord = Plugins.mainPanelGetLocationOnScreen().x + Plugins.mainPanelGetSize().width
						- hekPopUp.getSize().width;
			}
		}

		return new Point(xCoord, yCoord);

	}

	public void dateTimesChanged(int framecount)
	{
		LocalDateTime startDateTime;
		startDateTime = Plugins.getStartDateTime();
		LocalDateTime endDateTime = Plugins.getEndDateTime();
		if (startDateTime != null && endDateTime != null)
		{
			Date start = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
			Date end = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());

			Interval<Date> newInterval = new Interval<>(start, end);
			hekPluginPanel.setCurInterval(newInterval);
		}
	}

	public void mouseMoved(MouseEvent e, Vector3d point)
	{
		HEKEvent lastHEKEvent = mouseOverHEKEvent;

		LocalDateTime in = Plugins.SINGLETON.getCurrentDateTime();
		if (in != null)
		{
			Date currentDate = Date.from(in.atZone(ZoneId.systemDefault()).toInstant());

			mouseOverHEKEvent = null;
			mouseOverPosition = null;

			List<HEKEvent> toDraw = HEKCache.getSingletonInstance().getModel().getActiveEvents(currentDate);
			if (toDraw.size() > 0)
			{
				for (HEKEvent evt : toDraw)
				{
					SphericalCoord stony = evt.getStony(currentDate);
					Vector3d coords = HEKEvent.convertToSceneCoordinates(stony, currentDate);

					double deltaX = Math.abs(point.x - coords.x);
					double deltaY = Math.abs(-point.y - coords.y);
					double deltaZ = Math.abs(point.z - coords.z);
					if (deltaX < 10000000 && deltaZ < 10000000 && deltaY < 10000000)
					{
						mouseOverHEKEvent = evt;
						mouseOverPosition = new Point(e.getX(), e.getY());
					}

				}

				if (lastHEKEvent == null && mouseOverHEKEvent != null)
				{
					lastCursor = Plugins.getCursor();
					Plugins.setCursor(CURSOR_HELP);
				}
				else if (lastHEKEvent != null && mouseOverHEKEvent == null)
				{
					Plugins.setCursor(lastCursor);
				}
			}
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * null because this is an internal plugin
	 */
	public String getAboutLicenseText()
	{
		String description = "";
		description += "<p>"
				+ "This software uses the <a href=\"http://www.json.org/java/\">JSON in Java</a> Library, licensed under a <a href=\"http://www.json.org/license.html\">custom License</a>.";

		return description;
	}

	public static URL getResourceUrl(String name)
	{
		return HEKPlugin.class.getResource(name);
	}

	@Override
	public void restoreConfiguration(JSONObject jsonObject)
	{
		/*if (jsonObject.has(JSON_NAME))
		{
			try
			{
				JSONObject jsonHek = jsonObject.getJSONObject(JSON_NAME);
			}
			catch (JSONException e)
			{
				Telemetry.trackException(e);
			}
		}*/
	}

	@Override
	public void storeConfiguration(JSONObject jsonObject)
	{
		/*JSONObject jsonHek = new JSONObject();
		JSONArray jsonHekEvents = new JSONArray();
		try
		{
			for (HEKPath hekPath : HEKCache.getSingletonInstance().getTrackPaths())
			{
				int state = HEKCache.getSingletonInstance().getSelectionModel().getState(hekPath);
				jsonHekEvents.put(state);
			}
			jsonHek.put(JSON_EVENTS, jsonHekEvents);
			jsonObject.put(JSON_NAME, jsonHek);
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}*/
	}

	@Override
	public boolean retryNeeded()
	{
		return false;
	}

	@Override
	public void retry()
	{
	}
}