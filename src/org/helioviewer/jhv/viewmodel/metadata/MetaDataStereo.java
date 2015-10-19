package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.w3c.dom.Document;

class MetaDataStereo extends MetaData
{
	public MetaDataStereo(Document _doc)
	{
		super(_doc, "COR1".equalsIgnoreCase(get(_doc, "OBSRVTRY")) ? new Vector2i(512, 512) : new Vector2i(2048, 2048), get(_doc, "OBSRVTRY"), get(_doc, "WAVELNTH"));

		fullName = instrument + " " + detector;

		double centerX = 0, centerY = 0;

		// Convert arcsec to meters
		double cdelt1 = tryGetDouble(_doc, "CDELT1");
		double cdelt2 = tryGetDouble(_doc, "CDELT2");
		if (!Double.isNaN(cdelt1) && !Double.isNaN(cdelt2) && cdelt1 != 0 && cdelt2 != 0)
		{
			centerX = centerX / cdelt1;
			centerY = centerY / cdelt2;
		}
		
		double innerDefault = -1;
		double outerDefault = -1;
		if ("STEREO_A".equalsIgnoreCase(observatory) && "COR1".equalsIgnoreCase(observatory))
		{
			innerDefault = 1.36 * Constants.SUN_RADIUS;
			outerDefault = 4.5 * Constants.SUN_RADIUS;
			flatDistance = 4.5 * Constants.SUN_RADIUS;
			
			// HACK - manual adjustment for occulter center
			centerX += 1;
			centerY += 1;
		}
		else if ("STEREO_A".equalsIgnoreCase(observatory) && "COR2".equalsIgnoreCase(observatory))
		{
			innerDefault = 2.4 * Constants.SUN_RADIUS;
			outerDefault = 15.6 * Constants.SUN_RADIUS;
			flatDistance = 15.75 * Constants.SUN_RADIUS;
			
			// HACK - manual adjustment for occulter center
			centerX += 3;
			centerY += 6;
		}
		else if ("STEREO_B".equalsIgnoreCase(observatory) && "COR1".equalsIgnoreCase(observatory))
		{
			innerDefault = 1.5 * Constants.SUN_RADIUS;
			outerDefault = 4.9 * Constants.SUN_RADIUS;
			flatDistance = 4.95 * Constants.SUN_RADIUS;
			
			// HACK - manual adjustment for occulter center
			centerX += 1;
			centerY += 3;
		}
		else if ("STEREO_B".equalsIgnoreCase(observatory) && "COR2".equalsIgnoreCase(observatory))
		{
			innerDefault = 3.25 * Constants.SUN_RADIUS;
			outerDefault = 17 * Constants.SUN_RADIUS;
			flatDistance = 18 * Constants.SUN_RADIUS;
			
			// HACK - manual adjustment for occulter center
			centerX += 22;
			centerY -= 37;
		}
		else
			throw new UnsuitableMetaDataException("invalid instrument: " + observatory + "/" + detector);
		
		innerRadius = tryGetDouble(_doc, "HV_ROCC_INNER") * Constants.SUN_RADIUS;
		outerRadius = tryGetDouble(_doc, "HV_ROCC_OUTER") * Constants.SUN_RADIUS;

		if (Double.isNaN(innerRadius) || Double.isNaN(outerRadius) || innerRadius == 0.0)
		{
			innerRadius = innerDefault;
			outerRadius = outerDefault;
		}
		
		maskRotation = Math.toRadians(tryGetDouble(_doc, "CROTA"));
		occulterCenter = new Vector2d(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());
	}
}
