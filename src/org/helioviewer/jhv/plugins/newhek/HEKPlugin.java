package org.helioviewer.jhv.plugins.newhek;

import java.time.LocalDateTime;

import org.helioviewer.jhv.plugins.plugin.AbstractPlugin;
import org.json.JSONObject;

public class HEKPlugin extends AbstractPlugin{

	private static final String PLUGIN_NAME = "HEK";
	
	public HEKPlugin() {
		super(PLUGIN_NAME);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getAboutLicenseText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadStateFile(JSONObject jsonObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeStateFile(JSONObject jsonObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVisible(boolean visible) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isVisible() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void load() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean checkBadRequests(LocalDateTime firstDate,
			LocalDateTime lastDate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getBadRequestCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void retryBadReqeuest() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void dateTimesChanged(int framecount) {
		
	}

}
