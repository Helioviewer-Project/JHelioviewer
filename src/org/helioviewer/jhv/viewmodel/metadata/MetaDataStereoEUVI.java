package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.w3c.dom.Document;

class MetaDataStereoEUVI extends MetaData
{
	public MetaDataStereoEUVI(Document _doc)
	{
        super(_doc, new Vector2i(2048, 2048), get(_doc, "OBSRVTRY"), get(_doc, "WAVELNTH"), get(_doc, "DETECTOR") + " " + get(_doc, "WAVELNTH"));
        
        if ((!"STEREO_A".equalsIgnoreCase(observatory) && !"STEREO_B".equalsIgnoreCase(observatory)) || !"EUVI".equalsIgnoreCase(detector))
        	throw new UnsuitableMetaDataException("invalid instrument");
        
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
   }
}
