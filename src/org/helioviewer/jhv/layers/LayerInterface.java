package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

public interface LayerInterface {
	public int getTexture();
	public boolean isVisible();
	public void setVisible(boolean visible);
	public void setImageData(LocalDateTime dateTime, SubImage subImage) throws InterruptedException, ExecutionException;
}
