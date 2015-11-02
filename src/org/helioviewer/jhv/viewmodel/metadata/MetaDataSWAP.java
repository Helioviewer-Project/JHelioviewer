package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataSWAP extends MetaData{
	
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataSWAP(Document _doc)
	{
        super(_doc, RESOLUTION, get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"));
        
        fullName = "SWAP " + measurement;
        
        if (!"SWAP".equalsIgnoreCase(instrument) || !"PROBA2".equalsIgnoreCase(observatory))
        	throw new UnsuitableMetaDataException("invalid instrument: "+observatory+"/"+instrument+"/"+detector);
   }
}
