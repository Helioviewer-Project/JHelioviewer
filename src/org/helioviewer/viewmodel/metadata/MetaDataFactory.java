package org.helioviewer.viewmodel.metadata;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.gui.states.GuiState3DWCS;

public class MetaDataFactory {
	
	static final List<Class<MetaData>> ads = new ArrayList<Class<MetaData>>();
	
	@SuppressWarnings("unchecked")
  static final Class<MetaData>[] metaDataClasses = new Class[]{
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
		MetaDataSWAP.class
	};
	
	
	public static MetaData getMetaData(MetaDataContainer metaDataContainer){
		
		MetaData metaData = null;
		Object[] args = {metaDataContainer};
		for (Class<MetaData> c : metaDataClasses){
			Constructor<MetaData> constructor;
			try {
				constructor = c.getDeclaredConstructor(MetaDataContainer.class);
				metaData = constructor.newInstance(args);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				metaData = null;
				//e.printStackTrace();
			}
			if (metaData != null) break;
		}
		
		if (metaData != null){
			return metaData;
		}
		
		JOptionPane.showMessageDialog(new GuiState3DWCS().getMainComponentView().getComponent(), "Not supported Metadata's");
		return null;
		
	}
}
