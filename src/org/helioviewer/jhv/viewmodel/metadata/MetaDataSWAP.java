package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataSWAP extends MetaData
{	
	public MetaDataSWAP(Document _doc)
	{
        super(_doc, new Vector2i(1024, 1024), get(_doc, "TELESCOP"), get(_doc, "WAVELNTH"),"SWAP " + get(_doc, "WAVELNTH"), GROUP_FOR_OPACITY_SUN | GROUP_FOR_OPACITY_CORONA_SMALL);
        
        if (!"SWAP".equalsIgnoreCase(instrument) || !"PROBA2".equalsIgnoreCase(observatory))
        	throw new UnsuitableMetaDataException("invalid instrument: "+observatory+"/"+instrument+"/"+detector);
   }
}
