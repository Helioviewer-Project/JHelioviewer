package org.helioviewer.jhv.layers;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewRender;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.UltimateLayer;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class NewLayer extends LayerInterface{
	
	
	private UltimateLayer ultimateLayer;
			
	private LayerRayTrace layerRayTrace;
	
	public NewLayer(int sourceID, NewRender newRender, NewCache newCache) {
		super();
		this.ultimateLayer = new UltimateLayer(id, sourceID, newCache, newRender, this);
		layerRayTrace = new LayerRayTrace(this);
	}
	
	public NewLayer(int sourceID, NewRender newRender, NewCache newCache, LocalDateTime start, LocalDateTime end, int cadence) {
		super();
		this.ultimateLayer = new UltimateLayer(id, sourceID, newCache, newRender, this);
		this.ultimateLayer.setTimeRange(start, end, cadence);
		layerRayTrace = new LayerRayTrace(this);
	}
	
	public NewLayer(String uri, NewRender newRender) {
		super();
		this.ultimateLayer = new UltimateLayer(id, uri, newRender, this);
		layerRayTrace = new LayerRayTrace(this);
	}
	
	@Override
	public int getTexture(MainPanel compenentView, boolean highResolution) {
		if (updateTexture(TimeLine.SINGLETON.getCurrentDateTime(), compenentView, highResolution))
			return this.getLastDecodedImageRegion().getTextureID();
		return -1;
	}
	
	private boolean updateTexture(LocalDateTime currentDateTime, MainPanel compenentView, boolean highResolution){
		try {
			MetaData metaData = ultimateLayer.getMetaData(currentDateTime);
			if (metaData != null){
				if (firstRun){
					this.lut = metaData.getDefaultLUT();
					firstRun = false;
					MainFrame.SINGLETON.filterTabPanel.updateLayer(this);
					MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
				}
			ByteBuffer byteBuffer = ultimateLayer.getImageData(currentDateTime, layerRayTrace.getCurrentRegion(compenentView, metaData), metaData, highResolution);
			if (byteBuffer != null){
				OpenGLHelper.bindByteBufferToGLTexture(this.getLastDecodedImageRegion(), byteBuffer, this.getLastDecodedImageRegion().getImageSize());
			}
			return true;
			}
		} catch (InterruptedException | ExecutionException | JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public String getName() {
		if (this.getLastDecodedImageRegion() == null) return "";
		return this.getLastDecodedImageRegion().getMetaData().getFullName();
	}

	@Override
	public LocalDateTime getTime() {
		if (getLastDecodedImageRegion() == null) return null;
		return getLastDecodedImageRegion().getDateTime();
	}
	
	@Deprecated
	public LocalDateTime[] getLocalDateTime(){
		return ultimateLayer.getLocalDateTimes();
	}

	@Override
	public MetaData getMetaData() {
		if (getLastDecodedImageRegion() != null) {
			return this.getLastDecodedImageRegion().getMetaData();
		}
		return null;
	}

	public ImageRegion getLastDecodedImageRegion(){
		return ultimateLayer.getImageRegion();
	}

}
