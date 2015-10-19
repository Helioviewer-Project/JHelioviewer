package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;

class MetaDataMDI extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataMDI(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION, metaDataContainer.get("TELESCOP"), metaDataContainer.get("DPC_OBSR"));
        if (!(instrument.equalsIgnoreCase("MDI")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
	
        fullName = "MDI " + measurement.substring(3, 6);
   }
}
