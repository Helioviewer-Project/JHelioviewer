package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataSXT extends MetaData
{
	public MetaDataSXT(Document _doc)
	{
	  	super(_doc, new Vector2i(1024, 1024), get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"),"SXT " + get(_doc, "WAVELNTH"), GROUP_FOR_OPACITY_SUN | GROUP_FOR_OPACITY_CORONA_SMALL);

        if (!instrument.equalsIgnoreCase("SXT"))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
   }
}
