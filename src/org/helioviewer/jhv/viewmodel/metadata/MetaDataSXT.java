package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;

class MetaDataSXT extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataSXT(MetaDataContainer metaDataContainer)
	{
	  	super(metaDataContainer, RESOLUTION, metaDataContainer.get("TELESCOP"), metaDataContainer.get("WAVELNTH"));

        if (!instrument.equalsIgnoreCase("SXT"))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);

        instrument = "SXT";
        fullName = "SXT " + measurement;
   }
}
