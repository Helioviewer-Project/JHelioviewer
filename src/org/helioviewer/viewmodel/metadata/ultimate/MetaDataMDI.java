package org.helioviewer.viewmodel.metadata.ultimate;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.MathUtils;
import org.helioviewer.base.math.RectangleDouble;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.metadata.MetaDataContainer;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.cache.HelioviewerDateTimeCache;

public class MetaDataMDI extends UltimateMetaData{

	public MetaDataMDI(MetaDataContainer metaDataContainer) {
		this.instrument = "AIA";
        measurement = metaDataContainer.get("WAVELNTH");
        observatory = metaDataContainer.get("TELESCOP");
        fullName = "AIA " + measurement;
        
        String observedDate = metaDataContainer.get("DATE_OBS");
        time = HelioviewerDateTimeCache.parseDateTime(observedDate);

        
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
	
	

}
