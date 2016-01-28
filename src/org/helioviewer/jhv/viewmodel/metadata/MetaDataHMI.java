package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataHMI extends MetaData
{
	@SuppressWarnings("null")
	public MetaDataHMI(Document _doc)
	{
        super(_doc, new Vector2i(4096, 4096), get(_doc, "TELESCOP"), get(_doc, "CONTENT"),
        		"HMI " + get(_doc, "CONTENT").substring(0, 1) + get(_doc, "CONTENT").substring(1, 3).toLowerCase(), GROUP_FOR_OPACITY_SUN);
        
        if (!(instrument.equalsIgnoreCase("HMI_FRONT2")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
   }
}
