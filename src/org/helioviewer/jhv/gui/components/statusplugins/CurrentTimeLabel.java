package org.helioviewer.jhv.gui.components.statusplugins;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;


public class CurrentTimeLabel extends JLabel implements TimeLineListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9216168376764918306L;
	
	private String empty = " - ";
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
	
	public CurrentTimeLabel() {
        this.setBorder(BorderFactory.createEtchedBorder());

		this.setText(empty);
		TimeLine.SINGLETON.addListener(this);
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		this.setText(current.format(JHVGlobals.DATE_TIME_FORMATTER));
	}

	@Override
	public void dateTimesChanged(int framecount) {
		// TODO Auto-generated method stub
		
	}
}
