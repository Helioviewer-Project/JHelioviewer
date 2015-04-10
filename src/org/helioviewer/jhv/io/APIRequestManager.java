package org.helioviewer.jhv.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.viewmodel.io.APIResponse;
import org.helioviewer.jhv.viewmodel.io.APIResponseDump;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.ImageInfoView;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJP2View;

/**
 * This class provides methods to download files from a server.
 * 
 * Most of the methods only will work with the current Helioviewer server
 * because they modify links and requests that they will fit with the API.
 * 
 * @author Stephan Pagel
 * @author Andre Dau
 * @author Helge Dietert
 */
public class APIRequestManager {
    /**
     * Returns the date of the latest image available from the server
     * 
     * @param observatory
     *            observatory of the requested image.
     * @param instrument
     *            instrument of the requested image.
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @return time stamp of the latest available image on the server
     * @throws IOException
     * @throws MalformedURLException
     */
    public static Date getLatestImageDate(String observatory, String instrument, String detector, String measurement) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date date = new Date();
        boolean readDate = false;
        ImageInfoView view = null;
        
        try {
            view = loadImage(false, observatory, instrument, detector, measurement, formatter.format(date));
            if (view != null) {
                MetaData metaData = view.getAdapter(MetaDataView.class).getMetaData();
                date = metaData.getDateTime().getTime();
                readDate = true;
                if (view instanceof JHVJP2View) {
                    ((JHVJP2View) view).abolish();
                }
            } else {
                System.err.println(">> APIRequestManager.getLatestImageDate() > Could not load latest image. Use current date as initial end date.");
                new Exception().printStackTrace();
            }
        } catch (MalformedURLException e) {
            System.err.println(">> APIRequestManager.getLatestImageDate() > Malformed jpip request url. Use current date as initial end date.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(">> APIRequestManager.getLatestImageDate() > Error while opening stream. Use current date as initial end date.");
            e.printStackTrace();
        }

        if (readDate) {
            return date;
        } else {
            return new Date(System.currentTimeMillis() - 48 * 60 * 60 * 1000);
        }
    }

    /**
     * Sends an request to the server to compute where the nearest image is
     * located on the server. The address of the file will be returned.
     * 
     * @param addToViewChain
     *            specifies whether the generated ImageInfoView should be added
     *            to the view chain of the main image
     * @param observatory
     *            observatory of the requested image.
     * @param instrument
     *            instrument of the requested image.
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @param startTime
     *            time if the requested image.
     * @return image info view of the nearest image file on the server.
     * @throws MalformedURLException
     * @throws IOException
     */
    private static ImageInfoView loadImage(boolean addToViewChain, String observatory, String instrument, String detector, String measurement, String startTime) throws MalformedURLException, IOException {
        String fileRequest = Settings.getProperty("API.jp2images.path") + "?action=getJP2Image&observatory=" + observatory + "&instrument=" + instrument + "&detector=" + detector + "&measurement=" + measurement + "&date=" + startTime + "&json=true";
        String jpipRequest = fileRequest + "&jpip=true";

        // get URL from server where file with image series is located
        try {
            return requestData(addToViewChain, new URL(jpipRequest), new URI(fileRequest));
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                System.out.println(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String)  > Error will be throw");
                e.printStackTrace();
                throw new IOException("Unknown Host: " + e.getMessage());
            } else {
                System.out.println(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String)  > Error will be throw");
                e.printStackTrace();
                throw new IOException("Error in the server communication:" + e.getMessage());
            }
        } catch (URISyntaxException e) {
            System.err.println("Error creating jpip request");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sends an request to the server to compute where the image series is
     * located on the server. The address of the file will be returned.
     * 
     * @param addToViewChain
     *            specifies whether the generated ImageInfoView should be added
     *            to the view chain of the main image
     * @param observatory
     *            observatory of the requested image series.
     * @param instrument
     *            instrument of the requested image series.
     * @param detector
     *            detector of the requested image series.
     * @param measurement
     *            measurement of the requested image series.
     * @param startTime
     *            start time of the requested image series.
     * @param endTime
     *            end time of the requested image series.
     * @param cadence
     *            cadence between to images of the image series.
     * @return image info view of the file which represents the image series on
     *         the server.
     * @throws MalformedURLException
     * @throws IOException
     */
    private static ImageInfoView loadImageSeries(boolean addToViewChain, String observatory, String instrument, String detector, String measurement, String startTime, String endTime, String cadence) throws MalformedURLException, IOException {
        String fileRequest = Settings.getProperty("API.jp2series.path") + "?action=getJPX&observatory=" + observatory + "&instrument=" + instrument + "&detector=" + detector + "&measurement=" + measurement + "&startTime=" + startTime + "&endTime=" + endTime;
        if (cadence != null) {
            fileRequest += "&cadence=" + cadence;
        }

        String jpipRequest = fileRequest + "&jpip=true&verbose=true&linked=true";

        System.out.println(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String) > jpip request url: " + jpipRequest);
        System.out.println(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String) > http request url: " + fileRequest);

        // get URL from server where file with image series is located
        try {
            return requestData(addToViewChain, new URL(jpipRequest), new URI(fileRequest));
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                System.out.println(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String)  > Error will be throw");
                e.printStackTrace();
                throw new IOException("Unknown Host: " + e.getMessage());
            } else {
                System.out.println(">> APIRequestManager.loadImageSeries(boolean,String,String,String,String,String,String,String)  > Error will be throw");
                e.printStackTrace();
                throw new IOException("Error in the server communication:" + e.getMessage());
            }
        } catch (URISyntaxException e) {
            System.err.println("Error creating jpip request");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Sends an request to the server to compute where the image series is
     * located on the server together with meta information like timestamps for
     * the frames.
     * <p>
     * After processing the request it will if the server gives a sufficient
     * reply, i.e. "uri" is set it will try to load the result with
     * {@link #newLoad(URI, URI, boolean)}. It will display and log any further
     * message from the server.
     * <p>
     * Returns the corresponding ImageInfoView for the file.
     * 
     * @param addToViewChain
     *            specifies whether the generated ImageInfoView should be added
     *            to the view chain of the main image
     * @param jpipRequest
     *            The http request url which is sent to the server
     * @param downloadUri
     *            the http uri from which the whole file can be downloaded
     * @return The ImageInfoView corresponding to the file whose location was
     *         returned by the server
     */
    public static ImageInfoView requestData(boolean addToViewChain, URL jpipRequest, URI downloadUri) throws IOException {
        try {
            DownloadStream ds = new DownloadStream(jpipRequest, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
            APIResponse response = new APIResponse(new BufferedReader(new InputStreamReader(ds.getInput())));

            // Could we handle the answer from the server
            if (!response.hasData()) {
                System.err.println("Could not understand server answer from " + jpipRequest);
                Message.err("Invalid Server reply", "The server data could not be parsed.", false);
                return null;
            }
            // Just some error from the server
            String error = response.getString("error");
            if (error != null) {
                System.err.println("Data query returned error: " + error);
                Message.err("The server returned the following error message: \n", Message.formatMessageString(error), false);
                return null;
            }

            // Try to load
            if (response.getURI() != null) {
                // The server wants to load us the data
                String message = response.getString("message");
                if (message != null && !message.equalsIgnoreCase("null")) {
                    Message.warn("Warning", Message.formatMessageString(message));
                }
                APIResponseDump.getSingletonInstance().putResponse(response);
                return newLoad(response.getURI(), downloadUri, addToViewChain);
            } else {
                // We did not get a reply to load data or no reply at all
                String message = response.getString("message");
                if (message != null && !message.equalsIgnoreCase("null")) {
                    System.err.println("No data to load returned from " + jpipRequest);
                    System.err.println("Server message: " + message);
                    Message.err("Server could not return data", Message.formatMessageString(message), false);
                } else {
                    System.err.println("Did not find uri in reponse to " + jpipRequest);
                    Message.err("No data source response", "While quering the data source, the server did not provide an answer.", false);
                }
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Socket timeout while requesting jpip url");
            e.printStackTrace();
            Message.err("Socket timeout", "Socket timeout while requesting jpip url", false);
        }
        return null;
    }

    /**
     * Loads the image or image series from the given URI, creates a new image
     * info view and adds it as a new layer to the view chain of the main image.
     * 
     * @param uri
     *            specifies the location of the file.
     * @param addToViewChain
     *            specifies whether the generated ImageInfoView should be added
     *            to the view chain of the main image
     * @return associated image info view of the given image or image series
     *         file.
     * @throws IOException
     */
    public static ImageInfoView newLoad(URI uri, boolean addToViewChain) throws IOException {
        if (uri == null) {
            return null;
        }

        // Load new view and assign it to view chain of Main Image

        ImageInfoView view = ViewHelper.loadView(uri);
        if (addToViewChain) {
            GuiState3DWCS.addLayerToViewchainMain(view, GuiState3DWCS.mainComponentView);
        }
        return view;
    }

    /**
     * Loads the image or image series from the given URI, creates a new image
     * info view and adds it as a new layer to the view chain of the main image.
     * 
     * @param uri
     *            specifies the location of the file.
     * @param downloadURI
     *            the http uri from which the whole file can be downloaded
     * @param addToViewChain
     *            specifies whether the generated ImageInfoView should be added
     *            to the view chain of the main image
     * @return associated image info view of the given image or image series
     *         file.
     * @throws IOException
     */
    public static ImageInfoView newLoad(URI uri, URI downloadURI, boolean addToViewChain) throws IOException {
        if (uri == null) {
            return null;
        }

        // Load new view and assign it to view chain of Main Image
        ImageInfoView view = ViewHelper.loadView(uri, downloadURI);

        if (addToViewChain) {
          GuiState3DWCS.addLayerToViewchainMain(view, GuiState3DWCS.mainComponentView);
        }
        return view;
    }

    /**
     * Method does remote opening. If image series, file is downloaded. If
     * single frame, file is opened via JPIP on delphi.nascom.nasa.gov:8090.
     * 
     * @param cadence
     *            cadence between two frames (null for single images).
     * @param startTime
     *            start time of the requested image
     * @param endTime
     *            end time of the requested image (empty for single images).
     * @param observatory
     *            observatory of the requested image
     * @param instrument
     *            instrument of the requested image
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @return new view
     * @throws IOException
     */
    public static ImageInfoView requestAndOpenRemoteFile(String cadence, String startTime, String endTime, String observatory, String instrument, String detector, String measurement) throws IOException {
        if (endTime.equals("")) {
            return loadImage(true, observatory, instrument, detector, measurement, startTime);
        } else {
            return loadImageSeries(true, observatory, instrument, detector, measurement, startTime, endTime, cadence);
        }
    }
}
