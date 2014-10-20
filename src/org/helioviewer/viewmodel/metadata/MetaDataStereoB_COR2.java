package org.helioviewer.viewmodel.metadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.cache.HelioviewerDateTimeCache;
import org.helioviewer.viewmodel.view.fitsview.FITSImage;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

public class MetaDataStereoB_COR2 extends MetaData{

	public enum Type{
		STEREO_A, STEREO_A_COR_1, STEREO_A_COR_2, STEREO_B, STEREO_B_COR_1, STEREO_B_COR_2 
	}

	public MetaDataStereoB_COR2(MetaDataContainer metaDataContainer) {
        super(metaDataContainer);
        observatory = metaDataContainer.get("OBSRVTRY");
        measurement = metaDataContainer.get("WAVELNTH");
        if (measurement == null) {
            measurement = "" + metaDataContainer.tryGetDouble("WAVELNTH");
        }
        fullName = instrument + " " + detector;
        
        if (!(observatory.equalsIgnoreCase("STEREO_B") && detector.equalsIgnoreCase("COR2"))){
        	throw new MetaDataException("invalid instrument");
        }
        this.metaDataContainer = metaDataContainer;

        String observedDate = metaDataContainer.get("DATE_OBS");
        time = HelioviewerDateTimeCache.parseDateTime(observedDate);

        updatePixelParameters();

        setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
        setPhysicalImageSize(new Vector2dDouble(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));

        double arcsecPerPixelX = metaDataContainer.tryGetDouble("CDELT1");
        double arcsecPerPixelY = metaDataContainer.tryGetDouble("CDELT2");
        if (Double.isNaN(arcsecPerPixelX)) {
            if (Double.isNaN(arcsecPerPixelY)) {
                Log.warn(">> HelioviewerMetaData.readPixelParamters() > Both CDELT1 and CDELT2 are NaN. Use 0.6 as default value.");
                arcsecPerPixelX = 0.6;
            } else {
                Log.warn(">> HelioviewerMetaData.readPixelParamters() > CDELT1 is NaN. CDELT2 is used.");
                arcsecPerPixelX = arcsecPerPixelY;
            }
        }
        if (Math.abs(arcsecPerPixelX - arcsecPerPixelY) > arcsecPerPixelX * 0.0001) {
            Log.warn(">> HelioviewerMetaData.readPixelParamters() > CDELT1 and CDELT2 have different values. CDELT1 is used.");
        }
        // distance to sun in meters
        double distanceToSun = metaDataContainer.tryGetDouble("DSUN_OBS");
        double radiusSunInArcsec = Math.atan(Constants.SunRadius / distanceToSun) * MathUtils.radeg * 3600;

        solarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;
        meterPerPixel = Constants.SunRadius / solarPixelRadius;
        setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
        setPhysicalImageSize(new Vector2dDouble(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));

        
        innerRadius = metaDataContainer.tryGetDouble("HV_ROCC_INNER") * Constants.SunRadius;
        outerRadius = metaDataContainer.tryGetDouble("HV_ROCC_OUTER") * Constants.SunRadius;
        
        if (innerRadius == 0.0 && detector != null) {
            innerRadius = 3.25 * Constants.SunRadius;
            outerRadius = 17 * Constants.SunRadius;
        }

        flatDistance = 18 * Constants.SunRadius;

        maskRotation = Math.toRadians(metaDataContainer.tryGetDouble("CROTA"));

        double centerX = 0, centerY = 0;
        if (detector != null &&  metaDataContainer instanceof JP2Image) {
            JP2Image jp2image = (JP2Image) metaDataContainer;
            try {
                String crval1Original = jp2image.getValueFromXML("HV_CRVAL1_ORIGINAL", "helioviewer");
                String crval2Original = jp2image.getValueFromXML("HV_CRVAL2_ORIGINAL", "helioviewer");
                if (crval1Original != null && crval2Original != null) {
                    centerX = Double.parseDouble(crval1Original);
                    centerY = Double.parseDouble(crval2Original);
                } else {
                    String crvalComment = jp2image.getValueFromXML("HV_SECCHI_COMMENT_CRVAL", "helioviewer");
                    if(crvalComment == null) {
                        crvalComment = jp2image.getValueFromXML("HV_COMMENT", "helioviewer");
                    }
                    if(crvalComment != null) {
                        Pattern pattern = Pattern.compile(".*CRVAL1=([+-]?\\d+(.\\d+)?).*");
                        Matcher matcher = pattern.matcher(crvalComment);
                        if(matcher.matches()) {
                            centerX = Double.parseDouble(matcher.group(1));
                        }
                        pattern = Pattern.compile(".*CRVAL2=([+-]?\\d+(.\\d+)?).*");
                        matcher = pattern.matcher(crvalComment);
                        if(matcher.matches()) {
                            centerY = Double.parseDouble(matcher.group(1));
                        }
                    }
                }
            } catch (JHV_KduException e) {
                Log.error(">> HelioviewerOcculterMetaData > Error reading helioviewer meta data key HV_SECCHI_COMMENT_CRVAL", e);
            }
        } else if (metaDataContainer instanceof FITSImage && centerX == 0 && centerY == 0) {
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
        
        // HACK - manual adjustment for occulter center
        centerX += 22;
        centerY -= 37;

        occulterCenter = new Vector2dDouble(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());

    }

	@Override
	boolean updatePixelParameters() {
		boolean changed = false;

        if (pixelImageSize.getX() != metaDataContainer.getPixelWidth() || pixelImageSize.getY() != metaDataContainer.getPixelHeight()) {
            pixelImageSize = new Vector2dInt(metaDataContainer.getPixelWidth(), metaDataContainer.getPixelHeight());
            changed = true;
        }

        double newSolarPixelRadius = -1.0;
        double allowedRelativeDifference = 0.01;

        double solarRadiusArcSec = metaDataContainer.tryGetDouble("RSUN");
        double arcSecPerPixel = metaDataContainer.tryGetDouble("CDELT1");
        double arcSecPerPixel2 = metaDataContainer.tryGetDouble("CDELT2");
        if (arcSecPerPixel != arcSecPerPixel2) {
            Log.warn("HelioviewerMetaData: STEREO Meta Data inconsistent! Resolution not the same in x and y direction! (1: " + arcSecPerPixel + ", 2: " + arcSecPerPixel2 + ")");
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

            if (changed || Math.abs(sunPixelPosition.getX() - sunX) > allowedAbsoluteDifference || Math.abs(sunPixelPosition.getY() - sunY) > allowedAbsoluteDifference) {
                sunPixelPosition = new Vector2dDouble(sunX, sunY);
                changed = true;
            }
        }

        if (changed) {
            solarPixelRadius = newSolarPixelRadius;
            meterPerPixel = Constants.SunRadius / solarPixelRadius;
            setPhysicalLowerLeftCorner(sunPixelPosition.scale(-meterPerPixel));
            setPhysicalImageSize(new Vector2dDouble(pixelImageSize.getX() * meterPerPixel, pixelImageSize.getY() * meterPerPixel));
        }

        return changed;
	}
	
	

}
