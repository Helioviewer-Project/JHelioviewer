package org.helioviewer.base.physics;

public class DifferentialRotation {

    public static double calculateRotationInRadians(double latitude, double timeDifferenceInSeconds) {
        double sin2l = Math.sin(latitude);
        sin2l = sin2l * sin2l;
        double sin4l = sin2l * sin2l;
        return 1.0e-6 * timeDifferenceInSeconds * (2.894 - 0.428 * sin2l - 0.37 * sin4l);
    }

    private static double calculateRotationInDegree(double latitude, double timeDifferenceInSeconds) {
        return calculateRotationInRadians(latitude, timeDifferenceInSeconds) * 180.0 / Math.PI;
    }

    public static StonyhurstHeliographicCoordinates calculateNextPosition(StonyhurstHeliographicCoordinates currentPosition, double timeDifferenceInSeconds) {
        double rotation = calculateRotationInDegree(currentPosition.theta, timeDifferenceInSeconds);
        double newPhi = currentPosition.phi + rotation;
        newPhi = newPhi % 360.0;
        return new StonyhurstHeliographicCoordinates(currentPosition.theta, newPhi, currentPosition.r);
    }
}
