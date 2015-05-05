package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public abstract class LayerInterface {
		
	public enum SHADER_STATE{
		FALSE, TRUE;
	}
	
	public enum COLOR_CHANNEL_TYPE{
		RED, GREEN, BLUE;
	}
	
	// Channelfilter
	public class ColorChannel{
		private SHADER_STATE status = SHADER_STATE.TRUE;
		private String name;
		
		public ColorChannel(String name) {
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
	
	private double opacity = 1;
	private double sharpen = 0;
	private double gamma = 1;
	private double contrast = 0;;
	protected Lut lut = new Lut();
	private ColorChannel redChannel = new ColorChannel("red");
	private ColorChannel greenChannel = new ColorChannel("green");
	private ColorChannel blueChannel = new ColorChannel("blue");
	private int texture = -1;
	public int textureOverview;
	private boolean visible = true;

	private static int idCounter;
	protected int id;
	
	public LayerInterface() {
		this.id = idCounter++;
	}
	
	public abstract int getTexture(CompenentView compenentView);
	public abstract String getName();
	public abstract LocalDateTime getTime();
	public abstract LocalDateTime[] getLocalDateTime();
	public abstract MetaData getMetaData();
	
	public boolean isVisible(){
		return visible;
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
		GuiState3DWCS.mainComponentView.getComponent().repaint();
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

	public Lut getLut() {
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
		// TODO Auto-generated method stub
		return id;
	}

}
