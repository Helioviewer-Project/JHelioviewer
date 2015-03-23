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
	
	private int size;
	
	private Kdu_cache cache;
	
	private int insturmentID;
	
	private LocalDateTime[] framesDateTime;
	
	public ImageLayer(int insturmentID){
		size = 0;
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
	
	public void addSize(int size){
		this.size += size;
	}
	
	public int getSize(){
		return this.size;
	}

	@Override
	public void setFramesDateTime(LocalDateTime[] framesDateTime) {
		this.framesDateTime = framesDateTime;
	}

	@Override
	public LocalDateTime[] getFramesDateTime() {
		return this.framesDateTime;
	}	
}
