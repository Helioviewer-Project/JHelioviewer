package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.w3c.dom.Document;

class MetaDataStereoCOR extends MetaData
{
	public MetaDataStereoCOR(Document _doc)
	{
		super(_doc, "COR1".equalsIgnoreCase(get(_doc, "OBSRVTRY")) ? new Vector2i(512, 512) : new Vector2i(2048, 2048),
				get(_doc, "OBSRVTRY"),
				get(_doc, "WAVELNTH"),
				get(_doc, "INSTRUME") + " " + get(_doc, "DETECTOR"),
				GROUP_FOR_OPACITY_CORONA_OUTSIDE);

		Vector2d center = Vector2d.NULL;

		// Convert arcsec to meters
		double cdelt1 = tryGetDouble(_doc, "CDELT1");
		double cdelt2 = tryGetDouble(_doc, "CDELT2");
		if (!Double.isNaN(cdelt1) && !Double.isNaN(cdelt2) && cdelt1 != 0 && cdelt2 != 0)
		{
			//TODO: center will always be Vector2d.NULL anyway?!
			center = center.scaled(1/cdelt1, 1/cdelt2);
		}
		
		double innerDefault;
		double outerDefault;
		if ("STEREO_A".equalsIgnoreCase(observatory) && "COR1".equalsIgnoreCase(detector))
		{
			innerDefault = 1.36 * Constants.SUN_RADIUS;
			outerDefault = 4.5 * Constants.SUN_RADIUS;
			flatDistance = 4.5 * Constants.SUN_RADIUS;
			
			// HACK - manual adjustment for occulter center
			center=center.add(new Vector2d(1,1));
		}
		else if ("STEREO_A".equalsIgnoreCase(observatory) && "COR2".equalsIgnoreCase(detector))
		{
			innerDefault = 2.4 * Constants.SUN_RADIUS;
			outerDefault = 15.6 * Constants.SUN_RADIUS;
			flatDistance = 15.75 * Constants.SUN_RADIUS;
			
			// HACK - manual adjustment for occulter center
			center=center.add(new Vector2d(3,6));
		}
		else if ("STEREO_B".equalsIgnoreCase(observatory) && "COR1".equalsIgnoreCase(detector))
		{
			innerDefault = 1.5 * Constants.SUN_RADIUS;
			outerDefault = 4.9 * Constants.SUN_RADIUS;
			flatDistance = 4.95 * Constants.SUN_RADIUS;
			
			// HACK - manual adjustment for occulter center
			center=center.add(new Vector2d(1,3));
		}
		else if ("STEREO_B".equalsIgnoreCase(observatory) && "COR2".equalsIgnoreCase(detector))
		{
			innerDefault = 3.25 * Constants.SUN_RADIUS;
			outerDefault = 17 * Constants.SUN_RADIUS;
			flatDistance = 18 * Constants.SUN_RADIUS;
			
			// HACK - manual adjustment for occulter center
			center=center.add(new Vector2d(22,-37));
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
		
		occulterCenter = center.scaled(getUnitsPerPixel());
	}
}
