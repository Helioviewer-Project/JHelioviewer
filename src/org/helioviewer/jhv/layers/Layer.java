package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.DefaultTable;
import org.helioviewer.jhv.layers.filter.LUT;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

@Deprecated
public class Layer extends LayerInterface{
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
		
		@Override
		public String toString() {
			return name;
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
	private JHVJPXView jhvjpxView;
	
	// filter
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
	public boolean visible = true;
	
	private OpenGLHelper openGLHelper;
	
	public Layer(JHVJPXView jhvjpxView) {
		openGLHelper = new OpenGLHelper();
		this.jhvjpxView = jhvjpxView;
        MetaData metaData = jhvjpxView.getMetaData();
    	String colorKey = DefaultTable.getSingletonInstance().getColorTable(metaData);
        if(colorKey == null)
        	colorKey = "Gray";
        
		lut = new Lut();
		lut.name = colorKey;
		lut.idx = LUT.getLutPosition(colorKey);
		redChannel = new Channelcolor("red");
		greenChannel = new Channelcolor("green");
		blueChannel = new Channelcolor("blue");
		this.initLayer(OpenGLHelper.glContext.getGL().getGL2());
		//this.textureOverview = OpenGLHelper.createTexture(gl);
	}

	private void initLayer(GL2 gl){
		this.texture = openGLHelper.createTextureID();
		openGLHelper.bindLayerToGLTexture(this);
	}
	
	public JHVJPXView getJhvjpxView() {
		return jhvjpxView;
	}
	
	public boolean isVisible(){
		return visible;
	}

	public int getTexture(){
		return texture;
	}
	
	public void updateTexture(GL2 gl){
		openGLHelper.bindLayerToGLTexture(this);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LocalDateTime getTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LocalDateTime[] getLocalDateTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MetaData getMetaData() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public double getContrast() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setContrast(double contrast) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getGamma() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setGamma(double gamma) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getOpacity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setOpacity(double opacity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getSharpen() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSharpen(double sharpen) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public org.helioviewer.jhv.layers.LayerInterface.Lut getLut() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ColorChannel getColorChannel(COLOR_CHANNEL_TYPE type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTexture(CompenentView compenentView) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getID() {
		// TODO Auto-generated method stub
		return 0;
	}
}
