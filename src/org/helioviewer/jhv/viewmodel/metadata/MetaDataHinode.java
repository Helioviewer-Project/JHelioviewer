package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;

class MetaDataHinode extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(4096, 4096);
	public MetaDataHinode(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION, metaDataContainer.get("TELESCOP"), metaDataContainer.get("WAVELNTH"));
        
        if (!instrument.equalsIgnoreCase("XRT"))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);

        instrument = "XRT";
        fullName = "XRT";
   }
}
