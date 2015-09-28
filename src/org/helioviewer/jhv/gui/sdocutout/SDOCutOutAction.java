package org.helioviewer.jhv.gui.sdocutout;

import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.Globals;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

class SDOCutOutAction extends AbstractAction
{
	private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String URL = "http://www.lmsal.com/get_aia_data/?";
    
    public SDOCutOutAction()
    {
        super("SDO Cut-Out");
    }
    
    public void actionPerformed(ActionEvent e)
    {
		AbstractImageLayer activeLayer = Layers.getActiveImageLayer();
		ArrayList<KakaduLayer> sdoLayers = new ArrayList<KakaduLayer>();
		for (AbstractLayer layer : Layers.getLayers())
		{
			//FIXME: don't search by comparing strings, also improve logic
			if (layer.getName().contains("AIA") && layer instanceof KakaduLayer)
				sdoLayers.add((KakaduLayer)layer);
		}
		
		KakaduLayer mainSDOLayer = activeLayer.getName().contains("AIA") && (activeLayer instanceof KakaduLayer) ? (KakaduLayer)activeLayer : sdoLayers.get(0);
		
		Rectangle2D size = mainSDOLayer.getLastDecodedImageRegion().getImageData();
		LocalDateTime start = TimeLine.SINGLETON.getFirstDateTime();
		LocalDateTime end = TimeLine.SINGLETON.getLastDateTime();
		MetaData metaData = mainSDOLayer.getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
		
		if(metaData==null || start==null || end==null)
			return;
		
		StringBuilder url = new StringBuilder(URL);
		url.append("startDate="+start.format(DATE_FORMATTER) + "&startTime=" + start.format(TIME_FORMATTER)); 
		url.append("&stopDate=" + end.format(DATE_FORMATTER) + "&stopTime=" + end.format(TIME_FORMATTER));

		url.append("&wavelengths=");
		for (AbstractImageLayer sdoLayer : sdoLayers)
		{
			MetaData md=sdoLayer.getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
			if(md!=null)
				url.append("," + md.getMeasurement());
		}
		
		Vector2i resolution = metaData.getResolution();
		double arcsecFactor = metaData.getArcsecPerPixel();
		Vector2d sunPosArcSec = metaData.getSunPixelPosition().scale(arcsecFactor);
		Vector2d sizeArcSec = new Vector2d(size.getWidth() * resolution.x, size.getHeight() * resolution.y).scale(arcsecFactor);
		Vector2d offsetArcSec = new Vector2d(size.getX() * resolution.x, size.getY() * resolution.y).scale(arcsecFactor);
		Vector2d centerOffsetArcSec = sunPosArcSec.subtract(sizeArcSec.add(offsetArcSec).scale(0.5));
		url.append("&width=" + sizeArcSec.x + "&height=" + sizeArcSec.y);
		url.append("&xCen=" + centerOffsetArcSec.x + "&yCen=" + centerOffsetArcSec.y);
		
		url.append("&cadence=" + mainSDOLayer.getCadence() + "&cadenceUnits=s");
		Globals.openURL(url.toString());
    }
}
