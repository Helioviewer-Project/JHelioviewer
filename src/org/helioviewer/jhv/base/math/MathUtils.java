package org.helioviewer.jhv.base.math;

/**
 * A collection of useful static methods.
 */
public class MathUtils
{
    public static final double RAD_TO_DEG = 180.0 / Math.PI;

    /**
     * Returns the integer, x, closest on the number line such that
     * min(_side1,_side2) <= x <= max(_side1,_side2).
     * 
     * @param _val
     *            the value to squeee into the interval
     * @param _side1
     *            one side of the interval
     * @param _side2
     *            the other side of the interval
     * @return the closest value within the interval
     */
    public static int clip(int _val, int _side1, int _side2) {
        int temp = Math.max(_side1, _side2);
        _side1 = Math.min(_side1, _side2);
        _side2 = temp;
        if (_val <= _side1)
            return _side1;
        else if (_val >= _side2)
            return _side2;
        else
            return _val;
    }

    /**
     * Takes and returns the maximum value from the given args.
     * 
     * @param _is
     *            the values to compare
     * @return the maximum of the given values
     */
    public static int max(int... _is) {
        int max = Integer.MIN_VALUE;
        for (int i : _is)
            if (max < i)
                max = i;
        return max;
    }

    /**
     * Takes and returns the minimum value from the given args.
     * 
     * @param _is
     *            the values to compare
     * @return the minimum of the given values
     */
    public static int min(int... _is) {
        int min = Integer.MAX_VALUE;
        for (int i : _is)
            if (min > i)
                min = i;
        return min;
    }

    public static double mapTo0To360(double x) {
        double tmp = x % 360.0;
        if (tmp < 0) {
            return tmp + 360.0;
        } else {
            return tmp;
        }
    }
    
    public static int nextPowerOfTwo(int value)
    {
    	value--;
    	value |= value >> 1;
    	value |= value >> 2;
    	value |= value >> 4;
    	value |= value >> 8;
    	value |= value >> 16;
    	value++;
    	return value;
    }
}
