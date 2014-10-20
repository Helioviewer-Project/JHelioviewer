package org.helioviewer.gl3d.model.image;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.metadata.MetaDataLASCO_C2;
import org.helioviewer.viewmodel.metadata.MetaDataLASCO_C3;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;

/**
 * Factory to be used for creating GL3DImageLayer Objects. This class is used by
 * the GL3DSceneGraphView.
 * 
 * @author Simon Sp���rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageLayerFactory {

    public static GL3DImageLayer createImageLayer(GL3DState state, GL3DView mainView) {
        MetaData metaData = mainView.getAdapter(MetaDataView.class).getMetaData();

        GL3DImageLayer imageLayer = null;

        if (metaData instanceof MetaDataLASCO_C2 || metaData instanceof MetaDataLASCO_C3) {
            // LASCO
            Log.debug("GL3DImageLayerFactory: Creating LASCO Image Layer");
            return new GL3DLascoImageLayer(mainView);
        } else{
        	
            if (metaData.getInstrument().equalsIgnoreCase("MDI")) {
                // MDI
                return new GL3DMDIImageLayer(mainView);
            } else if (metaData.getInstrument().equalsIgnoreCase("HMI")) {
                // HMI
                Log.debug("GL3DImageLayerFactory: Creating HMI Image Layer!");
                return new GL3DHMIImageLayer(mainView);
            } else if (metaData.getInstrument().equalsIgnoreCase("EIT")) {
                // EIT
                return new GL3DEITImageLayer(mainView);
            } else if (metaData.getInstrument().equalsIgnoreCase("AIA")) {
            	// AIA
            	return new GL3DAIAImageLayer(mainView);
            } else {
                // STEREO
                return new GL3DStereoImageLayer(mainView);
            }
        }
    }
}
