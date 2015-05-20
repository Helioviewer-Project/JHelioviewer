package org.helioviewer.jhv.viewmodel.metadata;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.physics.Constants;

public class MetaDataStereoB_COR1 extends MetaData{

	public enum Type{
		STEREO_A, STEREO_A_COR_1, STEREO_A_COR_2, STEREO_B, STEREO_B_COR_1, STEREO_B_COR_2 
	}

	public MetaDataStereoB_COR1(MetaDataContainer metaDataContainer) {
        super(metaDataContainer);
        observatory = metaDataContainer.get("OBSRVTRY");
        measurement = metaDataContainer.get("WAVELNTH");
        if (measurement == null) {
            measurement = "" + metaDataContainer.tryGetDouble("WAVELNTH");
        }
        fullName = instrument + " " + detector;
        
        if (!(observatory.equalsIgnoreCase("STEREO_B") && detector.equalsIgnoreCase("COR1"))){
        	throw new NonSuitableMetaDataException("invalid instrument: "+observatory+"/"+detector);
        }
        hasCorona = true;
        hasSphere = false;
        this.hasRotation = true;
        this.metaDataContainer = metaDataContainer;

        String observedDate = metaDataContainer.get("DATE_OBS");
        localDateTime = LocalDateTime.parse(observedDate, DATE_FORMAT);

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
            innerRadius = 1.5 * Constants.SUN_RADIUS;
            outerRadius = 4.9 * Constants.SUN_RADIUS;
        }

        flatDistance = 4.95 * Constants.SUN_RADIUS;
        maskRotation = Math.toRadians(metaDataContainer.tryGetDouble("CROTA"));
        
        double centerX = 0, centerY = 0;
        
        //Convert arcsec to meters
        double cdelt1 = metaDataContainer.tryGetDouble("CDELT1");
        double cdelt2 = metaDataContainer.tryGetDouble("CDELT2");
        if( cdelt1 != 0 && cdelt2 != 0) {
            centerX = centerX / cdelt1;
            centerY = centerY / cdelt2;
        }
        
        // HACK - manual adjustment for occulter center
        centerX += 1;
        centerY += 3;

        occulterCenter = new Vector2d(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());


  }

	@Override
	public boolean updatePixelParameters() {
		boolean changed = true;

        double newSolarPixelRadius = -1.0;
        double allowedRelativeDifference = 0.01;

        double solarRadiusArcSec = metaDataContainer.tryGetDouble("RSUN");
        double arcSecPerPixel = metaDataContainer.tryGetDouble("CDELT1");
        double arcSecPerPixel2 = metaDataContainer.tryGetDouble("CDELT2");
        if (arcSecPerPixel != arcSecPerPixel2) {
            System.out.println("HelioviewerMetaData: STEREO Meta Data inconsistent! Resolution not the same in x and y direction! (1: " + arcSecPerPixel + ", 2: " + arcSecPerPixel2 + ")");
        }
        double solarRadiusPixel = solarRadiusArcSec / arcSecPerPixel;
        newSolarPixelRadius = solarRadiusPixel;

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
