
package org.helioviewer.gl3d.plugin.pfss.data.managers;

import org.helioviewer.gl3d.plugin.pfss.olddata.PfssFitsFile;
import org.helioviewer.gl3d.plugin.pfss.olddata.dataStructure.PfssDayAndTime;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * Runnable class to load the Pfss-data in a thread
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssDataLoader implements Runnable {
	private PfssDayAndTime dayAndTime;
	private PfssFitsFile fitsFile;

	public PfssDataLoader(PfssDayAndTime dayAndTime, PfssFitsFile fitsFile) {
		this.dayAndTime = dayAndTime;
		this.fitsFile = fitsFile;

	}

	@Override
	public void run() {
		String m = (dayAndTime.getMonth()) < 9 ? "0"
				+ (dayAndTime.getMonth() + 1) : (dayAndTime.getMonth() + 1)
				+ "";
		String url = PfssSettings.SERVER_URL + dayAndTime.getYear() + "/" + m
				+ "/" + dayAndTime.getUrl();
		fitsFile.loadFile(url);

	}

}
