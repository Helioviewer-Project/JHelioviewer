package org.helioviewer.plugins.sdocutoutplugin;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JToggleButton;

import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.plugins.sdocutoutplugin.controller.SDOCutOutController;
import org.helioviewer.plugins.sdocutoutplugin.settings.SDOCutOutAPI;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

public class SDOCutOutAction extends AbstractAction {
	
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SDOCutOutAction() {
        super("SDO Cut-Outs");
    }
    
    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
    	// LayersModel, helioMetaData
		LayersModel layersModel = LayersModel.getSingletonInstance();
		 
		// Active layer index
		int idx_active = layersModel.getActiveLayer();
    	
		// RegionView, ViewportView and MetaDataView
		if (idx_active > -1){
			RegionView regionView = layersModel.getLayer(idx_active).getAdapter(RegionView.class);
			ViewportView viewportView = layersModel.getLayer(idx_active).getAdapter(ViewportView.class);
			MetaDataView metaDataView= layersModel.getLayer(idx_active).getAdapter(MetaDataView.class);
			
			// get viewport image size
	        ViewportImageSize viewportImageSize = ViewHelper.calculateViewportImageSize(viewportView.getViewport(), regionView.getRegion());
	        
	        // Upper-left, Upper-right and Lower-left points of the region of interest (in arcsec)
	        Point pul = SDOCutOutController.getSingletonInstance().getPositioninArcsec(new Point(0,0));
	        Point pur = SDOCutOutController.getSingletonInstance().getPositioninArcsec(new Point(viewportImageSize.getWidth()-1,0));
	        Point pll = SDOCutOutController.getSingletonInstance().getPositioninArcsec(new Point(0, viewportImageSize.getHeight()-1));
	        
			long xCen = Math.round((pul.getX() + pur.getX())/2);
			long yCen = Math.round((pul.getY() + pll.getY())/2);
						
			// Check if xCen, yCen are in the sun region
			if (SDOCutOutController.getSingletonInstance().getDistancePoints(new Point((int)xCen, (int)yCen), new Point(0,0)) > metaDataView.getMetaData().getRadiusSuninArcsec()) 
			{
				Message.err("Wrong region of interest", "xCen and yCen are not in the sun region", false);
				return;
			}
			
			// Base url
			String url=SDOCutOutAPI.API_URL;// Computation of xCen, yCen, Height and Width
					
			Pattern p = Pattern.compile("\\{#(\\w+)(?:,(.+?))?\\}");
			Matcher m = p.matcher(url);
			
			String newUrl="";
			int start=0;
			
			while (m.find())
			{
				newUrl += url.substring(start, m.start());
				// Start and end date
				if (m.group(1).equals("StartDate") || m.group(1).equals("StopDate"))
				{
					String format = m.group(2);
					if (format == null)
						format= SDOCutOutAPI.API_DATE_FORMAT;
					
					newUrl += (m.group(1).equals("StartDate") ? SDOCutOutAPI.API_START_DATE+SDOCutOutController.getSingletonInstance().getDate(layersModel.getStartDate(idx_active).getTime(),format) 
							: SDOCutOutAPI.API_STOP_DATE+SDOCutOutController.getSingletonInstance().getDate(layersModel.getEndDate(idx_active).getTime(),format));
				}
				// Start and end time
				if (m.group(1).equals("StartTime") || m.group(1).equals("StopTime"))
				{
					String format = m.group(2);
					if (format == null)
						format= SDOCutOutAPI.API_TIME_FORMAT;
					
					newUrl += (m.group(1).equals("StartTime") ? SDOCutOutAPI.API_START_TIME+SDOCutOutController.getSingletonInstance().getTime(layersModel.getStartDate(idx_active).getTime(),format) 
							: SDOCutOutAPI.API_STOP_TIME+SDOCutOutController.getSingletonInstance().getTime(layersModel.getEndDate(idx_active).getTime(),format));
				}
				// Wavelengths
				else if (m.group(1).equals("Wavelengths"))
				{
					String wavelengths=SDOCutOutAPI.API_WAVELENGTHS;
					for (int i=0; i< layersModel.getNumLayers(); i++)
					{
					  MetaData helioMetaData = layersModel.getLayer(i).getAdapter(MetaDataView.class).getMetaData();
						
						if (helioMetaData.getObservatory().contains("SDO"))
			    		{
			    			wavelengths+=helioMetaData.getMeasurement()+",";
			    		}
					}
					wavelengths = wavelengths.substring(0, wavelengths.length()-1);
					
					newUrl += wavelengths;
				}
				// Width
				else if (m.group(1).equals("Width"))
				{
					newUrl += SDOCutOutAPI.API_WIDTH + Math.round(Math.abs(pul.getX() - pur.getX()));
				}
				// Height
				else if (m.group(1).equals("Height"))
				{
					newUrl += SDOCutOutAPI.API_HEIGHT + Math.round(Math.abs(pul.getY() - pll.getY()));
				}
				// xCen
				else if (m.group(1).equals("XCen"))
				{
					newUrl += SDOCutOutAPI.API_XCEN + xCen;
				}
				// yCen
				else if (m.group(1).equals("YCen"))
				{
					newUrl += SDOCutOutAPI.API_YCEN + yCen;
				}
				// Cadence
				else if (m.group(1).equals("Cadence"))
				{
					StringBuilder cadenceUnits = new StringBuilder();
					long cadence = SDOCutOutController.getSingletonInstance().getCadence(cadenceUnits);
					if (cadence != 0)
					{
						newUrl += SDOCutOutAPI.API_CADENCE + cadence + "&" + SDOCutOutAPI.API_CADENCEUNITS + cadenceUnits.toString();
					}
				}
				start = m.end();
			}	
			
			newUrl += url.substring(start);
					
			// Tracking
			/*
			Component[] components=ImageViewerGui.getSingletonInstance().getTopToolBar().getComponents();
			for (int i=0; i<components.length; i++)
			{
				if (components[i] instanceof JToggleButton && ((JToggleButton)components[i]).getText().equals("Track") && ((JToggleButton)components[i]).isSelected())
				{
					Log.info("Track is activated");
					break;
				}
			}*/
			
			JHVGlobals.openURL(newUrl);
		}
		((JToggleButton)e.getSource()).setSelected(false);
    }
}
