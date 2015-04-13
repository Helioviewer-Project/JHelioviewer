package org.helioviewer.jhv.layers;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.layers.filter.LUT;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewRender;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.UltimateLayer;

public class NewLayer implements LayerInterface{

	public enum SHADER_STATE{
		FALSE, TRUE;
	}
	
	// Channelfilter
	public class Channelcolor{
		private SHADER_STATE status = SHADER_STATE.TRUE;
		private String name;
		
		public Channelcolor(String name) {
			this.name = name;
		}
		
		public boolean isActivated(){
			return this.status == SHADER_STATE.TRUE;
		}
		
		public void setActive(boolean status){
			this.status = status ? SHADER_STATE.TRUE : SHADER_STATE.FALSE;
		}
		
		public int getState(){
			return this.status == SHADER_STATE.TRUE ? 1 : 0;
		}
	}
	
	public class Lut{
		private SHADER_STATE inverted = SHADER_STATE.FALSE;
		public int idx = 0;
		public String name;
		
		public boolean isInverted(){
			return inverted == SHADER_STATE.TRUE;
		}
		
		public void setInverted(boolean inverted){
			this.inverted = inverted ? SHADER_STATE.TRUE : SHADER_STATE.FALSE;
		}
		
		public int getState(){
			return this.inverted == SHADER_STATE.TRUE ? 1 : 0;
		}
	}
	
	public UltimateLayer ultimateLayer;
	private NewRender newRender;
	private NewCache newCache;
	
	public double opacity = 1;
	public double sharpen = 0;
	public double gamma = 1;
	public double contrast = 0;;
	public Lut lut;
	public Channelcolor redChannel;
	public Channelcolor greenChannel;
	public Channelcolor blueChannel;
	private int texture = -1;
	public int textureOverview;
	private boolean visible = true;

	private OpenGLHelper openGLHelper;
	private MetaData metaData;
	
	public NewLayer(int sourceID, NewRender newRender, NewCache newCache) {
		this.ultimateLayer = new UltimateLayer(sourceID, newCache, newRender);
		this.initGL();
	}
	
	public NewLayer(String uri, NewRender newRender) {
		System.out.println(uri);
		this.ultimateLayer = new UltimateLayer(uri, newRender);
		this.initGL();
		metaData = this.ultimateLayer.getMetaData(0);
		
	}
	
	@Override
	public int getTexture() {
		return texture;
	}
	
	private void initGL(){
		MetaData metaData = null;
		
    	//String colorKey = DefaultTable.getSingletonInstance().getColorTable(metaData);
        String colorKey = null;
		if(colorKey == null)
        	colorKey = "Gray";
        
		openGLHelper = new OpenGLHelper();		
		lut = new Lut();
		lut.name = colorKey;
		lut.idx = LUT.getLutPosition(colorKey);
		redChannel = new Channelcolor("red");
		greenChannel = new Channelcolor("green");
		blueChannel = new Channelcolor("blue");
		texture = openGLHelper.createTextureID();
	}

	@Override
	public boolean isVisible() {
		return this.visible && opacity > 0;
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public void setImageData(LocalDateTime dateTime, SubImage subImage) throws InterruptedException, ExecutionException{
		this.ultimateLayer.getImageData(dateTime, subImage);
	}

	@Override
	public String getName() {
		String retValue = "";
		retValue = metaData.getDetector();
		System.out.println(retValue);
		retValue = metaData.getFullName();
		return retValue;
	}

	@Override
	public LocalDateTime getTime() {
		// TODO Auto-generated method stub
		return ultimateLayer.getLocalDateTime(0);
	}

}
