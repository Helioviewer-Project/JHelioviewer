package org.helioviewer.gl3d.view;

import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.physics.Astronomy;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.HEEQCoordinateSystem;
import org.helioviewer.gl3d.wcs.HeliocentricCartesianCoordinateSystem;
import org.helioviewer.gl3d.wcs.StonyhurstCoordinateSystem;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.MetaDataStereo;
import org.helioviewer.viewmodel.metadata.MetaDataStereoA_COR1;
import org.helioviewer.viewmodel.metadata.MetaDataStereoA_COR2;
import org.helioviewer.viewmodel.metadata.MetaDataStereoB_COR1;
import org.helioviewer.viewmodel.metadata.MetaDataStereoB_COR2;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;

/**
 * This view is responsible for providing information about the orientation of
 * the image layer. The orientation vector should give the image plane normal
 * and the coordinate system should define in which coordinate system this
 * normal is defined in.
 * 
 * @author Simon Sp���rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCoordinateSystemView extends AbstractGL3DView implements GL3DView {
    private CoordinateSystem coordinateSystem;

    private MetaDataView metaDataView;

    private CoordinateVector orientation;

    public void render3D(GL3DState state) {
        GL2 gl = state.gl;

        this.renderChild(gl);
    }

    public CoordinateSystem getCoordinateSystem() {
        return this.coordinateSystem;
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        if (this.metaDataView == null) {
            metaDataView = getAdapter(MetaDataView.class);
            MetaData metaData = metaDataView.getMetaData();

            initialiseCoordinateSystem(metaData);
        }
    }

    private void initialiseCoordinateSystem(MetaData metaData) {
        this.coordinateSystem = getDefaultCoordinateSystem();
        this.orientation = getDefaultOrientation();

        if (metaData instanceof MetaDataStereo || metaData instanceof MetaDataStereoA_COR1 || metaData instanceof MetaDataStereoA_COR2 || metaData instanceof MetaDataStereoB_COR1 || metaData instanceof MetaDataStereoB_COR2 )
        {
                // STEREO
                Log.debug("GL3DCoordinateSystemView: Creating STEREO Image Layer!");
                    if (metaData.isStonyhurstProvided()) {
                        Calendar c = new GregorianCalendar();
                        c.setTime(metaData.getDateTime().getTime());
                        double b0 = Astronomy.getB0InRadians(c);
                        this.coordinateSystem = new StonyhurstCoordinateSystem(b0);
                        this.orientation = this.coordinateSystem.createCoordinateVector(Math.toRadians(metaData.getStonyhurstLongitude()), Math.toRadians(metaData.getStonyhurstLatitude()), metaData.getDobs());
                        System.out.println("Stonyhurst-CoordinateSystem :  " + this.orientation);
                        Log.debug("GL3DCoordinateSystemView: Providing Stonyhurst Coordinate System and orientation");
                    } else if (metaData.isHEEQProvided()) {
                        Calendar c = new GregorianCalendar();
                        c.setTime(metaData.getDateTime().getTime());
                        double b0 = Astronomy.getB0InRadians(c);
                        this.coordinateSystem = new HEEQCoordinateSystem(b0);
                        this.orientation = this.coordinateSystem.createCoordinateVector(metaData.getHEEQX(), metaData.getHEEQY(), metaData.getHEEQZ());
                        System.out.println("HEEQ-CoordinateSystem :  " + this.orientation);
                        Log.debug("GL3DCoordinateSystemView: Providing HEEQ Coordinate System and orientation");
                    } else {
                        this.coordinateSystem = getDefaultCoordinateSystem();
                        this.orientation = getDefaultOrientation();
                    }
        }
        
        Log.debug("GL3DCoordinateSystem: CoordinateSystemView produced a " + this.coordinateSystem.getClass());
    }

    private static CoordinateSystem getDefaultCoordinateSystem() {
        return new HeliocentricCartesianCoordinateSystem();
        // return new HEECoordinateSystem();
    }

    private static CoordinateVector getDefaultOrientation() {
        return getDefaultCoordinateSystem().createCoordinateVector(0, 0, 1);
    }

    public CoordinateVector getOrientation() {
        return this.orientation;
    }
}
