package org.helioviewer.jhv.io;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.base.Telemetry;

//TODO: merge all getDownloadOptionValues-style methods
//TODO: does this still work?

public class CommandLineProcessor
{
	private static String[] arguments;
    public static final String USAGE_MESSAGE="The following command-line options are available: \n"
    		+"\n"
    		+ "-jhv  jhv \"request(s)\"\n"
    		+ "      Allows the user to pass a jhv request. The jhv option can be used multiple times.\n"
    		+ "      A request should be surrounded by quotation marks\n"
    		+ "      There are two sorts of request, one for a single image and one for a image series\n"
    		+ "\n"
    		+ "      The request for a single image has the following form:\n"
    		+ "      [date=yyyy-MM-dd'T'HH:mm:ss'Z';imageScale=KILOMETER_PER_PIXEL;imageLayers=LAYER1,LAYER2,...]\n"
    		+ "\n"
    		+ "      The single layers LAYER1, LAYER2,... must be of the form:\n"
    		+ "      [OBSERVATORY,INSTRUMENT,DETECTOR,MEASUREMENT,VISIBILITY,OPACITY]\n"
    		+ "\n"
    		+ "      The form for an image series is similar:\n"
    		+ "      [startTime=yyy-MM-dd'T'HH:mm:ss'Z';endTime=yyyy-MM-dd'T'HH:mm:ss'Z';linked=LOAD_LAYERS_LINKED;cadence=SECONDS_BETWEEN_IMAGES;imageScale=KILOMETER_PER_PIXEL;imageLayers=LAYER1,LAYER2,...]\n"
    		+ "\n"
    		+ "      Example for retrieving a single image with multiple layers:\n"
    		+ "      -jhv \"[date=2003-10-05T00:00:00Z;imageScale=5000;imageLayers=[SOHO,EIT,EIT,171,1,100],[SOHO,LASCO,C2,white-light,1,100]]\"\n"
    		+ "\n"
    		+ "      Example for retrieving an image sequence with multiple layers:\n"
    		+ "      -jhv \"[startTime=2003-10-05T00:00:00Z;endTime=2003-10-20T00:00:00Z;linked=true;cadence=3600;imageScale=5000;imageLayers=[SOHO,EIT,EIT,171,1,100],[SOHO,LASCO,C2,white-light,1,100]]\"\n"
    		+ "\n"
    		+ "\n"
    		+ "-jpx   JPX_REQUEST_URL\n"
    		+ "       Allows users to pass a jpx request url for a jpx movie which will be opened upon program start. The option can be used multiple times.\n"
    		+ "\n"
    		+ "       Example:\n"
    		+ "       -jpx \"http://helioviewer.nascom.nasa.gov/api/index.php?action=getJPX&observatory=SOHO&instrument=MDI&detector=MDI&measurement=magnetogram&startTime=2003-10-05T00:00:00Z&endTime=2003-10-20T00:00:00Z&cadence=3600&linked=true&jpip=true&frames=true\"\n"
    		+ "\n"
    		+ "\n"
    		+ "-jpip  JPIP_URL\n"
    		+ "       Allows users to pass a jpip url of a JP2 or JPX image to be opened upon program start. The option can be used multiple times.\n"
    		+ "\n"
    		+ "Example:\n"
    		+ "       -jpip \"jpip://delphi.nascom.nasa.gov:8090/test/images/JP2_v20090917/2003_10_05__00_00_10_653__SOHO_EIT_EIT_195.jp2\"\n"
    		+ "\n"
    		+ "\n"
    		+ "-download  URI_TO_FILE \n"
    		+ "       Allows the users to pass the location of JP2 or JPX image, which will be \n"
    		+ "       downloaded to a default location and opened when the program starts. This is especially useful \n"
    		+ "       for the case of large jpx files which can be slow to play remotely.\n"
    		+ "\n"
    		+ "       Example:\n"
    		+ "       -download \"http://delphi.nascom.nasa.gov/jp2/test/images/JP2_v20090917/2003_10_05__00_00_10_653__SOHO_EIT_EIT_195.jp2\"";

    public static void setArguments(String[] args)
    {
        arguments = args;
    }

    /**
     * Returns the values of the -jpx option.
     * 
     * @return list of values of the -jpx option as URLs
     */
    public static List<URL> getJPXOptionValues()
    {
        // get associated value string for "jpip" option
        List<String> jpxURLs = getOptionValues("jpx");
        List<URL> result = new ArrayList<>(jpxURLs.size());
        for (String jpxURL : jpxURLs)
            if (!jpxURL.equals(""))
                try
                {
                    result.add(new URL(jpxURL));
                }
                catch (MalformedURLException e)
                {
                	Telemetry.trackException(e);
                }
            
        return result;
    }

    /**
     * Returns the values of the -jhv option. Each value which was separated by
     * two semicolons in the command line will be returned as an entry in the
     * list. Two semicolons are used to distinguish inner and outer semicolons.
     * 
     * @return values of the -jhv option.
     */
    public static List<CommandLineRequest> getJHVOptionValues()
    {
        List<String> optionValues = getOptionValues("jhv");
        List<CommandLineRequest> result = new ArrayList<>(optionValues.size());

        for (String optionValue : optionValues)
            if (!optionValue.equals(""))
                try
                {
                    result.add(parseJHVRequest(optionValue));
                }
                catch (IllegalArgumentException e)
                {
                    Telemetry.trackException(e);
                }
        
        return result;
    }

    /**
     * Returns the values of the -jpip option
     * 
     * @return list of values of the -jpip option as URIs
     */
    public static List<URI> getJPIPOptionValues()
    {
        List<String> jpipURIs = getOptionValues("jpip");
        List<URI> uris = new ArrayList<>(jpipURIs.size());
        for (String jpipURI : jpipURIs)
            if (!jpipURI.equals(""))
                try
                {
                    uris.add(new URI(jpipURI));
                }
                catch (URISyntaxException e)
                {
                	Telemetry.trackException(e);
                }

        return uris;
    }

    /**
     * Returns the values of the -download option.
     * 
     * @return list of value of the -download option as URIs
     */
    public static List<URI> getDownloadOptionValues()
    {
        List<String> addresses = getOptionValues("download");
        List<URI> uris = new ArrayList<>(addresses.size());
        for (String address : addresses)
            if (!address.equals(""))
                try
                {
                    uris.add(new URI(address));
                }
                catch (URISyntaxException e)
                {
                	Telemetry.trackException(e);
                }
            
        return uris;
    }

    /**
     * Checks whether a specific option is set
     * 
     * @param option
     *            option which to check for
     * @return true if the option was given as a command line argument, false
     *         else
     */
    public static boolean isOptionSet(String option)
    {
        for (String arg : arguments)
            if (arg.equalsIgnoreCase(option))
                return true;
        
        return false;
    }

    /**
     * Method that looks for options in the command line.
     * 
     * @param param
     *            name of the option.
     * @return the values associated to the option.
     * */
    private static List<String> getOptionValues(String param)
    {
        param = "-" + param;
        List<String> values = new ArrayList<>(arguments.length);

        if (arguments != null)
            for (int i = 0; i < arguments.length; i++)
                if (param.equals(arguments[i]))
                    if (arguments.length > (i + 1))
                        values.add(arguments[i + 1]);
        
        return values;
    }

    /**
     * Method that parses JHV data request string. The request can have two
     * forms: a) Single image:
     * [date=2003-10-05T00:00:00Z;imageScale=1914;imageLayers
     * =[SOHO,EIT,EIT,171,1,100],[SOHO,LASCO,C2,white-light,1,100]] The time
     * must have the format yyyy-MM-dd'T'HH:mm:ss'Z' imageScale in
     * kilometer/pixel imageLayers is a comma separated list of layers in the
     * form [observatory,instrument,detector,measurement,visibility,opacity] b)
     * Image sequence
     * [startTime=2003-10-05T00:00:00Z;endTime=2003-10-20T00:00:00
     * Z;linked=true;cadence
     * =3600;imageScale=1914;imageLayers=[SOHO,EIT,EIT,171,
     * 1,100],[SOHO,LASCO,C2,white-light,1,100]] The cadence is the number of
     * seconds between two images.
     * 
     * @param _data
     *            data request from command line.
     * @return values of JHV data request as an array.
     * @throws IllegalArgumentException
     */
	private static CommandLineRequest parseJHVRequest(String _data) throws IllegalArgumentException
    {
        CommandLineRequest request = new CommandLineRequest();
        String[] fields;

        // Request must be in brackets
        if ((_data.charAt(0) != '[') || (_data.charAt(_data.length() - 1) != ']'))
        {
            System.err.println(USAGE_MESSAGE);
            throw new IllegalArgumentException("Brackets are not set properly");
        }

        // Remove brackets
        _data = _data.substring(1, _data.length() - 1);

        // Get the single request fields
        fields = _data.split(";");
        // Sequence has six, single image has three fields
        if ((fields.length != 6) && (fields.length != 3))
        {
            System.err.println(USAGE_MESSAGE);
            throw new IllegalArgumentException("Invalid number of arguments");
        }

        for (String field : fields)
        {
            // Each field can only be set one time
            if (field.startsWith("date="))
            {
                if ((request.startTime != null) || (request.endTime != null))
                {
                    System.err.println(USAGE_MESSAGE);
                    throw new IllegalArgumentException("startTime is already set");
                }
                request.startTime = field.substring(5, field.length());

                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                try
                {
                    dateformat.parse(request.startTime);
                }
                catch (ParseException e)
                {
                    throw new IllegalArgumentException("Invalid time format: " + request.startTime);
                }
                request.endTime = "";
            }
            else if (field.startsWith("startTime="))
            {
                if (request.startTime != null)
                {
                    System.err.println(USAGE_MESSAGE);
                    throw new IllegalArgumentException("startTime is already set");
                }
                request.startTime = field.substring(10, field.length());

                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                try
                {
                    dateformat.parse(request.startTime);
                }
                catch (ParseException e)
                {
                    throw new IllegalArgumentException("Invalid time format: " + request.startTime);
                }
            }
            else if (field.startsWith("endTime="))
            {
                if (request.endTime != null)
                {
                    System.err.println(USAGE_MESSAGE);
                    throw new IllegalArgumentException("endTime is already set");
                }
                request.endTime = field.substring(8, field.length());

                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                try
                {
                    dateformat.parse(request.endTime);
                }
                catch (ParseException e)
                {
                    throw new IllegalArgumentException("Invalid time format: " + request.endTime);
                }
            }
            else if (field.startsWith("cadence="))
            {
                if (request.cadence != null)
                {
                    System.err.println(USAGE_MESSAGE);
                    throw new IllegalArgumentException("cadence is already set");
                }
                request.cadence = field.substring(8, field.length());

                if (Integer.parseInt(request.cadence) <= 0)
                    throw new IllegalArgumentException("cadence must be a positive integer: " + request.cadence);
            }
            else if (field.startsWith("imageScale="))
            {
                if (request.imageScale != -1)
                {
                    System.err.println(USAGE_MESSAGE);
                    throw new IllegalArgumentException("imageScale is already set");
                }
                request.imageScale = Double.parseDouble(field.substring(11, field.length()));
                if (request.imageScale <= 0)
                {
                    throw new IllegalArgumentException("imageScale must be a positive real number: " + request.imageScale);
                }
            }
            else if (field.startsWith("linked="))
            {
                if (request.linked)
                {
                    System.err.println(USAGE_MESSAGE);
                    throw new IllegalArgumentException("linked is already set");
                }
                String linkedString = field.substring(7, field.length());
                if (linkedString.equals("0") || linkedString.equalsIgnoreCase("false"))
                    request.linked = false;
                else if (linkedString.equals("1") || linkedString.equalsIgnoreCase("true"))
                    request.linked = true;
                else
                    throw new IllegalArgumentException("linked must be either 0,1,true or false: " + request.linked);
            }
            else if (field.startsWith("imageLayers="))
            {
                if (request.imageLayers != null)
                {
                    System.err.println(USAGE_MESSAGE);
                    throw new IllegalArgumentException("imageLayers is already set");
                }

                field = field.substring(12);

                // Check for the opening bracket of the first layer and the
                // closing bracket of the last layer
                if ((field.charAt(0) != '[') || (field.charAt(field.length() - 1) != ']'))
                {
                    System.err.println(USAGE_MESSAGE);
                    throw new IllegalArgumentException("Brackets are not set properly");
                }

                // Strip the opening bracket of the first layer and the closing
                // bracket of the last layer
                field = field.substring(1, field.length() - 1);

                // Split layers; it is necessary to us the brackets in the
                // regular expression in order to distinguish inner and outer
                // commas
                String[] layerStrings = field.split("\\],\\[");

                // Each layer has exactly six subfields
                request.imageLayers = new CommandLineLayerRequest[layerStrings.length];

                for (int layerNumber = 0; layerNumber < layerStrings.length; ++layerNumber)
                {
                    // Split the subfields of each layer
                    String[] tokens = layerStrings[layerNumber].split(",");

                    if (tokens.length != CommandLineLayerRequest.numFields)
                    {
                        System.err.println(USAGE_MESSAGE);
                        throw new IllegalArgumentException("Invalid number of fields for layer: " + layerStrings[layerNumber]);
                    }

                    // Copy layer information
                    request.imageLayers[layerNumber] = new CommandLineLayerRequest();
                    request.imageLayers[layerNumber].observatory = tokens[CommandLineLayerRequest.OBSERVATORY_INDEX];
                    request.imageLayers[layerNumber].instrument = tokens[CommandLineLayerRequest.INSTRUMENT_INDEX];
                    request.imageLayers[layerNumber].detector = tokens[CommandLineLayerRequest.DETECTOR_INDEX];
                    request.imageLayers[layerNumber].measurement = tokens[CommandLineLayerRequest.MEASUREMENT_INDEX];
                    request.imageLayers[layerNumber].opacity = Integer.parseInt(tokens[CommandLineLayerRequest.OPACITY_INDEX]);

                    if (request.imageLayers[layerNumber].opacity < 0 || request.imageLayers[layerNumber].opacity > 100)
                        throw new IllegalArgumentException("opacity must be given in percent as an integer between and including 0 and 100: " + request.imageLayers[layerNumber].opacity);
                }
            }
            else
            {
                System.err.println(USAGE_MESSAGE);
                throw new IllegalArgumentException("Unknown field: " + field);
            }
        }

        // Check for all necessary fields
        if ((request.startTime == null) || (request.endTime == null) || (request.imageScale == -1) || (request.imageLayers == null))
        {
            System.err.println(USAGE_MESSAGE);
            throw new IllegalArgumentException("Required fields missing");
        }

        return request;
    }
}
