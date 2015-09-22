package org.helioviewer.jhv.base.sdocutout;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

class SDOCutOutAction extends AbstractAction {
	
	private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String URL = "http://www.lmsal.com/get_aia_data/?";
    /**
     * Default constructor.
     */
    public SDOCutOutAction() {
        super("SDO Cut-Out");
    }
    
    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
			AbstractImageLayer layer = Layers.getActiveImageLayer();
			if (layer != null){
			ArrayList<AbstractImageLayer> sdoLayers = new ArrayList<AbstractImageLayer>();
			for (AbstractLayer layerInterface : Layers.getLayers()){
				//FIXME: don't search by comparing string
				if (layerInterface.getName().contains("AIA")){
					sdoLayers.add((AbstractImageLayer)layerInterface);
				}
			}
			AbstractImageLayer mainSDOLayer = layer.getName().contains("AIA") ? layer : sdoLayers.get(0);
			
			Rectangle2D size = mainSDOLayer.getLastDecodedImageRegion().getImageData();
			LocalDateTime start = layer.getLocalDateTime().first();
			LocalDateTime end = layer.getLocalDateTime().last();
			MetaData metaData;
			try {
				metaData = mainSDOLayer.getMetaData(TimeLine.SINGLETON.getCurrentDateTime());

				StringBuilder url = new StringBuilder(URL);
				url.append("startDate="+start.format(DATE_FORMATTER) + "&startTime=" + start.format(TIME_FORMATTER)); 
				url.append("&stopDate=" + end.format(DATE_FORMATTER) + "&stopTime=" + end.format(TIME_FORMATTER));

				url.append("&wavelengths=");
				for (AbstractImageLayer sdoLayer : sdoLayers){
					url.append("," + sdoLayer.getMetaData(TimeLine.SINGLETON.getCurrentDateTime()).getMeasurement());
				}
				
				Rectangle resolution = metaData.getResolution();
				double arcsecFactor = metaData.getArcsecPerPixel();
				Vector2d sunPosArcSec = metaData.getSunPixelPosition().scale(arcsecFactor);
				Vector2d sizeArcSec = new Vector2d(size.getWidth() * resolution.getWidth(), size.getHeight() * resolution.getHeight()).scale(arcsecFactor);
				Vector2d offsetArcSec = new Vector2d(size.getX() * resolution.getWidth(), size.getY() * resolution.getHeight()).scale(arcsecFactor);
				Vector2d centerOffsetArcSec = sunPosArcSec.subtract(sizeArcSec.add(offsetArcSec).scale(0.5));
				url.append("&width=" + sizeArcSec.x + "&height=" + sizeArcSec.y);
				url.append("&xCen=" + centerOffsetArcSec.x + "&yCen=" + centerOffsetArcSec.y);
				
				url.append("&cadence=" + mainSDOLayer.getCadence() + "&cadenceUnits=s");
				JHVGlobals.openURL(url.toString());
			} catch (MetaDataException e1) {
				
				e1.printStackTrace();
			}
			

		}
    }
}
