package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.Rectangle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.physics.Constants;

public class MetaDataLASCO_C3 extends MetaData{

	private final static Rectangle RESOLUTION = new Rectangle(1024, 1024);
	private final double IMAGE_SCALE = 56.0;

	public MetaDataLASCO_C3(MetaDataContainer metaDataContainer) {
        super(metaDataContainer, RESOLUTION);
        if (!(instrument.equalsIgnoreCase("LASCO") && detector.equalsIgnoreCase("C3"))){
           	throw new NonSuitableMetaDataException("invalid instrument: "+instrument+"/"+detector);
        }

        String measurement1 = metaDataContainer.get("FILTER");
        String measurement2 = metaDataContainer.get("POLAR");
        measurement = measurement1 + " " + measurement2;

        observatory = metaDataContainer.get("TELESCOP");
        fullName = "LASCO " + detector;
        this.metaDataContainer = metaDataContainer;


        String observedDate = metaDataContainer.get("DATE_OBS");
        observedDate += "T" + metaDataContainer.get("TIME_OBS");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");
        localDateTime = LocalDateTime.parse(observedDate, formatter);

        updatePixelParameters();
        
        innerRadius = metaDataContainer.tryGetDouble("HV_ROCC_INNER") * Constants.SUN_RADIUS;
        outerRadius = metaDataContainer.tryGetDouble("HV_ROCC_OUTER") * Constants.SUN_RADIUS;

        if (innerRadius == 0.0 && detector != null) {
            innerRadius = 4.4 * Constants.SUN_RADIUS;
            outerRadius = 31.5 * Constants.SUN_RADIUS;
        }
        
        flatDistance = 38 * Constants.SUN_RADIUS;
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
