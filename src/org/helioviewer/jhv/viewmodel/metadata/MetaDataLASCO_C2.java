package org.helioviewer.jhv.viewmodel.metadata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.viewmodel.view.cache.HelioviewerDateTimeCache;
import org.helioviewer.jhv.viewmodel.view.fitsview.FITSImage;

public class MetaDataLASCO_C2 extends MetaData{

	public MetaDataLASCO_C2(MetaDataContainer metaDataContainer) {
        super(metaDataContainer);
        if (!(instrument.equalsIgnoreCase("LASCO") && detector.equalsIgnoreCase("C2"))){
        	throw new NonSuitableMetaDataException("invalid instrument: "+instrument+"/"+detector);
        }
        hasCorona = true;

        String measurement1 = metaDataContainer.get("FILTER");
        String measurement2 = metaDataContainer.get("POLAR");
        measurement = measurement1 + " " + measurement2;

        observatory = metaDataContainer.get("TELESCOP");
        fullName = "LASCO " + detector;
        this.metaDataContainer = metaDataContainer;


        String observedDate = metaDataContainer.get("DATE_OBS");
        observedDate += "T" + metaDataContainer.get("TIME_OBS");
        time = HelioviewerDateTimeCache.parseDateTime(observedDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");
        localDateTime = LocalDateTime.parse(observedDate, formatter);

        updatePixelParameters();

        setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
        setPhysicalImageSize(new Vector2d(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));

        double arcsecPerPixelX = metaDataContainer.tryGetDouble("CDELT1");
        double arcsecPerPixelY = metaDataContainer.tryGetDouble("CDELT2");
        if (Double.isNaN(arcsecPerPixelX)) {
            if (Double.isNaN(arcsecPerPixelY)) {
                System.out.println(">> HelioviewerMetaData.readPixelParamters() > Both CDELT1 and CDELT2 are NaN. Use 0.6 as default value.");
                arcsecPerPixelX = 0.6;
            } else {
                System.out.println(">> HelioviewerMetaData.readPixelParamters() > CDELT1 is NaN. CDELT2 is used.");
                arcsecPerPixelX = arcsecPerPixelY;
            }
        }
        if (Math.abs(arcsecPerPixelX - arcsecPerPixelY) > arcsecPerPixelX * 0.0001) {
            System.out.println(">> HelioviewerMetaData.readPixelParamters() > CDELT1 and CDELT2 have different values. CDELT1 is used.");
        }
        // distance to sun in meters
        double distanceToSun = metaDataContainer.tryGetDouble("DSUN_OBS");
        double radiusSunInArcsec = Math.atan(Constants.SUN_RADIUS / distanceToSun) * MathUtils.RAD_TO_DEG * 3600;

        solarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;
        meterPerPixel = Constants.SUN_RADIUS / solarPixelRadius;
        setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
        setPhysicalImageSize(new Vector2d(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));

        
        
        innerRadius = metaDataContainer.tryGetDouble("HV_ROCC_INNER") * Constants.SUN_RADIUS;
        outerRadius = metaDataContainer.tryGetDouble("HV_ROCC_OUTER") * Constants.SUN_RADIUS;

        if (innerRadius == 0.0 && detector != null) {
        	innerRadius = 2.3 * Constants.SUN_RADIUS;
            outerRadius = 8.0 * Constants.SUN_RADIUS;
        }
        
        flatDistance = 6.2 * Constants.SUN_RADIUS;
        maskRotation = Math.toRadians(metaDataContainer.tryGetDouble("CROTA"));

        double centerX = 0, centerY = 0;
        if (metaDataContainer instanceof FITSImage && centerX == 0 && centerY == 0) {
            centerX = metaDataContainer.tryGetDouble("CRVAL1");
            centerY = metaDataContainer.tryGetDouble("CRVAL2");
        }       
        
        //Convert arcsec to meters
        double cdelt1 = metaDataContainer.tryGetDouble("CDELT1");
        double cdelt2 = metaDataContainer.tryGetDouble("CDELT2");
        if( cdelt1 != 0 && cdelt2 != 0) {
            centerX = centerX / cdelt1;
            centerY = centerY / cdelt2;
        }
        
        occulterCenter = new Vector2d(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());

}

	@Override
	boolean updatePixelParameters() {
		boolean changed = false;

        if (pixelImageSize.getX() != metaDataContainer.getPixelWidth() || pixelImageSize.getY() != metaDataContainer.getPixelHeight()) {
            pixelImageSize = new Vector2i(metaDataContainer.getPixelWidth(), metaDataContainer.getPixelHeight());
            changed = true;
        }

        double newSolarPixelRadius = -1.0;
        double allowedRelativeDifference = 0.01;

        newSolarPixelRadius = metaDataContainer.tryGetDouble("RSUN");
        allowedRelativeDifference = 0.05;

        if (newSolarPixelRadius == 0) {
            if (detector.equals("C2")) {
                newSolarPixelRadius = 80.814221;
            } else if (detector.equals("C3")) {
                newSolarPixelRadius = 17.173021;
            }

        }

        if (newSolarPixelRadius > 0) {
            double allowedAbsoluteDifference = newSolarPixelRadius * allowedRelativeDifference;
            if (Math.abs(solarPixelRadius - newSolarPixelRadius) > allowedAbsoluteDifference) {
                changed = true;
            }

            double sunX = metaDataContainer.tryGetDouble("CRPIX1");
            double sunY = metaDataContainer.tryGetDouble("CRPIX2");

            if (changed || Math.abs(sunPixelPosition.x - sunX) > allowedAbsoluteDifference || Math.abs(sunPixelPosition.y - sunY) > allowedAbsoluteDifference) {
                sunPixelPosition = new Vector2d(sunX, sunY);
                changed = true;
            }
        }

        if (changed) {
            solarPixelRadius = newSolarPixelRadius;
            meterPerPixel = Constants.SUN_RADIUS / solarPixelRadius;
            setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
            setPhysicalImageSize(new Vector2d(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));
        }

        return changed;
	}
	
	

}
