package org.helioviewer.jhv.gui.statusLabels;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.viewmodel.TimeLine;


public class CurrentTimeLabel extends StatusLabel
{
	public CurrentTimeLabel()
	{
		super();
        setBorder(BorderFactory.createEtchedBorder());
		setText(" - ");
	}

	@Override
	public void timeStampChanged(long current, long last)
	{
		LocalDateTime ldt=MathUtils.toLDT(TimeLine.SINGLETON.getCurrentTimeMS());
		if(ldt==null)
			setText(" - ");
		else
			setText(ldt.format(Globals.DATE_TIME_FORMATTER));
	}
}
