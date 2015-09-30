package org.helioviewer.jhv.viewmodel.metadata;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.helioviewer.jhv.Telemetry;
import org.w3c.dom.Document;

public class MetaDataFactory
{
	@SuppressWarnings("unchecked")
	static final Class<MetaData>[] META_DATA_CLASSES = new Class[]
		{
			MetaDataAIA.class,
			MetaDataEIT.class,
			MetaDataHMI.class,
			MetaDataLASCO_C2.class,
			MetaDataLASCO_C3.class,
			MetaDataMDI.class,
			MetaDataStereo.class,
			MetaDataStereoA_COR1.class,
			MetaDataStereoA_COR2.class,
			MetaDataStereoB_COR1.class,
			MetaDataStereoB_COR2.class,
			MetaDataHinode.class,
			MetaDataSXT.class,
			MetaDataSWAP.class
		};
	
	public static MetaData getMetaData(Document doc)
	{
		MetaDataContainer metaDataContainer = new MetaDataContainer(doc);
		MetaData metaData = null;
		Object[] args = {metaDataContainer};
		for (Class<MetaData> c : META_DATA_CLASSES)
			try
			{
			    Constructor<MetaData> constructor = c.getDeclaredConstructor(MetaDataContainer.class);
				metaData = constructor.newInstance(args);
				break;
			}
            catch(NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
            {
                if(e.getCause() instanceof UnsuitableMetaDataException)
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

		
		return metaData;
	}
}
