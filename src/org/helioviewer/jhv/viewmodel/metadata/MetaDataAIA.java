package org.helioviewer.jhv.viewmodel.metadata;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;
import org.helioviewer.jhv.viewmodel.view.cache.HelioviewerDateTimeCache;

public class MetaDataAIA extends MetaData{
  public MetaDataAIA(MetaDataContainer metaDataContainer){
        super(metaDataContainer);
        
        measurement = metaDataContainer.get("WAVELNTH");
        observatory = metaDataContainer.get("TELESCOP");
        if (!(instrument.equalsIgnoreCase("AIA_1") || instrument.equalsIgnoreCase("AIA_2") || instrument.equalsIgnoreCase("AIA_3") || instrument.equalsIgnoreCase("AIA_4"))){
        	throw new NonSuitableMetaDataException("invalid instrument: "+instrument);
        }

        this.hasCorona = true;
        this.hasSphere = true;

        this.metaDataContainer = metaDataContainer;

        this.instrument = "AIA";
        fullName = "AIA " + measurement;
        
        System.out.println("measurement : " + measurement);
        switch (measurement) {
		case "131":
			defaultLUT = LUT_ENTRY.SDO_AIA_131;
			break;
		case "1600":
			defaultLUT = LUT_ENTRY.SDO_AIA_1600;
			break;
		case "1700":
			defaultLUT = LUT_ENTRY.SDO_AIA_1700;
			break;
		case "171":
			defaultLUT = LUT_ENTRY.SDO_AIA_171;
			break;
		case "193":
			defaultLUT = LUT_ENTRY.SDO_AIA_193;
			break;
		case "211":
			defaultLUT = LUT_ENTRY.SDO_AIA_211;
			break;
		case "304":
			defaultLUT = LUT_ENTRY.SDO_AIA_304;
			break;
		case "335":
			defaultLUT = LUT_ENTRY.SDO_AIA_335;
			break;
		case "4500":
			defaultLUT = LUT_ENTRY.SDO_AIA_4500;
			break;
		case "94":
			defaultLUT = LUT_ENTRY.SDO_AIA_94;
			break;
		default:
			break;
		}
        
        String observedDate = metaDataContainer.get("DATE_OBS");
        time = HelioviewerDateTimeCache.parseDateTime(observedDate);
        localDateTime = LocalDateTime.parse(observedDate, DATE_FORMAT);

        updatePixelParameters();

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

        
        
        this.heeqX = metaDataContainer.tryGetDouble("HEQX_OBS");
        this.heeqY = metaDataContainer.tryGetDouble("HEQY_OBS");
        this.heeqZ = metaDataContainer.tryGetDouble("HEQZ_OBS");
        this.heeqAvailable = this.heeqX != 0.0 || this.heeqY != 0.0 || this.heeqZ != 0.0;

        this.heeX = metaDataContainer.tryGetDouble("HEEX_OBS");
        this.heeY = metaDataContainer.tryGetDouble("HEEY_OBS");
        this.heeZ = metaDataContainer.tryGetDouble("HEEZ_OBS");
        this.heeAvailable = this.heeX != 0.0 || this.heeY != 0.0 || this.heeZ != 0.0;

        this.crlt = metaDataContainer.tryGetDouble("CRLT_OBS");
        this.crln = metaDataContainer.tryGetDouble("CRLN_OBS");
        this.dobs = metaDataContainer.tryGetDouble("DSUN_OBS");
        this.carringtonAvailable = this.crlt != 0.0 || this.crln != 0.0;

        this.stonyhurstLatitude = metaDataContainer.tryGetDouble("HGLT_OBS");
        this.stonyhurstLongitude = metaDataContainer.tryGetDouble("HGLN_OBS");
        this.stonyhurstAvailable = this.stonyhurstLatitude != 0.0 || this.stonyhurstLongitude != 0.0;
        
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
        newSolarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;

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
