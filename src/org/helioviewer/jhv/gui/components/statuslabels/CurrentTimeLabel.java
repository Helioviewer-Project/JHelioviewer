package org.helioviewer.jhv.gui.components.statuslabels;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;


public class CurrentTimeLabel extends StatusLabel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9216168376764918306L;
	
	private static final String EMPTY = " - ";
	
	public CurrentTimeLabel() {
		super();
        this.setBorder(BorderFactory.createEtchedBorder());
		this.setText(EMPTY);
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		this.setText(current.format(JHVGlobals.DATE_TIME_FORMATTER));
	}
}
