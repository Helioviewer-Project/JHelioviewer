package org.helioviewer.jhv.layers;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewRender;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.UltimateLayer;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class NewLayer extends LayerInterface{
	
	
	public UltimateLayer ultimateLayer;
	private NewRender newRender;
	private NewCache newCache;
	
	public int textureOverview;

	private MetaData metaData;
		
	private LayerRayTrace layerRayTrace;
	
	public NewLayer(int sourceID, NewRender newRender, NewCache newCache) {
		super();
		this.ultimateLayer = new UltimateLayer(id, sourceID, newCache, newRender, this);
		metaData = this.ultimateLayer.getMetaData(0);
		this.initGL();
		layerRayTrace = new LayerRayTrace(this);
	}
	
	public NewLayer(int sourceID, NewRender newRender, NewCache newCache, LocalDateTime start, LocalDateTime end, int cadence) {
		super();
		this.ultimateLayer = new UltimateLayer(id, sourceID, newCache, newRender, this);
		this.ultimateLayer.setTimeRange(start, end, cadence);
		this.initGL();
		metaData = this.ultimateLayer.getMetaData(0);
		layerRayTrace = new LayerRayTrace(this);
	}
	
	public NewLayer(String uri, NewRender newRender) {
		super();
		this.ultimateLayer = new UltimateLayer(id, uri, newRender, this);
		metaData = this.ultimateLayer.getMetaData(0);
		this.initGL();
		layerRayTrace = new LayerRayTrace(this);
	}
	
	@Override
	public int getTexture(CompenentView compenentView) {
		updateTexture(TimeLine.SINGLETON.getCurrentDateTime(), compenentView);
		return this.getLastDecodedImageRegion().getTextureID();
	}
	
	private void updateTexture(LocalDateTime currentDateTime, CompenentView compenentView){
		try {
			ByteBuffer byteBuffer = ultimateLayer.getImageData(currentDateTime, layerRayTrace.getCurrentRegion(compenentView));
			if (byteBuffer != null){
				OpenGLHelper.bindByteBufferToGLTexture(this.getLastDecodedImageRegion(), byteBuffer, this.getLastDecodedImageRegion().getImageSize());
			}
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initGL(){
		//MetaData metaData = null;
		
    	//String colorKey = DefaultTable.getSingletonInstance().getColorTable(metaData);
        String colorKey = null;
		if(colorKey == null)
        	colorKey = "Gray";
        
		lut = new Lut();
		lut.name = metaData.getDefaultLUT().name();
		lut.idx = metaData.getDefaultLUT().ordinal();
	}

	@Override
	public String getName() {
		if (metaData == null) return "";
		return metaData.getFullName();
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
		return metaData;
	}

	public ImageRegion getLastDecodedImageRegion(){
		return ultimateLayer.getImageRegion();
	}

	public void setTimeRange(LocalDateTime start, LocalDateTime end,
			int cadence) {
		ultimateLayer.setTimeRange(start, end, cadence);
	}
	
}
