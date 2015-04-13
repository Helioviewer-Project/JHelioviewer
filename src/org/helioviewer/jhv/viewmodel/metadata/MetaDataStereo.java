package org.helioviewer.jhv.viewmodel.metadata;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.viewmodel.view.cache.HelioviewerDateTimeCache;

import ch.fhnw.i4ds.helio.coordinate.api.Angle;
import ch.fhnw.i4ds.helio.coordinate.api.CoordConverter;
import ch.fhnw.i4ds.helio.coordinate.converter.Hg2HccConverter;
import ch.fhnw.i4ds.helio.coordinate.coord.HeliocentricCartesianCoordinate;
import ch.fhnw.i4ds.helio.coordinate.coord.HeliographicCoordinate;

public class MetaDataStereo extends MetaData{


	public MetaDataStereo(MetaDataContainer metaDataContainer) {
        super(metaDataContainer);
        observatory = metaDataContainer.get("OBSRVTRY");
        measurement = metaDataContainer.get("WAVELNTH");
        if (measurement == null) {
            measurement = "" + metaDataContainer.tryGetDouble("WAVELNTH");
        }
        if (!((observatory.equalsIgnoreCase("STEREO_A") || observatory.equalsIgnoreCase("STEREO_B") && detector.equalsIgnoreCase("EUVI")))){
        	throw new NonSuitableMetaDataException("invalid instrument");
        }
        hasCorona = true;
        hasSphere = true;
        this.hasRotation = true;

        fullName = detector + " " + measurement;
        this.metaDataContainer = metaDataContainer;
        

        
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
        if (stonyhurstAvailable){
        	CoordConverter<HeliographicCoordinate, HeliocentricCartesianCoordinate> converter = new Hg2HccConverter();
        	Angle hgLongitude = new Angle(stonyhurstLongitude);
        	Angle hgLatitude = new Angle(stonyhurstLatitude);
        	HeliographicCoordinate hgc = new HeliographicCoordinate(hgLongitude, hgLatitude);
        	
        	HeliocentricCartesianCoordinate hcc = converter.convert(hgc);
        	this.orientation = new Vector3d(hcc.getX(), hcc.getY(), hcc.getZ());
        }
        this.calcDefaultRotation();
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
