package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.LUT;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;

public class Layer {
	// Channelfilter
	public class Channelcolor{
		public boolean status = true;
		private String name;
		
		public Channelcolor(String name) {
			this.name = name;
		}
		
		public boolean isActivated(){
			return this.status;
		}
	}
	
	private class Lut{
		public boolean inverted = false;
		public int idx = 0;
		public String name;
		
		public boolean isInverted(){
			return this.inverted;
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
	public int texture = -1;
	public boolean visible = true;
	
	public Layer(JHVJPXView jhvjpxView) {
		this.jhvjpxView = jhvjpxView;
		lut = new Lut();
		redChannel = new Channelcolor("red");
		greenChannel = new Channelcolor("green");
		redChannel = new Channelcolor("blue");
	}

	public JHVJPXView getJhvjpxView() {
		return jhvjpxView;
	}
	
	public boolean isVisible(){
		return visible;
	}
}
