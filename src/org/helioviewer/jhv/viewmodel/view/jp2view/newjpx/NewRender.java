package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import kdu_jni.Jp2_threadsafe_family_src;

public class NewRender {
	
	private Jp2_threadsafe_family_src family_src = new Jp2_threadsafe_family_src();
	
	public NewRender() {
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
		});
		thread.start();
	}
	
	
	private void createKakaduMachinery(){
		
	}
}
