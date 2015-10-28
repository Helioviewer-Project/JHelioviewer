package org.helioviewer.jhv.gui.statusLabels;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.viewmodel.TimeLine;


public class CurrentTimeLabel extends StatusLabel
{
	private static final String EMPTY = " - ";
	
	public CurrentTimeLabel()
	{
		super();
        setBorder(BorderFactory.createEtchedBorder());
		setText(EMPTY);
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
		setText(TimeLine.SINGLETON.getCurrentDateTime().format(Globals.DATE_TIME_FORMATTER));
	}
}
