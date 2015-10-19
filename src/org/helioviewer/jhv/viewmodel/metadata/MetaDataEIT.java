package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.w3c.dom.Document;

class MetaDataEIT extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataEIT(Document _doc)
	{
        super(_doc, RESOLUTION, get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"));
        
        if (!(instrument.equalsIgnoreCase("EIT")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
                
        fullName = "EIT " + measurement;

        switch (measurement)
        {
			case "171":
				defaultLUT = Lut.SOHO_EIT_171;
				break;
			case "195":
				defaultLUT = Lut.SOHO_EIT_195;
				break;
			case "284":
				defaultLUT = Lut.SOHO_EIT_284;
				break;
			case "304":
				defaultLUT = Lut.SOHO_EIT_304;
				break;
			default:
				UnsuitableMetaDataException e=new UnsuitableMetaDataException("Unexpected measurement: "+measurement);
				Telemetry.trackException(e);
				throw e;
		}
   }
}
