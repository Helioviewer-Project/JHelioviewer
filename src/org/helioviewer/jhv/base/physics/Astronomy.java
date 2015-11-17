package org.helioviewer.jhv.base.physics;

import java.util.Calendar;

import org.helioviewer.jhv.base.math.MathUtils;

public class Astronomy {
    
    private static double calendarToJulianDay(Calendar _d)
    {
        int mo = _d.get(Calendar.MONTH) + 1;
        int da = _d.get(Calendar.DAY_OF_MONTH);

        int yr = _d.get(Calendar.YEAR);
        int A;
        int B;
        int C;
        int D;

        if (mo <= 2)
        {
            yr--;
            mo += 12;
        }

        if ((_d.get(Calendar.YEAR) > 1582) || ((_d.get(Calendar.YEAR) == 1582) && (_d.get(Calendar.MONTH) >= 10) && (da >= 15))) {
            A = (int)(yr / 100.0);
            B = 2 - A + (int)(A / 4.0);
        } else {
            B = 0;
        }

        double c1 = 365.25 * yr;
        if (yr < 0) {
            c1 = 365.25 * yr - 0.75;
        }

        C = (int)c1;
        double d1 = 30.6001 * (mo + 1);
        D = (int)d1;

        return B + C + D + da
                + _d.get(Calendar.HOUR) / 24.0
                + _d.get(Calendar.MINUTE) / 1440.0
                + _d.get(Calendar.SECOND) / 86400.0
                + 1720994.5;
    }

    // This method is based on the SolarSoft GET_SUN routine
    public static double getB0InRadians(Calendar time)
    {

        double t = (calendarToJulianDay(time) - 2415020) / 36525;

        double mnl = 279.69668 + 36000.76892 * t + 0.0003025 * t * t;
        mnl = MathUtils.mapTo0To360(mnl);

        double mna = 358.47583 + 35999.04975 * t - 0.000150 * t * t - 0.0000033 * t * t * t;
        mna = MathUtils.mapTo0To360(mna);

        double c = (1.919460 - 0.004789 * t - 0.000014 * t * t) * Math.sin(mna / MathUtils.RAD_TO_DEG) + (0.020094 - 0.000100 * t) * Math.sin(2 * mna / MathUtils.RAD_TO_DEG) + 0.000293 * Math.sin(3 * mna / MathUtils.RAD_TO_DEG);

        double true_long = MathUtils.mapTo0To360(mnl + c);

        double k = 74.3646 + 1.395833 * t;

        double lamda = true_long - 0.00569;

        double diff = (lamda - k) / MathUtils.RAD_TO_DEG;

        // do we want to change this to 7.33?
        double i = 7.25;

        return Math.asin(Math.sin(diff) * Math.sin(i / MathUtils.RAD_TO_DEG));
    }

    public static double getB0InDegree(Calendar time) {
        return getB0InRadians(time) * MathUtils.RAD_TO_DEG;
    }

}
