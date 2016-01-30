package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LUT;
import org.w3c.dom.Document;

class MetaDataEIT extends MetaData
{
	public MetaDataEIT(Document _doc)
	{
        super(_doc, new Vector2i(1024, 1024), get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"), "EIT " + get(_doc, "WAVELNTH"), GROUP_FOR_OPACITY_SUN | GROUP_FOR_OPACITY_CORONA_SMALL);
        
        if (!(instrument.equalsIgnoreCase("EIT")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
                
        switch (measurement)
        {
			case "171":
				defaultLUT = LUT.SOHO_EIT_171;
				break;
			case "195":
				defaultLUT = LUT.SOHO_EIT_195;
				break;
			case "284":
				defaultLUT = LUT.SOHO_EIT_284;
				break;
			case "304":
				defaultLUT = LUT.SOHO_EIT_304;
				break;
			default:
				UnsuitableMetaDataException e=new UnsuitableMetaDataException("Unexpected measurement: "+measurement);
				Telemetry.trackException(e);
				throw e;
		}
   }
}
