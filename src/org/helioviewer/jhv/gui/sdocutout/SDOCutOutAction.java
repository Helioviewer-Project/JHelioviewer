package org.helioviewer.jhv.gui.sdocutout;

import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataAIA;

class SDOCutOutAction extends AbstractAction
{
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String URL = "http://www.lmsal.com/get_aia_data/?";
    
    public SDOCutOutAction()
    {
        super("SDO Cut-Out");
    }
    
    public void actionPerformed(@Nullable ActionEvent e)
    {
		ImageLayer activeLayer = Layers.getActiveImageLayer();
		ArrayList<KakaduLayer> sdoLayers = new ArrayList<>();
		
		for (Layer layer : Layers.getLayers())
			if(layer instanceof KakaduLayer)
				if(((KakaduLayer)layer).getMetaData(TimeLine.SINGLETON.getCurrentDateTime()) instanceof MetaDataAIA)
					sdoLayers.add((KakaduLayer)layer);
		
		if(sdoLayers.isEmpty())
			return;
		
		KakaduLayer mainSDOLayer;
		if(activeLayer instanceof KakaduLayer && activeLayer.getMetaData(TimeLine.SINGLETON.getCurrentDateTime()) instanceof MetaDataAIA)
			mainSDOLayer=(KakaduLayer)activeLayer;
		else
			mainSDOLayer=sdoLayers.get(0);
		
		MetaData metaData = mainSDOLayer.getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
		LocalDateTime start = TimeLine.SINGLETON.getFirstDateTime();
		LocalDateTime end = TimeLine.SINGLETON.getLastDateTime();
		
		if(metaData==null || start==null || end==null)
			return;
		
		ImageRegion ir=mainSDOLayer.calculateRegion(MainFrame.SINGLETON.MAIN_PANEL, metaData, MainFrame.SINGLETON.MAIN_PANEL.getCanavasSize());
		if(ir==null)
			return;
		
		Rectangle2D sourceRegion = ir.areaOfSourceImage;
		
		StringBuilder url = new StringBuilder(URL);
		url.append("startDate=").append(start.format(DATE_FORMATTER)).append("&startTime=").append(start.format(TIME_FORMATTER));
		url.append("&stopDate=").append(end.format(DATE_FORMATTER)).append("&stopTime=").append(end.format(TIME_FORMATTER));

		url.append("&wavelengths=");
		for (ImageLayer sdoLayer : sdoLayers)
		{
			MetaData md=sdoLayer.getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
			if(md!=null)
				url.append(",").append(md.measurement);
		}
		
		Vector2i resolution = metaData.resolution;
		Vector2d arcsecFactor = metaData.arcsecPerPixel;
		Vector2d sunPosArcSec = metaData.sunPixelPosition.scaled(arcsecFactor);
		Vector2d sizeArcSec = new Vector2d(sourceRegion.getWidth() * resolution.x, sourceRegion.getHeight() * resolution.y).scaled(arcsecFactor);
		Vector2d offsetArcSec = new Vector2d(sourceRegion.getX() * resolution.x, sourceRegion.getY() * resolution.y).scaled(arcsecFactor);
		Vector2d centerOffsetArcSec = sunPosArcSec.subtract(sizeArcSec.add(offsetArcSec).scaled(0.5));
		url.append("&width=").append(sizeArcSec.x).append("&height=").append(sizeArcSec.y);
		url.append("&xCen=").append(centerOffsetArcSec.x).append("&yCen=").append(centerOffsetArcSec.y);
		
		url.append("&cadence=").append(mainSDOLayer.getCadence()).append("&cadenceUnits=s");
		Globals.openURL(url.toString());
    }
}
