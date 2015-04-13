package org.helioviewer.jhv.viewmodel.metadata;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.viewmodel.view.cache.HelioviewerDateTimeCache;
import org.helioviewer.jhv.viewmodel.view.fitsview.FITSImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;

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
        time = HelioviewerDateTimeCache.parseDateTime(observedDate);
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
                System.err.println(">> HelioviewerOcculterMetaData > Error reading helioviewer meta data key HV_SECCHI_COMMENT_CRVAL");
                e.printStackTrace();
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
        centerX += 1;
        centerY += 3;

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
