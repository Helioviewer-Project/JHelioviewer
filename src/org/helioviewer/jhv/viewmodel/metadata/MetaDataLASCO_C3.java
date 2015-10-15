package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;

class MetaDataLASCO_C3 extends MetaData{

	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataLASCO_C3(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION, metaDataContainer.get("TELESCOP"), metaDataContainer.get("FILTER") + " " + metaDataContainer.get("POLAR"));
        if (!("LASCO".equalsIgnoreCase(instrument) && "C3".equalsIgnoreCase(detector)))
           	throw new UnsuitableMetaDataException("invalid instrument: "+instrument+"/"+detector);

        fullName = "LASCO " + detector;

        readPixelParameters(metaDataContainer);
        
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
