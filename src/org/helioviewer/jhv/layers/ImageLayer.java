package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.JHVException;
import org.helioviewer.jhv.JHVException.CacheException;
import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.JHVException.TextureException;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.KakaduRender;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.UltimateLayer;
import org.json.JSONException;
import org.json.JSONObject;

public class ImageLayer extends LayerInterface {

	private static final String LAYERS = "layers";
	private static final String IS_LOCALFILE = "isLocalFile";
	private static final String LOCAL_PATH = "localPath";
	private static final String ID = "id";
	private static final String CADENCE = "cadence";
	private static final String START_DATE_TIME = "startDateTime";
	private static final String END_DATE_TIME = "endDateTime";

	private UltimateLayer ultimateLayer;

	private LayerRayTrace layerRayTrace;
	private int sourceID;
	
	public ImageLayer(int sourceID, KakaduRender newRender,
			LocalDateTime start, LocalDateTime end, int cadence, String name) {
		super();
		this.downloadable = true;
		this.sourceID = sourceID;
		this.start = start;
		this.end = end;
		this.cadence = cadence;
		this.name = name;
		this.ultimateLayer = new UltimateLayer(id, sourceID, newRender, this);
		this.ultimateLayer.setTimeRange(start, end, cadence);
		layerRayTrace = new LayerRayTrace(this);
	}

	public ImageLayer(String uri, KakaduRender newRender) {
		super();
		this.localPath = uri;
		this.ultimateLayer = new UltimateLayer(id, uri, newRender, this);
		this.name = ultimateLayer.getMetaData(0, uri).getFullName();
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
		} catch (MetaDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CacheException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public LocalDateTime getTime() throws MetaDataException {
		return getMetaData(TimeLine.SINGLETON.getCurrentDateTime()).getLocalDateTime();
	}

	@Deprecated
	public TreeSet<LocalDateTime> getLocalDateTime() {
		return ultimateLayer.getLocalDateTimes();
	}

	@Override
	protected MetaData getMetaData() throws JHVException.MetaDataException {
		if (getLastDecodedImageRegion() == null
				|| getLastDecodedImageRegion().getMetaData() == null)
			throw new JHVException.MetaDataException("No imagedata available");
		return this.getLastDecodedImageRegion().getMetaData();
	}

	public ImageRegion getLastDecodedImageRegion() {
		return ultimateLayer.getImageRegion();
	}

	public void cancelDownload() {
		ultimateLayer.cancelDownload();
	}

	@Override
	public String getURL(){
		return ultimateLayer.getURL();
	}

	@Override
	public MetaData getMetaData(LocalDateTime currentDateTime)
			throws MetaDataException {
		MetaData metaData = null;
		try {
			if (getMetaData().getLocalDateTime().isEqual(currentDateTime) && getLastDecodedImageRegion().getID() == this.id)
				return getMetaData();
			metaData = ultimateLayer.getMetaData(currentDateTime);
			if (metaData == null)
				throw new JHVException.MetaDataException("No metadata available");
		} catch (InterruptedException e) {
			throw new MetaDataException("No metadata available \nbecause :"
					+ e.getMessage());
		} catch (ExecutionException e) {
			throw new MetaDataException("No metadata available \nbecause :"
					+ e.getMessage());
		} catch (JHV_KduException e) {
			throw new MetaDataException("No metadata available \nbecause :"
					+ e.getMessage());
		} catch (CacheException e) {
			throw new MetaDataException("No metadata available \nbecause :"
					+ e.getMessage());
		} catch (JHVException.MetaDataException e) {
			try {
				metaData = ultimateLayer.getMetaData(currentDateTime);
				if (metaData == null)
					throw new MetaDataException("No metadata available");
			} catch (InterruptedException | ExecutionException
					| JHV_KduException | CacheException e1) {
				throw new JHVException.MetaDataException("No metadata available \nbecause :"
						+ e1.getMessage());
			}
		}
		return metaData;
	}

	public void writeStateFile(JSONObject jsonLayer) {
		try {
			jsonLayer.put(IS_LOCALFILE, ultimateLayer.isLocalFile());
			jsonLayer.put(LOCAL_PATH, getLocalFilePath());
			jsonLayer.put(ID, sourceID);
			jsonLayer.put(CADENCE, cadence);
			jsonLayer.put(START_DATE_TIME, start);
			jsonLayer.put(END_DATE_TIME, end);
			super.writeStateFile(jsonLayer);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
