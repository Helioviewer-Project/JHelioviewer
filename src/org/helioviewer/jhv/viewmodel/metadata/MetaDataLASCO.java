package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.w3c.dom.Document;

class MetaDataLASCO extends MetaData
{
	public MetaDataLASCO(Document _doc)
	{
		super(_doc, new Vector2i(1024, 1024), get(_doc, "TELESCOP"), get(_doc, "FILTER") + " " + get(_doc, "POLAR"), "LASCO "+get(_doc, "DETECTOR"));
		if (!"LASCO".equalsIgnoreCase(instrument) || detector==null)
			throw new UnsuitableMetaDataException("invalid instrument: " + instrument + "/" + detector);
		
		double innerDefault;
		double outerDefault;
		
		switch(detector.toUpperCase())
		{
			case "C2":
				innerDefault = 2.3 * Constants.SUN_RADIUS;
				outerDefault = 8.0 * Constants.SUN_RADIUS;
				flatDistance = 6.2 * Constants.SUN_RADIUS;
				solarPixelRadius = new Vector2d(80.814221, 80.814221);
				break;
			case "C3":
				innerDefault = 4.4 * Constants.SUN_RADIUS;
				outerDefault = 31.5 * Constants.SUN_RADIUS;
				flatDistance = 38 * Constants.SUN_RADIUS;
				solarPixelRadius = new Vector2d(17.173021, 17.173021);
				break;
			default:
				throw new UnsuitableMetaDataException("invalid instrument: " + instrument + "/" + detector);
		}
		
		innerRadius = tryGetDouble(_doc, "HV_ROCC_INNER") * Constants.SUN_RADIUS;
		outerRadius = tryGetDouble(_doc, "HV_ROCC_OUTER") * Constants.SUN_RADIUS;

		if (Double.isNaN(innerRadius) || Double.isNaN(outerRadius) || innerRadius == 0.0)
		{
			innerRadius = innerDefault;
			outerRadius = outerDefault;
		}
		
		//TODO: wut? will always stay 0...
		Vector2d center=new Vector2d(0,0);

		// Convert arcsec to meters
		double cdelt1 = tryGetDouble(_doc, "CDELT1");
		double cdelt2 = tryGetDouble(_doc, "CDELT2");
		if (!Double.isNaN(cdelt1) && !Double.isNaN(cdelt2) && cdelt1 != 0 && cdelt2 != 0)
			center = center.scaled(cdelt1, cdelt2);

		occulterCenter = center.scaled(getUnitsPerPixel());
	}
}
