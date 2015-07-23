package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentSkipListSet;

import org.helioviewer.jhv.JHVException;
import org.helioviewer.jhv.JHVException.TextureException;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractImageLayer extends AbstractLayer{

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
	
	public enum SHADER_STATE {
		FALSE, TRUE;
	}

	public enum COLOR_CHANNEL_TYPE {
		RED, GREEN, BLUE;
	}
	
	public enum CACHE_STATUS{
		FILE, KDU, NONE;
	}

	// Channelfilter
	public static class ColorChannel {
		private SHADER_STATE status = SHADER_STATE.TRUE;
		private String name;

		public ColorChannel(String name) {
			this.name = name;
		}

		public boolean isActivated() {
			return this.status == SHADER_STATE.TRUE;
		}

		public void setActive(boolean status) {
			this.status = status ? SHADER_STATE.TRUE : SHADER_STATE.FALSE;
		}

		public int getState() {
			return this.status == SHADER_STATE.TRUE ? 1 : 0;
		}
	}

	private double opacity = 1;
	private double sharpen = 0;
	private double gamma = 1;
	private double contrast = 1;;
	protected LUT_ENTRY lut = LUT_ENTRY.GRAY;
	private ColorChannel redChannel = new ColorChannel("red");
	private ColorChannel greenChannel = new ColorChannel("green");
	private ColorChannel blueChannel = new ColorChannel("blue");
	private boolean visible = true;
	protected boolean invertedLut = false;
	protected boolean coronaVisibility = true;

	protected boolean firstRun = true;

	private static int idCounter = 0;
	protected int id;
	protected LocalDateTime start;
	protected LocalDateTime end;
	protected int cadence = -1;
	protected String localPath;

	protected String name;
	
	public abstract int getTexture(MainPanel compenentView,
			boolean highResolution, Dimension size) throws TextureException;

	public String getName(){
		return name;
	}

	public abstract LocalDateTime getTime();

	public abstract ConcurrentSkipListSet<LocalDateTime> getLocalDateTime();

	protected abstract MetaData getMetaData() throws JHVException.MetaDataException;

	public boolean isVisible() {
		return visible;
	}
	
	public AbstractImageLayer() {
		id = idCounter++;
		isImageLayer = true;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
	}

	public double getContrast() {
		return contrast;
	}

	public void setContrast(double contrast) {
		this.contrast = contrast;
	}

	public double getGamma() {
		return gamma;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public double getOpacity() {
		return opacity;
	}

	public void setOpacity(double opacity) {
		this.opacity = opacity;
	}

	public double getSharpen() {
		return sharpen;
	}

	public void setSharpen(double sharpen) {
		this.sharpen = sharpen;
	}

	public LUT_ENTRY getLut() {
		return lut;
	}

	public ColorChannel getColorChannel(COLOR_CHANNEL_TYPE type) {
		switch (type) {
		case RED:
			return redChannel;
		case GREEN:
			return greenChannel;
		case BLUE:
			return blueChannel;
		default:
			break;
		}
		return null;
	}

	public int getID() {
		return id;
	}

	public void setLut(LUT_ENTRY lutEntry) {
		this.lut = lutEntry;
	}

	public boolean isLutInverted() {
		return invertedLut;
	}

	public void setLutInverted(boolean selected) {
		this.invertedLut = selected;
	}

	public int getLutState() {
		return invertedLut ? 1 : 0;
	}

	abstract public String getURL();

	public String getLocalFilePath(){
		return localPath;
	}

	public boolean isRedChannelActive() {
		return redChannel.isActivated();
	}
	
	public void setRedChannel(boolean value){
		redChannel.setActive(value);
	}
	
	public boolean isGreenChannelActive() {
		return greenChannel.isActivated();
	}

	public void setGreenChannel(boolean value){
		greenChannel.setActive(value);
	}

	public boolean isBlueChannelActive() {
		return blueChannel.isActivated();
	}

	public void setBlueChannel(boolean value){
		blueChannel.setActive(value);
	}

	public boolean isCoronaVisible() {
		return coronaVisibility;
	}
	
	public void setCoronaVisibility(boolean value){
		coronaVisibility = value;
	}

	public abstract MetaData getMetaData(LocalDateTime currentDateTime) throws JHVException.MetaDataException;

	public void writeStateFile(JSONObject jsonLayer){
		try {
			jsonLayer.put(OPACITY, getOpacity());
			jsonLayer.put(SHARPEN, getSharpen());
			jsonLayer.put(GAMMA, getGamma());
			jsonLayer.put(CONTRAST, getContrast());
			jsonLayer.put(LUT, getLut().ordinal());
			jsonLayer.put(RED_CHANNEL, isRedChannelActive());
			jsonLayer.put(GREEN_CHANNEL, isGreenChannelActive());
			jsonLayer.put(BLUE_CHANNEL, isBlueChannelActive());

			jsonLayer.put(VISIBILITY, isVisible());
			jsonLayer.put(INVERTED_LUT, isLutInverted());
			jsonLayer.put(CORONA_VISIBILITY, isCoronaVisible());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void readStateFile(JSONObject jsonLayer){
		try {
			setOpacity(jsonLayer.getDouble(OPACITY));
			setSharpen(jsonLayer.getDouble(SHARPEN));
			setGamma(jsonLayer.getDouble(GAMMA));
			setContrast(jsonLayer.getDouble(CONTRAST));
			setLut(LUT_ENTRY.values()[jsonLayer.getInt(LUT)]);
			setRedChannel(jsonLayer.getBoolean(RED_CHANNEL));
			setGreenChannel(jsonLayer.getBoolean(GREEN_CHANNEL));
			setBlueChannel(jsonLayer.getBoolean(BLUE_CHANNEL));
			
			visible = jsonLayer.getBoolean(VISIBILITY);
			setLutInverted(jsonLayer.getBoolean(INVERTED_LUT));
			setCoronaVisibility(jsonLayer.getBoolean(CORONA_VISIBILITY));
			MainFrame.FILTER_PANEL.updateLayer(this);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public abstract CacheableImageData getCacheStatus(LocalDateTime localDateTime);
	public abstract ImageRegion getLastDecodedImageRegion();

	public int getCadence(){
		return cadence;
	}

	public abstract LocalDateTime getFirstLocalDateTime();
	public abstract LocalDateTime getLastLocalDateTime();
}
