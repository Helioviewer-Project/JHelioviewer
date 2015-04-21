package org.helioviewer.jhv.layers;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

public interface LayerInterface {
	public int getTexture(Camera camera);
	public boolean isVisible();
	public void setVisible(boolean visible);
	public void setImageData(LocalDateTime dateTime) throws InterruptedException, ExecutionException;
	public String getName();
	public LocalDateTime getTime();
	public LocalDateTime[] getLocalDateTime();
	public MetaData getMetaData();
	public void setImageRegion(Rectangle2D rectangle);
	public ImageRegion getImageRegion();
}
