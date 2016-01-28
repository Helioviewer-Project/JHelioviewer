package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataHinode extends MetaData
{
	public MetaDataHinode(Document _doc)
	{
        super(_doc, new Vector2i(4096, 4096), get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"),"XRT", GROUP_FOR_OPACITY_SUN | GROUP_FOR_OPACITY_CORONA_SMALL);
        
        if (!instrument.equalsIgnoreCase("XRT"))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
   }
}
