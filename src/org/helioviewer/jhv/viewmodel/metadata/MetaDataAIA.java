package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LUT;
import org.w3c.dom.Document;

public class MetaDataAIA extends MetaData
{
	public MetaDataAIA(Document _doc)
	{
        super(_doc, new Vector2i(4096, 4096), get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"), "AIA "+get(_doc, "WAVELNTH"), GROUP_FOR_OPACITY_SUN | GROUP_FOR_OPACITY_CORONA_SMALL);
        
        if (!(instrument.equalsIgnoreCase("AIA_1") || instrument.equalsIgnoreCase("AIA_2") || instrument.equalsIgnoreCase("AIA_3") || instrument.equalsIgnoreCase("AIA_4")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
        
        switch (measurement)
        {
			case "131":
				defaultLUT = LUT.SDO_AIA_131;
				break;
			case "1600":
				defaultLUT = LUT.SDO_AIA_1600;
				break;
			case "1700":
				defaultLUT = LUT.SDO_AIA_1700;
				break;
			case "171":
				defaultLUT = LUT.SDO_AIA_171;
				break;
			case "193":
				defaultLUT = LUT.SDO_AIA_193;
				break;
			case "211":
				defaultLUT = LUT.SDO_AIA_211;
				break;
			case "304":
				defaultLUT = LUT.SDO_AIA_304;
				break;
			case "335":
				defaultLUT = LUT.SDO_AIA_335;
				break;
			case "4500":
				defaultLUT = LUT.SDO_AIA_4500;
				break;
			case "94":
				defaultLUT = LUT.SDO_AIA_94;
				break;
			default:
	        	UnsuitableMetaDataException e = new UnsuitableMetaDataException("Unexpected measurement: "+measurement);
	        	Telemetry.trackException(e);
	        	throw e;
		}
   }
}
