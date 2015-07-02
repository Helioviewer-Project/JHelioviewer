package org.helioviewer.jhv.plugins.hekplugin;

import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.DifferentialRotation;

/**
 * This class copes all coordinate transformations needed in JHV.
 * <p>
 * The old transformations are errornous and to be abolished, because they are
 * very confusing, since they are spread of too many classes
 * <p>
 * IT IS CURRENTLY HEAVILY WORKED ON - SO DON'T TRUST IT! IT IS ALSO NOT WELL
 * DOCUMENTED! SIMPLY IGNORE THIS FILE!
 * 
 * @author Malte Nuhn
 * 
 */
public class HEKCoordinateTransform
{


    public static SphericalCoord StonyhurstRotateStonyhurst(SphericalCoord stony, double timeDifferenceInSeconds) {
        double latitude = stony.theta / 180.0 * Math.PI;
        double degrees = DifferentialRotation.calculateRotationInDegrees(latitude, timeDifferenceInSeconds);

        SphericalCoord result = new SphericalCoord(stony);

        result.phi += degrees;
        result.phi %= 360.0; // was 180, but does this make sense?

        return result;
    }

    /**
     * Note: This is related to the current point of observation
     * 
     * @param stony
     * @return
     */
    public static HeliocentricCartesianCoordinate StonyhurstToHeliocentricCartesian(SphericalCoord stony, double bzero, double phizero)
    {
        double x = stony.r * Math.cos(stony.theta / MathUtils.RAD_TO_DEG) * Math.sin((stony.phi - phizero) / MathUtils.RAD_TO_DEG);
        double y = stony.r * (Math.sin(stony.theta / MathUtils.RAD_TO_DEG) * Math.cos(bzero / MathUtils.RAD_TO_DEG) - Math.cos(stony.theta / MathUtils.RAD_TO_DEG) * Math.cos((stony.phi - phizero) / MathUtils.RAD_TO_DEG) * Math.sin(bzero / MathUtils.RAD_TO_DEG));
        double z = stony.r * (Math.sin(stony.theta / MathUtils.RAD_TO_DEG) * Math.sin(bzero / MathUtils.RAD_TO_DEG) + Math.cos(stony.theta / MathUtils.RAD_TO_DEG) * Math.cos((stony.phi - phizero) / MathUtils.RAD_TO_DEG) * Math.cos(bzero / MathUtils.RAD_TO_DEG));
    	return new HeliocentricCartesianCoordinate(x, y, z);
    }

    public static SphericalCoord CartesianToStonyhurst(Vector3d cart){
        SphericalCoord result = new SphericalCoord();
        result.r = Math.sqrt(cart.x * cart.x + cart.y * cart.y + cart.z * cart.z);
        result.theta = Math.asin(cart.y/result.r) * MathUtils.RAD_TO_DEG;
        result.phi = Math.atan2(cart.x, cart.z) * MathUtils.RAD_TO_DEG;
        return result;
    }

    public static boolean stonyIsVisible(SphericalCoord stony) {
        stony.phi = makePhi(stony.phi);
        if (stony.phi < -90 || stony.phi > 90)
            return false;
        return true;
    }

    public static double makePhi(double phi) {
        phi += 180.0;
        phi %= 360.0;
        phi -= 180.0;
        return phi;
    }

}
