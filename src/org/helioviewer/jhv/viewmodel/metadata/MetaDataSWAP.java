package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;

class MetaDataSWAP extends MetaData{
	
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataSWAP(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION, metaDataContainer.get("TELESCOP"), metaDataContainer.get("WAVELNTH"));
        instrument = "SWAP";
        fullName = "SWAP " + measurement;
        
        if (!(instrument.contains("SWAP")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+observatory+"/"+instrument+"/"+detector);
   }
}
