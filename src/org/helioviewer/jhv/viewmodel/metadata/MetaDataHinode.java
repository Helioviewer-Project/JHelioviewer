package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataHinode extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(4096, 4096);
	public MetaDataHinode(Document _doc)
	{
        super(_doc, RESOLUTION, get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"));
        
        if (!instrument.equalsIgnoreCase("XRT"))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);

        instrument = "XRT";
        fullName = "XRT";
   }
}
