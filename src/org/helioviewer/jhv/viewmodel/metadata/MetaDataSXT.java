package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataSXT extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataSXT(Document _doc)
	{
	  	super(_doc, RESOLUTION, get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"));

        if (!instrument.equalsIgnoreCase("SXT"))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);

        instrument = "SXT";
        fullName = "SXT " + measurement;
   }
}
