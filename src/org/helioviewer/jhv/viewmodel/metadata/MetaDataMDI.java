package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataMDI extends MetaData
{
	public MetaDataMDI(Document _doc)
	{
        super(_doc, new Vector2i(1024, 1024), get(_doc, "TELESCOP"), get(_doc, "DPC_OBSR"), "MDI " + get(_doc, "DPC_OBSR"), GROUP_FOR_OPACITY_SUN);
        if (!(instrument.equalsIgnoreCase("MDI")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
   }
}
