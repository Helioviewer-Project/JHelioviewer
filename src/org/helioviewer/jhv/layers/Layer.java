package org.helioviewer.jhv.layers;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.DefaultTable;
import org.helioviewer.jhv.layers.filter.LUT;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;

public class Layer {
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
	
	public Layer(JHVJPXView jhvjpxView) {
		this.jhvjpxView = jhvjpxView;
        MetaData metaData = jhvjpxView.getMetaData();
    	String colorKey = DefaultTable.getSingletonInstance().getColorTable(metaData);
    	System.out.println("colorKey : " + colorKey);
		lut = new Lut();
		lut.name = colorKey;
		lut.idx = LUT.getLutPosition(colorKey);
		redChannel = new Channelcolor("red");
		greenChannel = new Channelcolor("green");
		blueChannel = new Channelcolor("blue");
		OpenGLHelper.glContext.makeCurrent();
		this.initLayer(OpenGLHelper.glContext.getGL().getGL2());
		//this.textureOverview = OpenGLHelper.createTexture(gl);
	}

	private void initLayer(GL2 gl){
		this.texture = OpenGLHelper.createTexture(gl);
		OpenGLHelper.createTexture(gl, this);		
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
		OpenGLHelper.updateTexture(gl, this);
	}
}
