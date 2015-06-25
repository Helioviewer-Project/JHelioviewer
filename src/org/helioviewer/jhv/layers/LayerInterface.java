package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.TreeSet;

import org.helioviewer.jhv.MetaDataException;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public abstract class LayerInterface {

	public enum SHADER_STATE {
		FALSE, TRUE;
	}

	public enum COLOR_CHANNEL_TYPE {
		RED, GREEN, BLUE;
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

	private static int idCounter;
	protected int id;
	protected LocalDateTime start;
	protected LocalDateTime end;
	protected int cadence = -1;
	protected String localPath;

	public abstract int getTexture(MainPanel compenentView,
			boolean highResolution, Dimension size);

	public abstract String getName();

	public abstract LocalDateTime getTime();

	public abstract TreeSet<LocalDateTime> getLocalDateTime();

	public abstract MetaData getMetaData() throws MetaDataException;

	public abstract void cancelDownload();

	public boolean isVisible() {
		return visible;
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

	abstract public String getURL() throws LocalFileException;

	public String getLocalFilePath() throws LocalFileException {
		if (localPath == null)
			throw new LocalFileException("No filepath, that are remote data");
		return localPath;
	}

	public int getCadence() throws LocalFileException {
		if (cadence < 0)
			throw new LocalFileException("No remote connection available");
		return cadence;
	}

	public LocalDateTime getStartDateTime() throws LocalFileException {
		if (start == null)
			throw new LocalFileException("No remote connection available");
		return start;
	}

	public LocalDateTime getEndDateTime() throws LocalFileException {
		if (end == null)
			throw new LocalFileException("No remote connection available");
		return end;
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
}
