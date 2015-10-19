package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataMDI extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataMDI(Document _doc)
	{
        super(_doc, RESOLUTION, get(_doc, "TELESCOP"), get(_doc, "DPC_OBSR"));
        if (!(instrument.equalsIgnoreCase("MDI")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
        
        fullName = "MDI " + measurement.substring(3, 6);
   }
}
