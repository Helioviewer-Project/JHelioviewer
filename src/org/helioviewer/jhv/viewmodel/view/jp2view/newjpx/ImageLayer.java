package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.time.LocalDateTime;

import javax.sound.midi.Instrument;

import kdu_jni.Kdu_cache;

public class ImageLayer implements JHVCachable {
	
	private String observatory;
	private String instrument;
	private String measurement1;
	private String measurement2;
	private LocalDateTime dateTime;
	
	private Kdu_cache cache;
	
	private int insturmentID;
	
	public ImageLayer(int insturmentID){
		this.insturmentID = insturmentID;
		this.cache = new Kdu_cache();
	}
	
	public ImageLayer(String observatory, String instrument, String measurement1, String measurement2, LocalDateTime dateTime) {
		this.observatory = observatory;
		this.instrument = instrument;
		this.measurement1 = measurement1;
		this.measurement2 = measurement2;
		this.dateTime = dateTime;
	}
	
	public Kdu_cache getCache(){
		return cache;
	}
	
	
}
