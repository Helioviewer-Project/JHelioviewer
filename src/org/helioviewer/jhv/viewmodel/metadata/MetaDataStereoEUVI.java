package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LUT;
import org.w3c.dom.Document;

class MetaDataStereoEUVI extends MetaData
{
	public MetaDataStereoEUVI(Document _doc)
	{
        super(_doc,
        		new Vector2i(2048, 2048),
        		get(_doc, "OBSRVTRY"),
        		get(_doc, "WAVELNTH"),
        		get(_doc, "DETECTOR") + ("STEREO_A".equalsIgnoreCase(get(_doc, "OBSRVTRY")) ? "-A ":"-B ") + get(_doc, "WAVELNTH"),
        		GROUP_FOR_OPACITY_SUN | GROUP_FOR_OPACITY_CORONA_SMALL);
        
        if ((!"STEREO_A".equalsIgnoreCase(observatory) && !"STEREO_B".equalsIgnoreCase(observatory)) || !"EUVI".equalsIgnoreCase(detector))
        	throw new UnsuitableMetaDataException("invalid instrument");
        
        switch (measurement)
        {
			case "171":
				defaultLUT = LUT.STEREO_EUVI_171;
				break;
			case "195":
				defaultLUT = LUT.STEREO_EUVI_195;
				break;
			case "284":
				defaultLUT = LUT.STEREO_EUVI_284;
				break;
			case "304":
				defaultLUT = LUT.STEREO_EUVI_304;
				break;
			default:
	        	UnsuitableMetaDataException e = new UnsuitableMetaDataException("Unexpected WAVELNTH: "+measurement);
	        	Telemetry.trackException(e);
	        	throw e;
		}
   }
}
