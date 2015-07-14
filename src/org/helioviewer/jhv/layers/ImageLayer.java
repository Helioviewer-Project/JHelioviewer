package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.JHVException;
import org.helioviewer.jhv.JHVException.CacheException;
import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.JHVException.TextureException;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.Cache;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.KakaduRender;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.UltimateLayer;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class ImageLayer extends LayerInterface {

	private static final String LAYERS = "layers";
	private static final String IS_LOCALFILE = "isLocalFile";
	private static final String LOCAL_PATH = "localPath";
	private static final String ID = "id";
	private static final String CADENCE = "cadence";
	private static final String START_DATE_TIME = "startDateTime";
	private static final String END_DATE_TIME = "endDateTime";
	private static final String NAME = "name";

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
		ArrayList<AbstractRequest> badRequests = new ArrayList<AbstractRequest>();
		badRequests.add(new HTTPRequest("test", PRIORITY.HIGH));
		this.addBadRequest(badRequests);
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
					firstRun = false;
					if (lut == LUT_ENTRY.GRAY){
						this.lut = metaData.getDefaultLUT();
					MainFrame.FILTER_PANEL.updateLayer(this);
					MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
					}
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
			//e.printStackTrace();
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
			jsonLayer.put(NAME, name);
			super.writeStateFile(jsonLayer);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static ImageLayer readStateFile(JSONObject jsonLayer, KakaduRender kakaduRender){
		try {
			if (jsonLayer.getBoolean(IS_LOCALFILE)){
				return new ImageLayer(jsonLayer.getString(LOCAL_PATH), kakaduRender);
			}
			else if (jsonLayer.getInt(CADENCE) >= 0){
				LocalDateTime start = LocalDateTime.parse(jsonLayer.getString(START_DATE_TIME));
				LocalDateTime end = LocalDateTime.parse(jsonLayer.getString(END_DATE_TIME));
				return new ImageLayer(jsonLayer.getInt(ID), kakaduRender, start, end, jsonLayer.getInt(CADENCE), jsonLayer.getString(NAME));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public CacheableImageData getCacheStatus(LocalDateTime localDateTime) {
		return Cache.getCacheElement(id, localDateTime);
	}
	
	public void renderLayer(GL2 gl){
		
	}

}
