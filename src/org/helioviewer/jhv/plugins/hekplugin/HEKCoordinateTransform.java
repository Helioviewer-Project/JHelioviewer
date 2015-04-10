package org.helioviewer.jhv.plugins.hekplugin;

import java.util.Date;

import org.helioviewer.jhv.base.math.CartesianCoord;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
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
    /**
     * Rotanional Spherical Coordinates, where the Z axis is aligned to the
     * NORTH / SOUTH axis
     * 
     * This takes Spherical Coordinates, Transforms it to Cartesian, Switches
     * Axes,
     * 
     * result.x = input.z; result.y = input.x; result.z = input.y;
     * 
     * And transforms back to Spherical Coordinates
     * 
     * @param stony
     * @return
     */
    public static SphericalCoord StonyhurstToRotational(SphericalCoord stony) {

        CartesianCoord cart = StonyhurstToHeliocentricCartesian(stony, 0.0, 0.0);
        // switch axis!
        CartesianCoord rotational = new CartesianCoord();
        rotational.x = cart.z;
        rotational.y = cart.x;
        rotational.z = cart.y;

        SphericalCoord result = CartesianToSpherical(rotational);

        return result;
    }

    
    /**
     * Inverse operation...
     * 
     * @param stony
     * @return
     */
    public static SphericalCoord RotationalToStonyhurst(SphericalCoord rotational) {

        CartesianCoord cart = StonyhurstToHeliocentricCartesian(rotational, 0.0, 0.0);
        // switch axis!
        CartesianCoord swapped = new CartesianCoord();
        swapped.x = cart.y;
        swapped.y = cart.z;
        swapped.z = cart.x;

        return CartesianToSpherical(swapped);
    }

    public static SphericalCoord CartesianToSpherical(Vector3d cart) {
        return CartesianToSpherical(new CartesianCoord(cart));
    }

    public static SphericalCoord CartesianToSpherical(CartesianCoord cart) {
        SphericalCoord result = new SphericalCoord();
        result.r = Math.sqrt(cart.x * cart.x + cart.y * cart.y + cart.z * cart.z);
        result.theta = Math.atan(cart.x / Math.sqrt(cart.y * cart.y + cart.z * cart.z)) * MathUtils.RAD_TO_DEG;
        result.phi = Math.atan2(cart.z, cart.y) * MathUtils.RAD_TO_DEG;
        return result;
    }

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
    public static CartesianCoord StonyhurstToHeliocentricCartesian(SphericalCoord stony, double bzero, double phizero)
    {
        CartesianCoord result = new CartesianCoord();
        result.x = stony.r * Math.cos(stony.theta / MathUtils.RAD_TO_DEG) * Math.sin((stony.phi - phizero) / MathUtils.RAD_TO_DEG);
        result.y = stony.r * (Math.sin(stony.theta / MathUtils.RAD_TO_DEG) * Math.cos(bzero / MathUtils.RAD_TO_DEG) - Math.cos(stony.theta / MathUtils.RAD_TO_DEG) * Math.cos((stony.phi - phizero) / MathUtils.RAD_TO_DEG) * Math.sin(bzero / MathUtils.RAD_TO_DEG));
        result.z = stony.r * (Math.sin(stony.theta / MathUtils.RAD_TO_DEG) * Math.sin(bzero / MathUtils.RAD_TO_DEG) + Math.cos(stony.theta / MathUtils.RAD_TO_DEG) * Math.cos((stony.phi - phizero) / MathUtils.RAD_TO_DEG) * Math.cos(bzero / MathUtils.RAD_TO_DEG));
        return result;
    }

    public static SphericalCoord CartesianToStonyhurst(Vector3d cart){
        SphericalCoord result = new SphericalCoord();
        result.r = Math.sqrt(cart.x * cart.x + cart.y * cart.y + cart.z * cart.z);
        result.theta = Math.asin(cart.y/result.r) * MathUtils.RAD_TO_DEG;
        result.phi = Math.atan2(cart.x, cart.z) * MathUtils.RAD_TO_DEG;
        return result;
    }
    
    /*
     * HELIOCENTRIC EARTH EQUATORIAL (X,Y,Z)
     * 
     * Direct transformation from HELIOGRAPHIC STONYHURST
     * 
     * X = r cos(theta) cos(phi) Y = r cos(theta) sin(phi) Z = r sin(theta)
     * 
     * HOLDS ONLY IF FOR NON-TERRESTRIAL OBSERVERS, THE ORIGIN WILL STILL BE
     * REFERENCED TO THE CENTRAL MERIDIAN AS SEEN FROM EARTH!
     */

    public static boolean isVisibleStonyhurst(double theta, double phi) {
        if (phi > 90 || phi < -90)
            return false;
        return true;
    }

    public static CartesianCoord HelioProjectiveCartesianToHelioCentricCartesian(double thetax, double thetay) {
        CartesianCoord result = new CartesianCoord();
        result.x = Constants.SUN_MEAN_DISTANCE_TO_EARTH * Math.cos(thetay / MathUtils.RAD_TO_DEG) * Math.sin(thetax / MathUtils.RAD_TO_DEG) / Constants.SUN_RADIUS;
        result.y = Constants.SUN_MEAN_DISTANCE_TO_EARTH * Math.sin(thetay / MathUtils.RAD_TO_DEG) / Constants.SUN_RADIUS;
        result.z = 1 - Math.cos(thetay / MathUtils.RAD_TO_DEG) * Math.cos(thetax / MathUtils.RAD_TO_DEG);
        return result;
    }

    public static boolean isVisibleCarrington(double theta, double phi, Date date, Date now) {
        // phi is in carrington coordinates
        /*double days = (date.getTime() - 788918400000l) / (1000.0 * 3600.0 * 24.0);
        phi = phi + 349.03 - (360.0 * days / 27.2753);

        while (phi < 0) {
            phi += 360.0;
        }

        phi -= 180;
*/
        // return isVisibleStonyhurst(theta,phi,date,now);

        return true;
    }

    // UTC-HPR-TOPO [Helioprojective];
    public static Vector2d convertHelioprojective(double theta1, double theta2) {
        return new Vector2d(Math.sin(theta1 / MathUtils.RAD_TO_DEG) * 1000, Math.sin(theta2 / MathUtils.RAD_TO_DEG));
    }

    // UTC-HCR-TOPO[Heliocentric radial]
    public static Vector2d fromRadial(double rho, double phi) {
        // umwandeln in
        return new Vector2d(0, 0);
    }

    public static boolean isVisibleHelioprojective(double theta1, double theta2) {
        return false;
    }

    public static boolean isVisibleRadial(double rho, double phi) {
        return false;
    }

    // UTC-HGC-TOPO[Heliographic Carrington];
    public static SphericalCoord CarringtonToStonyhurst(SphericalCoord carrington, Date now) {

        /*
         * CARRINGTON COORDINATE SYSTEM IS A VARIANT OF THE HELIOGRAPHIC SYSTEM
         * WHICH ROTATES AT AN APPROXIMATION OF THE MEAN SOLAR ROTATIONAL RATE,
         * ASE ORIGINALLY USED BY CARRINGTON
         */

        /*
         * OLD mean Carrington Longitude in Degrees OLD = 349.03 - (360.* X /
         * 27.2753), http://umtof.umd.edu/pm/crn/CARRTIME.HTML where X is the
         * number of days since 1 January 1995. It is understood that OLD is to
         * be taken modulo 360. Note that the Carrington longitude decreases as
         * time increases. If one now compares the values of OLD with the values
         * listed in the Almanac one finds reasonable agreement, with maximum
         * discrepancies of about 4 hours.
         * 
         * 
         * Epoch timestamp: 788918400 Human time: Sun, 01 Jan 1995 00:00:00 GMT
         */

        // System.out.println("Phi OLD " + carrington.phi);

        // phi is in carrington coordinates
        double days = (now.getTime() - 788918400000l) / (1000.0 * 3600.0 * 24.0);
        carrington.phi += -349.03 + (360.0 * days / 27.2753);

        carrington.phi = makePhi(carrington.phi);
        return carrington;
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
