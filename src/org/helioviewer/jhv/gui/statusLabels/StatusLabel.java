package org.helioviewer.jhv.gui.statusLabels;

import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

import javax.annotation.Nullable;
import javax.swing.JLabel;

import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.RayTrace.Ray;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.TimeLineListener;

abstract class StatusLabel extends JLabel implements TimeLineListener, PanelMouseListener, CameraListener, LayerListener
{
	public StatusLabel()
	{
		TimeLine.SINGLETON.addListener(this);
		Layers.addLayerListener(this);
	}
	
	@Override
	public void mouseExited()
	{
	}

	@Override
	public void mouseMoved(MouseEvent e, Ray ray)
	{
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
	}
	
	@Override
	public void isPlayingChanged(boolean _isPlaying)
	{
	}

	@Override
	public void timeRangeChanged(LocalDateTime _start, LocalDateTime _end)
	{
	}
	
	public void cameraChanged()
	{
	}

	@Override
	public void layerAdded()
	{
	}

	@Override
	public void layersRemoved()
	{
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
	}	
}
