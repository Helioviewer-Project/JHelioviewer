package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

public class DummyLayer implements LayerInterface{

	@Override
	public int getTexture() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isVisible() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setVisible(boolean visible) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setImageData(LocalDateTime dateTime, SubImage subImage)
			throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LocalDateTime getTime() {
		// TODO Auto-generated method stub
		return null;
	}

}
