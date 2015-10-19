package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.coordinates.CoordinateHelper;
import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.coordinates.HeliographicCoordinate;
import org.helioviewer.jhv.base.math.SphericalCoord;
import org.helioviewer.jhv.base.math.Triangle;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Astronomy;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.plugins.hekplugin.HEKCoordinateTransform;
import org.helioviewer.jhv.plugins.hekplugin.Interval;
import org.helioviewer.jhv.plugins.hekplugin.IntervalComparison;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The class represents a solar event and manages all the associated
 * information. The current implementation is build around a
 * {@link org.json.JSONObject} object.
 * */
public class HEKEvent implements IntervalComparison<Date> {

    public class GenericTriangle<Coordinate> {

        public Coordinate A;
        public Coordinate B;
        public Coordinate C;

        public GenericTriangle(Coordinate a, Coordinate b, Coordinate c) {
            this.A = a;
            this.B = b;
            this.C = c;
        }

    }
    
    
    
    //Maximum number of points of the outline. If there are more points than this limit
    //the points will be subsampled. (no smoothing)
    private static final int MAX_OUTLINE_POINTS=256;
    
    //After subsampling the path segments are subdivided to make sure that all
    //segments are shorter than this (to account for curvature smoothness)
    private static final double MAX_LINE_SEGMENT_LENGTH=0.04*Constants.SUN_RADIUS;

    /**
     * Flag to indicate if the event is currently being displayed in any event popup
     */
    private boolean showEventInfo = false;
    
    /**
     * Flag to indicate if the cached triangled have already been calculated
     */
    private boolean cacheValid = false;
    
    /**
     * Cache boundary triangulation
     */
    private List<GenericTriangle<SphericalCoord>> cachedTriangles = null;

    /**
     * This field is used as a unique identifier of the event
     */
    private String id = "";

    /**
     * Stores where the event is store in the cache
     */
    private HEKPath path = null;

    /**
     * Stores the duration of the event
     */
    private Interval<Date> duration;

    /**
     * A raw JSONObject to store additional information
     */
    public JSONObject eventObject;

    /**
     * Constructor for an event containing a minimum of information The
     * JSONObject will be empty.
     * 
     * @param id
     *            - id of the new event
     * @param duration
     *            - duration of the event
     */
    public HEKEvent(String id, Interval<Date> duration) {
        this.duration = new Interval<Date>(duration);
        this.id = id;
        eventObject = new JSONObject();
    }

    /**
     * Default constructor. Creates a non valid object, with all member
     * variables ==null.
     */
    public HEKEvent() {
        this.id = null;
        this.setPath(null);
        this.duration = null;
        this.eventObject = null;
    }

    /**
     * Get the id of the event.
     * 
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id of the event.
     * 
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the duration of the event.
     * 
     * @return
     */
    public Interval<Date> getDuration() {
        return duration;
    }

    /**
     * Set the duration of the event.
     * 
     * @param duration
     */
    public void setDuration(Interval<Date> duration) {
        this.duration = duration;
    }

    /**
     * Returns the beginning of the solar event period.
     * 
     * @return time stamp of the beginning of the solar event period.
     */
    public Date getStart() {
        if (this.duration != null) {
            return this.duration.start;
        } else {
            return null;
        }
    }

    /**
     * Returns the end of the solar event period.
     * 
     * @return time stamp of the end of the solar event period.
     */
    public Date getEnd() {
        if (this.duration != null) {
            return this.duration.end;
        } else {
            return null;
        }
    }

    public JSONObject getEventObject() {
        return eventObject;
    }

    public void setEventObject(JSONObject eventObject) {
        this.eventObject = eventObject;
    }

    /**
     * Returns an array which contains all available field names of the solar
     * event.
     * 
     * @return array with all available field names of the solar event.
     */
    public String[] getFields() {
        return getNames();
    }

    /**
     * Returns the value of a property as String.
     * 
     * @param key
     *            read the value from this property.
     * @return value of passed property, null if property does not exist
     */
    public String getString(String key) {
        // name = name.toLowerCase();
        try {
            return eventObject.getString(key);
        } catch (JSONException e) {
            System.err.println("HEKEvent.getString('" + key + "') >> " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns the value of a property as Boolean.
     * 
     * @param key
     *            read the value from this property.
     * @return value of passed property, null if property does not exist
     */
    public boolean getBoolean(String key) {
        // name = name.toLowerCase();
        try {
            return eventObject.getBoolean(key);
        } catch (JSONException e) {
            System.err.println("HEKEvent.getBoolean('" + key + "') >> " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the value of a property as Double.
     * 
     * @param key
     *            read the value from this property.
     * @return value of passed property.
     * @throws SolarEventException
     */
    public double getDouble(String key) throws HEKEventException {
        try {
            return eventObject.getDouble(key);
        } catch (JSONException e) {
            throw new HEKEventException();
        }
    }

    /**
     * Returns all property names in alphabetic order
     * 
     * @return property names.
     */
    private String[] getNames() {
        String[] names = JSONObject.getNames(eventObject);
        java.util.Arrays.sort(names);
        return names;
    }

    /**
     * Do not change. Used as key for hashtables.
     */
    public String toString() {
        return this.id;
    }

    /**
     * Check whether two events are equal
     */
    public boolean equals(Object other) {
        if (other instanceof HEKEvent) {
            HEKEvent sother = (HEKEvent) other;
            return this.id.equals(sother.id);
        }
        return false;
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#contains
     */
    public boolean contains(Interval<Date> other) {
        return this.duration.contains(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#containsFully
     */
    public boolean containsFully(Interval<Date> other) {
        return this.duration.containsFully(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#containsInclusive
     */
    public boolean containsInclusive(Interval<Date> other) {
        return duration.containsInclusive(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#containsPoint
     */
    public boolean containsPoint(Date other) {
        return this.duration.containsPoint(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#containsPointFully
     */
    public boolean containsPointFully(Date other) {
        return this.duration.containsPointFully(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#containsPointInclusive
     */
    public boolean containsPointInclusive(Date other) {
        return this.duration.containsPointInclusive(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#overlaps
     */
    public boolean overlaps(Interval<Date> other) {
        return this.duration.overlaps(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#overlapsInclusive
     */
    public boolean overlapsInclusive(Interval<Date> other) {
        return this.duration.overlapsInclusive(other);
    }

    /**
     * Wrapper method around the event's duration methods.
     * 
     * @see org.helioviewer.jhv.plugins.hekplugin.Interval#compareTo
     */
    public int compareTo(Interval<Date> arg0) {
        return this.duration.compareTo(arg0);
    }

    /**
     * Exception class for exceptions which can occur inside the solar event
     * class. Exceptions which occurred from the internal structure of the solar
     * event class should be mapped to this exception class.
     */
    public static class HEKEventException extends Exception {
    }

    /**
     * Return the event Coordinates in Heliocentric Stonyhurst coordinates. This
     * might need internal coordinate transformations.
     * 
     * @param now
     *            - point in time for which the coordinates are needed (e.g. for
     *            tracking the event)
     * @return - Heliocentric Stonyhurst spherical coordinates
     */
    public SphericalCoord getStony(Date now) {

        // how many seconds is NOW after the point in time that the coordinates
        // are valid for?
        int timeDifferenceInSeconds = (int) ((now.getTime() - this.getStart().getTime()) / 1000);

        SphericalCoord result = new SphericalCoord();
        try {
            result.phi = this.getDouble("hgs_x");
            result.theta = this.getDouble("hgs_y");
            result.r = Constants.SUN_RADIUS;

            // rotate
            return HEKCoordinateTransform.StonyhurstRotateStonyhurst(result, timeDifferenceInSeconds);

        } catch (HEKEventException e) {
        	Telemetry.trackException(e);
        }

        // if nothing worked, just return null
        return null;

    }
    
    public HeliographicCoordinate getHeliographicCoordinate(Date now)
    {
    	int timeDifferenceInSeconds = (int) ((now.getTime() - this.getStart().getTime()) / 1000);

    	double longitute;
		try
		{
			longitute = Math.toRadians(this.getDouble("hgs_x"));
	    	double latitude = Math.toRadians(this.getDouble("hgs_y"));
	        HeliographicCoordinate result = new HeliographicCoordinate(longitute, latitude);
	    	return CoordinateHelper.calculateNextPosition(result, timeDifferenceInSeconds);
		}
		catch (HEKEventException e)
		{
			Telemetry.trackException(e);
		}
		return null;
    }

    /**
     * Check whether the event is on the visible side of the sun Internally
     * requests the events Stonyhurst coordinates and checks if the angle PHI is
     * in the visible range.
     * 
     * @see #getStony
     * @param now
     *            - point in time for which the coordinates are needed (e.g. for
     *            tracking the event)
     * @return
     */
    public boolean isVisible(Date now) {
        SphericalCoord stony = this.getStony(now);
        if (stony == null)
            return true;
        return HEKCoordinateTransform.stonyIsVisible(stony);
    }

    /**
     * Converts Stonyhurst coordinates to screencoordinates
     * 
     * @param stony
     *            - coordinates (stonyhurst) to be converted
     * @param now
     *            - time for which the transformation should be done
     * @return converted screen coordinates, (0.0,0.0) if an error occurs
     */
    public static Vector2d convertToScreenCoordinates(SphericalCoord stony, Date now)
    {
        if (stony == null)
            return new Vector2d(0, 0);

        GregorianCalendar c = new GregorianCalendar();
        c.setTime(now);
        double bzero = Astronomy.getB0InDegree(c);
        double phizero = 0.0; // do we have a value for this?
        HeliocentricCartesianCoordinate result = HEKCoordinateTransform.StonyhurstToHeliocentricCartesian(stony, bzero, phizero);

        return new Vector2d(result.x, -result.y);
    }

    /**
     * Converts Stonyhurst coordinates to 3d scenecoordinates with a normalized
     * radius == 1
     * 
     * @param stony
     *            - coordinates (stonyhurst) to be converted
     * @param now
     *            - time for which the transformation should be done
     * @return converted screen coordinates, (0.0,0.0) if an error occurs
     */
    
    private static Date lastDate;
    private static double lastBZero;
    public static Vector3d convertToSceneCoordinates(SphericalCoord stony, Date now)
    {
        //see http://jgiesen.de/sunrot/index.html
        
        if (stony == null)
            return new Vector3d(0, 0, 0);

        double bzero;
        if(lastDate==null || !now.equals(lastDate))
        {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(now);
            lastBZero = bzero = Astronomy.getB0InDegree(c);
            lastDate=now;
        }
        else
        {
            bzero=lastBZero;
        }
        
        double phizero = 0.0; // do we have a value for this?
        SphericalCoord normalizedStony = new SphericalCoord(stony);
        normalizedStony.r = Constants.SUN_RADIUS;

        HeliocentricCartesianCoordinate result = HEKCoordinateTransform.StonyhurstToHeliocentricCartesian(normalizedStony, bzero, phizero);
        return result.toVector3d();
    }
    

    /**
     * @param path
     *            the path to set
     */
    public void setPath(HEKPath path) {
        this.path = path;
    }

    /**
     * @return the path
     */
    public HEKPath getPath() {
        return path;
    }
    
    /**
     * @return true if the current event is currently being displayed in a popup window
     */
    public boolean getShowEventInfo() {
        return showEventInfo;
    }
    
    /**
     * update status: true if the current event is currently being displayed in a popup window
     */
    public void setShowEventInfo(boolean show) {
        this.showEventInfo = show;
    }
    

    private List<SphericalCoord> toStonyPolyon(String poly, Date now) {

    	List<SphericalCoord> result = new ArrayList<SphericalCoord>();
        poly = poly.trim();

        if (!(poly.startsWith("POLYGON((") && poly.endsWith(")"))) {
            return null;
        }

        poly = poly.substring(9);
        poly = poly.substring(0, poly.length() - 2);

        String[] parts=poly.split("[ ,]");
        for(int i=0;i<parts.length-1;i+=2)
        {
            try
            {
                double firstCoordinate=Double.parseDouble(parts[i]);
                double secondCoordinate=Double.parseDouble(parts[i+1]);
                SphericalCoord stony = new SphericalCoord(secondCoordinate, firstCoordinate, Constants.SUN_RADIUS);
                result.add(stony);
            }
            catch(NumberFormatException _nfe)
            {
                System.err.println("Inconsistent polygon string...");
            }
        }
        
        if(result.size()>MAX_OUTLINE_POINTS)
        {
        	List<SphericalCoord> oldResult = result;
            result = new ArrayList<SphericalCoord>(MAX_OUTLINE_POINTS);
            for(int i=0;i<MAX_OUTLINE_POINTS-1;i++)
                result.add(oldResult.get((int)(i/(double)MAX_OUTLINE_POINTS*(oldResult.size()-1))));
            result.add(result.get(0));
        }

        return result;
    }

    private Date oldStonyBoundDate;
    private List<SphericalCoord> oldStonyBound;
    public List<SphericalCoord> getStonyBound(Date now)
    {
        if(now.equals(oldStonyBoundDate))
            return oldStonyBound;
        
        try {
            if (this.eventObject.has("hgs_boundcc") && !this.eventObject.getString("hgs_boundcc").equals("")) {
                oldStonyBound = this.toStonyPolyon(this.eventObject.getString("hgs_boundcc"), this.getStart());
                oldStonyBoundDate = now;
                
                // duplicate first point at end
                if (!oldStonyBound.get(0).equals(oldStonyBound.get(oldStonyBound.size() - 1))) {
                    oldStonyBound.add(oldStonyBound.get(0));
                }
                
                //td: fix coordinate conversion
                for(int i=0;i<oldStonyBound.size()-1;i++)
                {
                    SphericalCoord a=oldStonyBound.get(i);
                    SphericalCoord b=oldStonyBound.get(i+1);
                    Vector3d va=HEKCoordinateTransform.StonyhurstToHeliocentricCartesian(a,0,0).toVector3d();
                    Vector3d vb=HEKCoordinateTransform.StonyhurstToHeliocentricCartesian(b,0,0).toVector3d();
                    
                    int steps=(int)(va.subtract(vb).length()/MAX_LINE_SEGMENT_LENGTH);
                    if(steps>1)
                    {
                        Vector3d step=vb.subtract(va).scale(1d/steps);
                        for(int j=1;j<steps;j++)
                        {
                            Vector3d interp=va.add(step.scale(j)).normalize().scale(Constants.SUN_RADIUS);
                            oldStonyBound.add(++i,HEKCoordinateTransform.CartesianToStonyhurst(interp));
                        }
                    }
                }
                
                // remove duplicate end-point
                oldStonyBound.remove(oldStonyBound.size()-1);

                return oldStonyBound;
            }
            // uncomment if we would like to draw rectangular bounds, too
            /*
             * else if (this.eventObject.has("hgs_bbox") &&
             * !this.eventObject.getString("hgs_bbox").equals("")) {
             * Log.info("Object has hgs_bbox"); return
             * this.toStonyPolyon(this.eventObject.getString("hgs_bbox"), now);
             * }
             */
        } catch (JSONException e) {
        	Telemetry.trackException(e);
        }

        return null;
    }

    /**
     * Caluclates a triangulation once - for the original position. Then
     * interpolates this triangultion by moving the points
     * 
     * @param now
     * @return
     */
    public List<GenericTriangle<Vector3d>> getTriangulation3D(Date now) {
        if (!cacheValid)
            return null;

        if (cachedTriangles == null)
            return null;

        List<GenericTriangle<Vector3d>> result = new ArrayList<GenericTriangle<Vector3d>>();
        for (GenericTriangle<SphericalCoord> triangle : cachedTriangles)
        {
        	Vector3d A = convertToSceneCoordinates(triangle.A, now).scale(1.005);
        	Vector3d B = convertToSceneCoordinates(triangle.B, now).scale(1.005);
        	Vector3d C = convertToSceneCoordinates(triangle.C, now).scale(1.005);
			
            result.add(new GenericTriangle<Vector3d>(A, B, C));
        }
        return result;
    }

    private void cacheTriangulation() {
        Date now = this.getStart();

        if (now != null) {
            List<SphericalCoord> outerBound = this.getStonyBound(now);

            if (outerBound != null) {
                // if we have less than three points, do nothing
                if (outerBound.size() < 3) {
                    cacheValid = true;
                    return;
                }

                // Setup the Polygon Boundary (External Library)
                Vector2d[] coordinates=new Vector2d[outerBound.size()];
                
                // needed to map back triangles
                List<Vector3d> outerBoundCartesian = new ArrayList<Vector3d>();

                {
                    int i=0;
                    for (SphericalCoord boundaryPoint : outerBound) {
                        Vector3d boundaryPointCartesian = HEKEvent.convertToSceneCoordinates(boundaryPoint, now);
                        outerBoundCartesian.add(boundaryPointCartesian);
                        coordinates[i++]=new Vector2d(boundaryPointCartesian.x/Constants.SUN_RADIUS, boundaryPointCartesian.y/Constants.SUN_RADIUS);
                    }
                }
                
                // add sun border points
                List<SphericalCoord> sunBorder = generateSunBorder();

                // needed to map back triangles
                List<Vector3d> sunBorderCartesian = new ArrayList<Vector3d>();
                for (SphericalCoord sunBoundaryPoint : sunBorder)
                {
                    Vector3d sunBoundaryPointCartesian = HEKEvent.convertToSceneCoordinates(sunBoundaryPoint, now);
                    sunBorderCartesian.add(sunBoundaryPointCartesian);
                }
                
                List<SphericalCoord> lookupSpherical = new ArrayList<SphericalCoord>();
                List<Vector3d> lookupCartesian = new ArrayList<Vector3d>();

                lookupSpherical.addAll(sunBorder);
                lookupCartesian.addAll(sunBorderCartesian);

                lookupSpherical.addAll(outerBound);
                lookupCartesian.addAll(outerBoundCartesian);

                cachedTriangles = new ArrayList<GenericTriangle<SphericalCoord>>();
                for (Triangle triangle : triangulate(coordinates)) {
                    Vector2d A2 = new Vector2d(triangle.x1*Constants.SUN_RADIUS, triangle.y1*Constants.SUN_RADIUS);
                    Vector2d B2 = new Vector2d(triangle.x2*Constants.SUN_RADIUS, triangle.y2*Constants.SUN_RADIUS);
                    Vector2d C2 = new Vector2d(triangle.x3*Constants.SUN_RADIUS, triangle.y3*Constants.SUN_RADIUS);

                    SphericalCoord A4 = findClosest(A2, lookupCartesian, lookupSpherical);
                    SphericalCoord B4 = findClosest(B2, lookupCartesian, lookupSpherical);
                    SphericalCoord C4 = findClosest(C2, lookupCartesian, lookupSpherical);
                    
                    if(!A4.equals(B4) && !A4.equals(C4) && !B4.equals(C4))
                        cachedTriangles.add(new GenericTriangle<SphericalCoord>(A4, C4, B4));
                }
            }

        } else {
            System.out.println("Event has no valid timing information");
        }

        cacheValid = true;

    }

    private List<Triangle> triangulate(Vector2d[] _coordinates)
    {
        List<Triangle> res=new ArrayList<Triangle>();
        if(_coordinates.length<3)
            return res;
        
        
        List<Vector2d> src=new ArrayList<Vector2d>();
        for(Vector2d v:_coordinates)
            src.add(v);
        
        //determine orientation of outline (clockwise/counter-clockwise?)
        //http://en.wikipedia.org/wiki/Shoelace_formula
        {
            double sum=0;
            Vector2d a=src.get(src.size()-1);
            for(Vector2d b:src)
            {
                sum+=(b.x-a.x)*(b.y+a.y);
                a=b;
            }
            if(sum<0)
                Collections.reverse(src);
        }
        
        while(src.size()>3)
        {
            double maxangle=Double.NEGATIVE_INFINITY;
            int maxA=0;
            int maxB=1;
            int maxC=2;
            
            int a=src.size()-2;
            int b=src.size()-1;
            for(int c=0;c<src.size();c++)
            {
                Vector2d ba=src.get(b).subtract(src.get(a));
                Vector2d bc=src.get(b).subtract(src.get(c));
                
                double angle=ba.dot(bc)/ba.length()/bc.length();
                if(angle>maxangle && Vector2d.clockwise(src.get(b),src.get(a),src.get(c))<=0)
                {
                    maxangle=angle;
                    maxA=a;
                    maxB=b;
                    maxC=c;
                }
                
                a=b;
                b=c;
            }
            
            res.add(new Triangle(src.get(maxA),src.get(maxB),src.get(maxC)));
            src.remove(maxB);
        }
        
        res.add(new Triangle(src.get(0),src.get(1),src.get(2)));
        return res;
    }

    private SphericalCoord findClosest(Vector2d toFind, List<Vector3d> lookupCartesian, List<SphericalCoord> lookupSpherical) {
        double closest = Double.POSITIVE_INFINITY;
        int closest_index = -1;
        
        for (int i = 0; i < lookupCartesian.size(); i++) {
            double distance = toFind.subtract(
                    new Vector2d(
                            lookupCartesian.get(i).x,
                            lookupCartesian.get(i).y
                            )
                    ).lengthSq();

            if (distance < closest) {
                closest_index = i;
                closest = distance;
            }
        }
        
        return lookupSpherical.get(closest_index);
    }

    public BufferedImage getIcon(boolean large) {

        boolean human = this.getBoolean("frm_humanflag");
        String type = this.getString("event_type");

        BufferedImage toDraw = HEKConstants.getSingletonInstance().acronymToBufferedImage(type, large);

        if (toDraw == null) {
            toDraw = HEKConstants.getSingletonInstance().acronymToBufferedImage(HEKConstants.ACRONYM_FALLBACK, large);
        }

        // overlay the human icon
        if (human) {
            BufferedImage stackImg = HEKConstants.getSingletonInstance().getOverlayBufferedImage("human", large);
            BufferedImage[] stack = { toDraw, stackImg };
            toDraw = IconBank.stackImages(stack, 1.0, 1.0);
        }

        return toDraw;
    }

    private List<SphericalCoord> generateSunBorder() {
    	List<SphericalCoord> borderPoints = new ArrayList<SphericalCoord>();

        for (double theta = 0.0; theta < 90; theta += 2) {
            borderPoints.add(new SphericalCoord(theta, 89.9, Constants.SUN_RADIUS));
            borderPoints.add(new SphericalCoord(theta, 90.0, Constants.SUN_RADIUS));
            borderPoints.add(new SphericalCoord(theta, -89.9, Constants.SUN_RADIUS));
            borderPoints.add(new SphericalCoord(theta, -90.0, Constants.SUN_RADIUS));
        }

        return borderPoints;
    }

    public void prepareCache() {
        cacheTriangulation();
    }

    /* **************************************************************************
     * 
     * Some legacy code to add additional points in between the loaded polygon
     * points
     * 
     * **************************************************************************
     * 
     * SphericalCoord old = null;
     * 
     * for (SphericalCoord c : bound) { c =
     * HEKCoordinateTransform.getSingletonInstance
     * ().StonyhurstRotateStonyhurst(c, timeDifferenceInSeconds); if (old !=
     * null) { double dphi = c.phi - old.phi; double dtheta = c.theta -
     * old.theta; double dr = c.r - old.r;
     * 
     * dr /= 10.0; dphi /= 10.0; dtheta /= 10.0;
     * 
     * for (int i = 0; i < 10; i++) { double theta = old.theta + dtheta * i;
     * double phi = old.phi + dphi * i; double r = old.r + dr * i;
     * 
     * if (phi > 90) phi = 90; if (phi < -90) phi = -90;
     * 
     * SphericalCoord n = new SphericalCoord(theta, phi, r);
     * interpolated.add(n); }
     * 
     * } else { interpolated.add(c); } old = c; }
     * 
     * interpolated.add(old);
     */

    /* **************************************************************************
     * 
     * Some legacy code that generates additional points on the edge of the
     * visible side of the sun
     * 
     * **************************************************************************
     */

}
