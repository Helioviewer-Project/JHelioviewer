package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;

class MetaDataStereoA_COR1 extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(512, 512);
	public MetaDataStereoA_COR1(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION);
        observatory = metaDataContainer.get("OBSRVTRY");
        measurement = metaDataContainer.get("WAVELNTH");
        if (measurement == null) {
            measurement = "" + metaDataContainer.tryGetDouble("WAVELNTH");
        }
        fullName = instrument + " " + detector;
        
        if (!("STEREO_A".equalsIgnoreCase(observatory) && "COR1".equalsIgnoreCase(observatory)))
        	throw new UnsuitableMetaDataException("invalid instrument: "+observatory+"/"+detector);
        
        readPixelParameters(metaDataContainer);

        innerRadius = metaDataContainer.tryGetDouble("HV_ROCC_INNER") * Constants.SUN_RADIUS;
        outerRadius = metaDataContainer.tryGetDouble("HV_ROCC_OUTER") * Constants.SUN_RADIUS;

        if (innerRadius == 0.0 && detector != null) {
            innerRadius = 1.36 * Constants.SUN_RADIUS;
            outerRadius = 4.5 * Constants.SUN_RADIUS;
        }
        
        flatDistance = 4.5 * Constants.SUN_RADIUS;
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
        centerY += 1;
 
        occulterCenter = new Vector2d(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());

   }
}
