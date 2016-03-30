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
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.DecodeQualityLevel;
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
				if(((KakaduLayer)layer).getCurrentMetaData() instanceof MetaDataAIA)
					sdoLayers.add((KakaduLayer)layer);
		
		if(sdoLayers.isEmpty())
			return;
		
		KakaduLayer mainSDOLayer;
		if(activeLayer instanceof KakaduLayer && activeLayer.getCurrentMetaData() instanceof MetaDataAIA)
			mainSDOLayer=(KakaduLayer)activeLayer;
		else
			mainSDOLayer=sdoLayers.get(0);
		
		MetaData metaData = mainSDOLayer.getCurrentMetaData();

		LocalDateTime start = MathUtils.toLDT(TimeLine.SINGLETON.getFirstTimeMS());
		LocalDateTime end = MathUtils.toLDT(TimeLine.SINGLETON.getLastTimeMS());
		
		if(metaData==null || start==null || end==null)
			return;

		
		ImageRegion ir=mainSDOLayer.calculateRegion(MainFrame.SINGLETON.MAIN_PANEL, DecodeQualityLevel.QUALITY, metaData, MainFrame.SINGLETON.MAIN_PANEL.getCanavasSize());
		if(ir==null)
			return;
		
		Rectangle2D sourceRegion = ir.areaOfSourceImage;
		
		StringBuilder url = new StringBuilder(URL);
		url.append("startDate="+start.format(DATE_FORMATTER));
		url.append("&startTime="+start.format(TIME_FORMATTER));
		url.append("&stopDate="+end.format(DATE_FORMATTER));
		url.append("&stopTime="+end.format(TIME_FORMATTER));

		url.append("&wavelengths=");
		for (ImageLayer sdoLayer : sdoLayers)
		{
			MetaData md=sdoLayer.getCurrentMetaData();
			if(md!=null)
				url.append(","+md.measurement);
		}
		
		Vector2i resolution = metaData.resolution;
		Vector2d arcsecFactor = metaData.arcsecPerPixel;
		Vector2d sunPosArcSec = metaData.sunPixelPosition.scaled(arcsecFactor);
		Vector2d sizeArcSec = new Vector2d(sourceRegion.getWidth() * resolution.x, sourceRegion.getHeight() * resolution.y).scaled(arcsecFactor);
		Vector2d offsetArcSec = new Vector2d(sourceRegion.getCenterX() * resolution.x, sourceRegion.getCenterY() * resolution.y).scaled(arcsecFactor);

		url.append("&width="+sizeArcSec.x);
		url.append("&height="+sizeArcSec.y);
		url.append("&xCen="+(offsetArcSec.x-sunPosArcSec.x));
		url.append("&yCen="+(-offsetArcSec.y+sunPosArcSec.y));
		
		url.append("&cadence="+(mainSDOLayer.getCadenceMS()/1000));
		url.append("&cadenceUnits=s");
		Globals.openURL(url.toString());
    }
}
