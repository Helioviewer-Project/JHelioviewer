package org.helioviewer.jhv.viewmodel.metadata;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.w3c.dom.Document;

public class MetaDataFactory
{
	@SuppressWarnings("unchecked")
	static final Class<MetaData>[] META_DATA_CLASSES = new Class[]
		{
			MetaDataAIA.class,
			MetaDataEIT.class,
			MetaDataHMI.class,
			MetaDataLASCO.class,
			MetaDataMDI.class,
			MetaDataStereoEUVI.class,
			MetaDataStereoCOR.class,
			MetaDataHinode.class,
			MetaDataSXT.class,
			MetaDataSWAP.class
		};
	
	public static @Nullable MetaData getMetaData(@Nullable Document _doc)
	{
		if(_doc==null)
			return null;
		
		for (Class<MetaData> c : META_DATA_CLASSES)
			try
			{
			    Constructor<MetaData> constructor = c.getDeclaredConstructor(Document.class);
				return constructor.newInstance(_doc);
			}
            catch(NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
            {
                if(e.getCause() instanceof UnsuitableMetaDataException || e.getCause() instanceof NullPointerException)
                {
                    //ignore - we only tried the "wrong" metadata factory. a better fit
                    //should be found in a later iteration
                }
                else
                {
                    //reflection problems are not expected in practice
                    Telemetry.trackException(e);
                }
            }
		
		return null;
	}
}
