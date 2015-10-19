package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.w3c.dom.Document;

class MetaDataLASCO extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);

	@SuppressWarnings("null")
	public MetaDataLASCO(Document _doc)
	{
		super(_doc, RESOLUTION, get(_doc, "TELESCOP"), get(_doc, "FILTER") + " " + get(_doc, "POLAR"));
		if (!"LASCO".equalsIgnoreCase(instrument) || detector==null)
			throw new UnsuitableMetaDataException("invalid instrument: " + instrument + "/" + detector);
		
		double innerDefault=-1;
		double outerDefault=-1;
		
		switch(detector.toUpperCase())
		{
			case "C2":
				innerDefault = 2.3 * Constants.SUN_RADIUS;
				outerDefault = 8.0 * Constants.SUN_RADIUS;
				flatDistance = 6.2 * Constants.SUN_RADIUS;
				break;
			case "C3":
				innerDefault = 4.4 * Constants.SUN_RADIUS;
				outerDefault = 31.5 * Constants.SUN_RADIUS;
				flatDistance = 38 * Constants.SUN_RADIUS;
				break;
			default:
				throw new UnsuitableMetaDataException("invalid instrument: " + instrument + "/" + detector);
		}
		
		fullName = "LASCO " + detector;

		innerRadius = tryGetDouble(_doc, "HV_ROCC_INNER") * Constants.SUN_RADIUS;
		outerRadius = tryGetDouble(_doc, "HV_ROCC_OUTER") * Constants.SUN_RADIUS;

		if (Double.isNaN(innerRadius) || Double.isNaN(outerRadius) || innerRadius == 0.0)
		{
			innerRadius = innerDefault;
			outerRadius = outerDefault;
		}
		
		maskRotation = Math.toRadians(tryGetDouble(_doc, "CROTA"));
		
		//TODO: wut? will always stay 0...
		double centerX = 0, centerY = 0;

		// Convert arcsec to meters
		double cdelt1 = tryGetDouble(_doc, "CDELT1");
		double cdelt2 = tryGetDouble(_doc, "CDELT2");
		if (!Double.isNaN(cdelt1) && !Double.isNaN(cdelt2) && cdelt1 != 0 && cdelt2 != 0)
		{
			centerX = centerX / cdelt1;
			centerY = centerY / cdelt2;
		}

		occulterCenter = new Vector2d(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());
	}
}
