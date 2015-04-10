package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.time.LocalDateTime;

public interface JHVCachable {

	public void setFramesDateTime(LocalDateTime[] framesDateTime);
	public LocalDateTime[] getFramesDateTime();
}
