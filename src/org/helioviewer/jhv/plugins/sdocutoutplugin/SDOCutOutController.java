package org.helioviewer.jhv.plugins.sdocutoutplugin;

import java.awt.Point;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.helioviewer.jhv.base.math.Vector2dDouble;
import org.helioviewer.jhv.base.math.Vector2dInt;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;
import org.helioviewer.jhv.viewmodel.viewportimagesize.ViewportImageSize;

public class SDOCutOutController {
	
	private static final SDOCutOutController SINGLETON = new SDOCutOutController();
	
	/**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static SDOCutOutController getSingletonInstance() {
        return SINGLETON;
    }

	
	// Date format for the SDO Cut-Out Service
	public String getDate(Date date, String format)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date, new StringBuffer(), new FieldPosition(0)).toString();
	}
	
	// Time format for the SDO Cut-Out Service
	public String getTime(Date time, String format)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(time, new StringBuffer(), new FieldPosition(0)).toString();
	}
	
	// Distance between two points
	public double getDistancePoints(Point p1, Point p2)
	{
		return Math.sqrt(Math.pow(p2.getX()-p1.getX(),2) + Math.pow(p2.getY()-p1.getY(),2)); 
	}
	
	// Transform the position of a point from pixels to arcsec
	public Point getPositioninArcsec(Point position) {
    	// check region and viewport
    	RegionView regionView = LayersModel.getSingletonInstance().getActiveView().getAdapter(RegionView.class);
    	Region region = regionView.getRegion();
		Viewport viewport = LayersModel.getSingletonInstance().getActiveView().getAdapter(ViewportView.class).getViewport();
		MetaData metaData = LayersModel.getSingletonInstance().getActiveView().getAdapter(MetaDataView.class).getMetaData();
		       	
    	Point point;
	    
	    // get viewport image size
	    ViewportImageSize vis = ViewHelper.calculateViewportImageSize(viewport, region);

	    // Helioviewer images have there physical lower left corner in a
	    // negative area; real pixel based image at 0
	    if (metaData.getPhysicalLowerLeft().getX() < 0) {

	    	Vector2dInt solarcenter = ViewHelper.convertImageToScreenDisplacement(regionView.getRegion().getUpperLeftCorner().negateX(), regionView.getRegion(), vis);

	    	Vector2dDouble scaling = new Vector2dDouble(Constants.SUN_RADIUS, Constants.SUN_RADIUS);
	        Vector2dDouble solarRadius = new Vector2dDouble(ViewHelper.convertImageToScreenDisplacement(scaling, regionView.getRegion(), vis));

	        Vector2dDouble pos = new Vector2dDouble(position.x - solarcenter.getX(), -position.y + solarcenter.getY()).invertedScale(solarRadius).scale(959.705);
	        
	        point = new Point((int)Math.round(pos.getX()), (int)Math.round(pos.getY()));
        } else {

        	// computes pixel position for simple images (e.g. jpg and png)
	        // where cursor points at
	        // compute coordinates in image
	        int x = (int) (region.getWidth() * (position.getX() / vis.getWidth()) + region.getCornerX());
	        int y = (int) (metaData.getPhysicalImageHeight() - (region.getCornerY() + region.getHeight()) + position.getY() / (double) vis.getHeight() * region.getHeight() + 0.5);
	            
	        point = new Point(x,y);
	    }
	    return point;
    }
	
	// Get cadence and cadence units of the active layer
	public long getCadence(StringBuilder cadenceUnits)
	{
		JHVJPXView tmv=LayersModel.getSingletonInstance().getActiveView().getAdapter(JHVJPXView.class);
		
		long difference;
		long sum=0;
		
		if (tmv == null || tmv.getMaximumFrameNumber()==1)
			return 0;
		
		for (int i=0; i< tmv.getMaximumFrameNumber()-1; i++)
		{
			difference=tmv.getFrameDateTime(i+1).getMillis()-tmv.getFrameDateTime(i).getMillis();
			sum+=difference;
		}
		
		double time = Math.floor(sum/((tmv.getMaximumFrameNumber()-1)*1000));
		
		if (time % 3600 == 0)
		{
			cadenceUnits.append('h');
			return (long) time/3600;
		}
		if (time % 60 == 0)
		{
			cadenceUnits.append('m');
			return (long) time/60;
		}
		cadenceUnits.append('s');
		return (long) time;
	}
}
