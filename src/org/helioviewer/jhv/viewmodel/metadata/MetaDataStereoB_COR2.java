package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.Rectangle;
import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.physics.Constants;

public class MetaDataStereoB_COR2 extends MetaData{

	private final static Rectangle RESOLUTION = new Rectangle(2048, 2048);
	private final double IMAGE_SCALE = 14.7;

	public MetaDataStereoB_COR2(MetaDataContainer metaDataContainer) {
        super(metaDataContainer, RESOLUTION);
        observatory = metaDataContainer.get("OBSRVTRY");
        measurement = metaDataContainer.get("WAVELNTH");
        if (measurement == null) {
            measurement = "" + metaDataContainer.tryGetDouble("WAVELNTH");
        }
        fullName = instrument + " " + detector;
        
        if (!(observatory.equalsIgnoreCase("STEREO_B") && detector.equalsIgnoreCase("COR2"))){
        	throw new NonSuitableMetaDataException("invalid instrument: "+observatory+"/"+detector);
        }
        this.metaDataContainer = metaDataContainer;

        String observedDate = metaDataContainer.get("DATE_OBS");
        localDateTime = LocalDateTime.parse(observedDate, DATE_FORMAT);

        updatePixelParameters();
        
        innerRadius = metaDataContainer.tryGetDouble("HV_ROCC_INNER") * Constants.SUN_RADIUS;
        outerRadius = metaDataContainer.tryGetDouble("HV_ROCC_OUTER") * Constants.SUN_RADIUS;
        
        if (innerRadius == 0.0 && detector != null) {
            innerRadius = 3.25 * Constants.SUN_RADIUS;
            outerRadius = 17 * Constants.SUN_RADIUS;
        }

        flatDistance = 18 * Constants.SUN_RADIUS;

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
        centerX += 22;
        centerY -= 37;

        occulterCenter = new Vector2d(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());

    }
}
