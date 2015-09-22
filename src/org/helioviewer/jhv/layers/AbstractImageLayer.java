package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.NavigableSet;
import java.util.concurrent.Future;

import org.helioviewer.jhv.JHVException;
import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.JHVException.TextureException;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.json.JSONException;
import org.json.JSONObject;

//FIXME: get rid of this
public abstract class AbstractImageLayer extends AbstractLayer
{
	private static final String OPACITY = "opacity";
	private static final String SHARPEN = "sharpen";
	private static final String GAMMA = "gamma";
	private static final String CONTRAST = "contrast";
	private static final String LUT = "lut";
	private static final String RED_CHANNEL = "redChannel";
	private static final String GREEN_CHANNEL = "greenChannel";
	private static final String BLUE_CHANNEL = "blueChannel";

	private static final String VISIBILITY = "visibility";
	private static final String INVERTED_LUT = "invertedLut";
	private static final String CORONA_VISIBILITY = "coronaVisiblity";
	
	public enum CacheStatus
	{
		FILE_FULL, KDU_PREVIEW, NONE;
	}

	public double opacity = 1;
	public double sharpness = 0;
	public double gamma = 1;
	public double contrast = 1;
	protected LUT_ENTRY lut = null;
	public boolean redChannel = true;
	public boolean greenChannel = true;
	public boolean blueChannel = true;
	private boolean visible = true;
	public boolean invertedLut = false;
	protected boolean coronaVisible = true;

	private static int idCounter = 0;
	protected int layerId;
	protected LocalDateTime start;
	protected LocalDateTime end;
	protected int cadence = -1;
	protected String localPath;

	public abstract int getTexture(MainPanel compenentView, ByteBuffer _imageData, Dimension size) throws TextureException;

	public abstract LocalDateTime getTime();

	public abstract NavigableSet<LocalDateTime> getLocalDateTime();

	protected abstract MetaData getMetaData() throws JHVException.MetaDataException;

	public boolean isVisible()
	{
		return visible;
	}
	
	public AbstractImageLayer()
	{
		layerId = idCounter++;
		isImageLayer = true;
	}

	public void setVisible(boolean visible)
	{
		this.visible = visible;
		MainFrame.MAIN_PANEL.repaint();
	}

	public LUT_ENTRY getLut() {
		return lut;
	}

	public int getID() {
		return layerId;
	}

	public void setLut(LUT_ENTRY lutEntry) {
		this.lut = lutEntry;
	}

	abstract public String getURL();

	public String getLocalFilePath()
	{
		return localPath;
	}

	public abstract MetaData getMetaData(LocalDateTime currentDateTime) throws JHVException.MetaDataException;

	public void writeStateFile(JSONObject jsonLayer)
	{
		try
		{
			jsonLayer.put(OPACITY, opacity);
			jsonLayer.put(SHARPEN, sharpness);
			jsonLayer.put(GAMMA, gamma);
			jsonLayer.put(CONTRAST, contrast);
			jsonLayer.put(LUT, getLut().ordinal());
			jsonLayer.put(RED_CHANNEL, redChannel);
			jsonLayer.put(GREEN_CHANNEL, greenChannel);
			jsonLayer.put(BLUE_CHANNEL, blueChannel);

			jsonLayer.put(VISIBILITY, isVisible());
			jsonLayer.put(INVERTED_LUT, invertedLut);
			jsonLayer.put(CORONA_VISIBILITY, coronaVisible);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	public void readStateFile(JSONObject jsonLayer)
	{
		try
		{
			this.opacity = jsonLayer.getDouble(OPACITY);
			this.sharpness = jsonLayer.getDouble(SHARPEN);
			this.gamma = jsonLayer.getDouble(GAMMA);
			this.contrast = jsonLayer.getDouble(CONTRAST);
			setLut(LUT_ENTRY.values()[jsonLayer.getInt(LUT)]);
			redChannel=jsonLayer.getBoolean(RED_CHANNEL);
			greenChannel=jsonLayer.getBoolean(GREEN_CHANNEL);
			blueChannel=jsonLayer.getBoolean(BLUE_CHANNEL);
			
			visible = jsonLayer.getBoolean(VISIBILITY);
			invertedLut = jsonLayer.getBoolean(INVERTED_LUT);
			coronaVisible=jsonLayer.getBoolean(CORONA_VISIBILITY);
			MainFrame.FILTER_PANEL.updateUIElements(this);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
	
	public abstract Movie getMovie(LocalDateTime localDateTime);
	
	//FIXME: remove
	public abstract ImageRegion getLastDecodedImageRegion();

	public int getCadence()
	{
		return cadence;
	}

	public abstract LocalDateTime getFirstLocalDateTime();
	public abstract LocalDateTime getLastLocalDateTime();
	
	public abstract Future<ByteBuffer> prepareImageData(final MainPanel mainPanel, final Dimension size) throws MetaDataException;

	public void toggleCoronaVisibility()
	{
		coronaVisible=!coronaVisible;
	}
}
