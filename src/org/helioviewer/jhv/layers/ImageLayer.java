package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.MetaDataException;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.layers.JHVException.TextureException;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.KakaduRender;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.UltimateLayer;

public class ImageLayer extends LayerInterface {

	private UltimateLayer ultimateLayer;

	private LayerRayTrace layerRayTrace;

	public ImageLayer(int sourceID, KakaduRender newRender,
			LocalDateTime start, LocalDateTime end, int cadence) {
		this.id = sourceID;
		this.start = start;
		this.end = end;
		this.cadence = cadence;
		this.ultimateLayer = new UltimateLayer(id, sourceID, newRender, this);
		this.ultimateLayer.setTimeRange(start, end, cadence);
		layerRayTrace = new LayerRayTrace(this);
	}

	public ImageLayer(String uri, KakaduRender newRender) {
		this.localPath = uri;
		this.ultimateLayer = new UltimateLayer(id, uri, newRender, this);
		layerRayTrace = new LayerRayTrace(this);
	}

	@Override
	public int getTexture(MainPanel compenentView, boolean highResolution,
			Dimension size) throws TextureException {
		if (updateTexture(TimeLine.SINGLETON.getCurrentDateTime(),
				compenentView, highResolution, size)) {
			return this.getLastDecodedImageRegion().getTextureID();
		}
		throw new JHVException.TextureException("no texture available");
	}

	private boolean updateTexture(LocalDateTime currentDateTime,
			MainPanel compenentView, boolean highResolution, Dimension size) {
		try {
			MetaData metaData = ultimateLayer.getMetaData(currentDateTime);
			if (metaData != null) {
				if (firstRun) {
					this.lut = metaData.getDefaultLUT();
					firstRun = false;
					MainFrame.SINGLETON.filterTabPanel.updateLayer(this);
					MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
				}
				ByteBuffer byteBuffer = ultimateLayer.getImageData(
						currentDateTime, layerRayTrace.getCurrentRegion(
								compenentView, metaData, size), metaData,
						highResolution);
				if (byteBuffer != null) {
					OpenGLHelper.bindByteBufferToGLTexture(this
							.getLastDecodedImageRegion(), byteBuffer, this
							.getLastDecodedImageRegion().getImageSize());
				}
				return true;
			}
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JHV_KduException e) {
		}
		return false;
	}

	@Override
	public String getName() {
		if (this.getLastDecodedImageRegion() == null)
			return "";
		return this.getLastDecodedImageRegion().getMetaData().getFullName();
	}

	@Override
	public LocalDateTime getTime() {
		if (getLastDecodedImageRegion() == null)
			return null;
		return getLastDecodedImageRegion().getDateTime();
	}

	@Deprecated
	public TreeSet<LocalDateTime> getLocalDateTime() {
		return ultimateLayer.getLocalDateTimes();
	}

	@Override
	protected MetaData getMetaData() throws MetaDataException {
		if (getLastDecodedImageRegion() == null
				|| getLastDecodedImageRegion().getMetaData() == null)
			throw new MetaDataException("No imagedata available");
		return this.getLastDecodedImageRegion().getMetaData();
	}

	public ImageRegion getLastDecodedImageRegion() {
		return ultimateLayer.getImageRegion();
	}

	public void cancelDownload() {
		ultimateLayer.cancelDownload();
	}

	@Override
	public String getURL() throws LocalFileException {
		return ultimateLayer.getURL();
	}

	@Override
	public MetaData getMetaData(LocalDateTime currentDateTime)
			throws MetaDataException {
		MetaData metaData = null;
		try {
			if (getMetaData().getLocalDateTime().isEqual(currentDateTime))
				return getMetaData();
			metaData = ultimateLayer.getMetaData(currentDateTime);
		} catch (InterruptedException e) {
			throw new MetaDataException("No metadata available \nbecause :"
					+ e.getMessage());
		} catch (ExecutionException e) {
			throw new MetaDataException("No metadata available \nbecause :"
					+ e.getMessage());
		} catch (JHV_KduException e) {
			throw new MetaDataException("No metadata available \nbecause :"
					+ e.getMessage());
		} catch (MetaDataException e) {
			try {
				metaData = ultimateLayer.getMetaData(currentDateTime);
				if (metaData == null)
					throw new MetaDataException("No metadata available");
			} catch (InterruptedException | ExecutionException
					| JHV_KduException e1) {
				throw new MetaDataException("No metadata available \nbecause :"
						+ e1.getMessage());
			}
		}
		return metaData;
	}
}
