package org.helioviewer.jhv.viewmodel.metadata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;

class MetaDataLASCO_C2 extends MetaData{

	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataLASCO_C2(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION);
        if (!("LASCO".equalsIgnoreCase(instrument) && "C2".equalsIgnoreCase(detector)))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument+"/"+detector);

        String measurement1 = metaDataContainer.get("FILTER");
        String measurement2 = metaDataContainer.get("POLAR");
        measurement = measurement1 + " " + measurement2;

        observatory = metaDataContainer.get("TELESCOP");
        fullName = "LASCO " + detector;


        String observedDate = metaDataContainer.get("DATE_OBS");
        observedDate += "T" + metaDataContainer.get("TIME_OBS");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");
        localDateTime = LocalDateTime.parse(observedDate, formatter);

        readPixelParameters(metaDataContainer);

        innerRadius = metaDataContainer.tryGetDouble("HV_ROCC_INNER") * Constants.SUN_RADIUS;
        outerRadius = metaDataContainer.tryGetDouble("HV_ROCC_OUTER") * Constants.SUN_RADIUS;

        if (innerRadius == 0.0 && detector != null) {
        	innerRadius = 2.3 * Constants.SUN_RADIUS;
            outerRadius = 8.0 * Constants.SUN_RADIUS;
        }
        
        flatDistance = 6.2 * Constants.SUN_RADIUS;
        maskRotation = Math.toRadians(metaDataContainer.tryGetDouble("CROTA"));

        double centerX = 0, centerY = 0;
        
        //Convert arcsec to meters
        double cdelt1 = metaDataContainer.tryGetDouble("CDELT1");
        double cdelt2 = metaDataContainer.tryGetDouble("CDELT2");
        if( cdelt1 != 0 && cdelt2 != 0) {
            centerX = centerX / cdelt1;
            centerY = centerY / cdelt2;
        }
        
        occulterCenter = new Vector2d(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());

}
}
