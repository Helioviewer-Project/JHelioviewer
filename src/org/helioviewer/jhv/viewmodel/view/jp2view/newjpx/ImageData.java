package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.nio.IntBuffer;

public class ImageData {
	private IntBuffer imageBuffer;
	private int width;
	private int height;
	private int xOffset;
	private int yOffset;
	
	public IntBuffer getImageBuffer() {
		return imageBuffer;
	}
	public void setImageBuffer(IntBuffer imageBuffer) {
		this.imageBuffer = imageBuffer;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public int getxOffset() {
		return xOffset;
	}
	public void setxOffset(int xOffset) {
		this.xOffset = xOffset;
	}
	public int getyOffset() {
		return yOffset;
	}
	public void setyOffset(int yOffset) {
		this.yOffset = yOffset;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("width : " + width + "\t");
		sb.append("height: " + height + "\t");
		sb.append("xOffset:" + xOffset + "\t");
		sb.append("yOffset;" + yOffset);
		return sb.toString();
	}
}
