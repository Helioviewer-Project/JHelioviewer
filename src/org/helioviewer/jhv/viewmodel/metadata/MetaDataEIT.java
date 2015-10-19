package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LUT.Lut;

class MetaDataEIT extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataEIT(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION, metaDataContainer.get("TELESCOP"), metaDataContainer.get("WAVELNTH"));
        
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
				break;
		}
   }
}
