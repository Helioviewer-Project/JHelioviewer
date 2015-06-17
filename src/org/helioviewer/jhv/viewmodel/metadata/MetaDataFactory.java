package org.helioviewer.jhv.viewmodel.metadata;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.gui.MainFrame;

public class MetaDataFactory {
	@SuppressWarnings("unchecked")
  static final Class<MetaData>[] META_DATA_CLASSES = new Class[]{
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
		MetaDataHinode.class//,
		//MetaDataSWAP.class
	};
	
	
	public static MetaData getMetaData(MetaDataContainer metaDataContainer){
		
		MetaData metaData = null;
		Object[] args = {metaDataContainer};
		for (Class<MetaData> c : META_DATA_CLASSES){
			try {
			    Constructor<MetaData> constructor = c.getDeclaredConstructor(MetaDataContainer.class);
				metaData = constructor.newInstance(args);
			}
            catch(NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
            {
                if(e.getCause() instanceof NonSuitableMetaDataException)
                {
                    //ignore - we only tried the "wrong" metadata factory. a better fit
                    //should be found in a later iteration
                }
                else
                {
                    //reflection problems are not expected in practice
                    e.printStackTrace();
                }
            }
			if (metaData != null) break;
		}
		
		if (metaData != null){
			return metaData;
		}
		
		JOptionPane.showMessageDialog(MainFrame.MAIN_PANEL, "This data source's metadata could not be read.");
		return null;
		
	}
}
