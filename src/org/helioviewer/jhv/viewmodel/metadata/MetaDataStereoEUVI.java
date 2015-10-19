package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.coordinates.HeliographicCoordinate;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.w3c.dom.Document;

class MetaDataStereoEUVI extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(2048, 2048);
	public MetaDataStereoEUVI(Document _doc)
	{
        super(_doc, RESOLUTION, get(_doc, "OBSRVTRY"), get(_doc, "WAVELNTH"));

        if (!(("STEREO_A".equalsIgnoreCase(observatory) || "STEREO_B".equalsIgnoreCase(observatory) && "EUVI".equalsIgnoreCase(detector))))
        	throw new UnsuitableMetaDataException("invalid instrument");

        fullName = detector + " " + measurement;
        
        switch (measurement)
        {
			case "171":
				defaultLUT = Lut.STEREO_EUVI_171;
				break;
			case "195":
				defaultLUT = Lut.STEREO_EUVI_195;
				break;
			case "284":
				defaultLUT = Lut.STEREO_EUVI_284;
				break;
			case "304":
				defaultLUT = Lut.STEREO_EUVI_304;
				break;
			default:
	        	UnsuitableMetaDataException e = new UnsuitableMetaDataException("Unexpected WAVELNTH: "+measurement);
	        	Telemetry.trackException(e);
	        	throw e;
		}
        
        if (stonyhurstAvailable)
        {
        	HeliocentricCartesianCoordinate hcc = new HeliographicCoordinate(Math.toRadians(stonyhurstLongitude), Math.toRadians(stonyhurstLatitude)).toHeliocentricCartesianCoordinate();
        	orientation = new Vector3d(hcc.x, hcc.y, hcc.z);
        	defaultRotation = Quaternion3d.calcRotation(new Vector3d(0, 0, Constants.SUN_RADIUS), orientation);
        }
   }
}
