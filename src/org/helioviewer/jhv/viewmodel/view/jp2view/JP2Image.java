package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import kdu_jni.*;

import org.helioviewer.jhv.base.math.Interval;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.viewmodel.io.APIResponseDump;
import org.helioviewer.jhv.viewmodel.metadata.MultiFrameMetaDataContainer;
import org.helioviewer.jhv.viewmodel.view.cache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class can open JPEG2000 images, yeah baby! Modified to improve the JPIP
 * communication.
 * 
 * @author caplins
 * @author Benjamin Wamsler
 * @author Juan Pablo
 */
public class JP2Image implements MultiFrameMetaDataContainer {

    /** An array of the file extensions this class currently supports */
    public static final String[] SUPPORTED_EXTENSIONS = { ".JP2", ".JPX" };

    private static AtomicInteger numJP2Images = new AtomicInteger();

    /** This is the URI that uniquely identifies the image. */
    private URI uri;

    /** This is the URI from whch the whole file can be downloaded via http */
    private URI downloadURI;

    /**
     * This is the object in which all transmitted data is stored. It has the
     * ability to write itself to disk, and read a relevant cache file from
     * disk.
     */
    private JHV_Kdu_cache cache;

    /**
     * The this extended version of Jp2_threadsafe_family_src can open any file
     * conforming to the jp2 specifications (.jp2, .jpx, .mj2, etc). The reason
     * for extending this class is that the Acquire/Release_lock() functions
     * needed to be implemented.
     */
    private Jp2_threadsafe_family_src familySrc = new Jp2_threadsafe_family_src();

    /** The Jpx_source object is capable of opening jp2 and jpx sources. */
    private Jpx_source jpxSrc = new Jpx_source();

    /**
     * The compositor object takes care of all the rendering via its process
     * function.
     */
    private Kdu_region_compositor compositor = new Kdu_region_compositor();

    /** The range of valid quality layers for the image. */
    private Interval<Integer> qLayerRange;

    /** The range of valid composition layer indices for the image. */
    private Interval<Integer> layerRange;

    /** An object with all the resolution layer information. */
    private ResolutionSet resolutionSet;
    private int resolutionSetCompositionLayer = -1;

    /**
     * This is a little tricky variable to specify that the file contains
     * multiple frames
     */
    private boolean isJpx = false;

    private NodeList[] xmlCache;

    private JHVJP2View parentView;
    private ReentrantLock lock = new ReentrantLock();
    private int referenceCounter = 0;
    private JPIPSocket socket;

    /**
     * The number of output components (should be the number of 8 bits
     * channels). Currently only value of 1 and 3 are supported (corresponding
     * to grayscale and RGB images).
     */
    private int numComponents;

    /**
     * Constructor
     * 
     * <p>
     * To open an image an URI must be given and this should be made unique. All
     * initialization for this object is done in the constructor or in methods
     * called by the constructor. Either the constructor throws an exception or
     * the image was opened successfully.
     * 
     * @param newUri
     *            URI representing the location of the image
     * @throws IOException
     * @throws JHV_KduException
     */
    public JP2Image(URI newUri) throws IOException, JHV_KduException {
        this(newUri, newUri);
    }

    /**
     * Constructor
     * 
     * <p>
     * To open an image an URI must be given and this should be made unique. All
     * initialization for this object is done in the constructor or in methods
     * called by the constructor. Either the constructor throws an exception or
     * the image was opened successfully.
     * 
     * @param newUri
     *            URI representing the location of the image
     * @param downloadURI
     *            In case the file should be downloaded to the local filesystem,
     *            use this URI as the source.
     * @throws IOException
     * @throws JHV_KduException
     */
    public JP2Image(URI newUri, URI downloadURI) throws IOException, JHV_KduException {
        numJP2Images.incrementAndGet();

        uri = newUri;
        this.downloadURI = downloadURI;
        String name = uri.getPath().toUpperCase();
        boolean supported = false;
        for (String ext : SUPPORTED_EXTENSIONS)
            if (name.endsWith(ext))
                supported = true;
        if (!supported)
            throw new JHV_KduException("File extension not supported.");

        isJpx = name.endsWith(".JPX");

        String scheme = uri.getScheme().toUpperCase();
        if (scheme.equals("JPIP"))
            initRemote();
        else if (scheme.equals("FILE"))
            initLocal();
        else
            throw new JHV_KduException(scheme + " scheme not supported!");

        createKakaduMachinery();

        xmlCache = new NodeList[layerRange.end + 1];
    }

    /**
     * Initializes the Jp2_threadsafe_family_src for a remote file. (JPIP comms
     * happen here).
     * 
     * @throws JHV_KduException
     * @throws IOException
     */
    private void initRemote() throws JHV_KduException {
        // Creates the JPIP-socket necessary for communications
        JPIPResponse res;
        socket = new JPIPSocket();

        try {
            // Connects to the JPIP server, stores the first response in the res
            // variable
            res = (JPIPResponse) socket.connect(uri);

            // Parses the first JPIP response for the JPIP target-ID
            String jpipTargetID;

            if (res.getHeader("JPIP-tid") == null)
                throw new JHV_KduException("The target id was not sent by the server");
            else
                jpipTargetID = res.getHeader("JPIP-tid");

            if (jpipTargetID.contains("/")) {
                jpipTargetID = jpipTargetID.substring(jpipTargetID.lastIndexOf("/") + 1);
            }

            // Creates the cache object and adds the first response to it.
            cache = new JHV_Kdu_cache(jpipTargetID,!isJpx);
            cache.addJPIPResponseData(res);

            // Download the necessary initial data if there isn't any cache file
            // yet
            if ((cache.getCacheFile() == null) || !cache.getCacheFile().exists()) {

                boolean initialDataLoaded = false;
                int numTries = 0;

                do {
                    try {
                        KakaduUtils.downloadInitialData(socket, cache);
                        initialDataLoaded = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        numTries++;
                        socket.close();
                        socket = new JPIPSocket();
                        socket.connect(uri);
                    }
                } while (!initialDataLoaded && numTries < 5);
            }

            familySrc.Open(cache);

        } catch (SocketTimeoutException e) {
            throw new JHV_KduException("Timeout while communicating with the server:" + System.getProperty("line.separator") + e.getMessage(), e);
        } catch (IOException e) {
            throw new JHV_KduException("Error in the server communication:" + System.getProperty("line.separator") + e.getMessage(), e);
        } catch (KduException e) {
            throw new JHV_KduException("Kakadu engine error opening the image", e);
        } finally {
            Timer timer = new Timer("WaitForCloseSocket");
            timer.schedule(new TimerTask() {

                public synchronized void run() {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.err.println(">> JP2Image.initRemote() > Error closing socket.");
                            e.printStackTrace();
                        }
                        socket = null;
                    }
                }
            }, 5000);
        }
    }

    /**
     * Initializes the Jp2_threadsafe_family_src for a local file.
     * 
     * @throws JHV_KduException
     * @throws IOException
     */
    private void initLocal() throws JHV_KduException, IOException {

        // Source is local so it must be a file
        File file = new File(uri);

        // Open the family source
        try {
            familySrc.Open(file.getCanonicalPath(), true);
        } catch (KduException ex) {
            throw new JHV_KduException("Failed to open familySrc", ex);
        }
    }

    /**
     * Creates the Kakadu objects and sets all the data-members in this object.
     * 
     * @throws JHV_KduException
     */
    private void createKakaduMachinery() throws JHV_KduException {

        // The amount of cache to allocate to each codestream
        final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;

        try {
            // Open the jpx source from the family source
            jpxSrc.Open(familySrc, false);

            // I don't know if I should be using the codestream in a persistent
            // mode or not...
            compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
            compositor.Set_thread_env(null, null);

            // I create references here so the GC doesn't try to collect the
            // Kdu_dims obj
            Kdu_dims ref1 = new Kdu_dims(), ref2 = new Kdu_dims();

            // A layer must be added to determine the image parameters
            compositor.Add_ilayer(0, ref1, ref2);

            {
                // Retrieve the number of composition layers
                {
                    int[] tempVar = new int[1];
                    jpxSrc.Count_compositing_layers(tempVar);
                    layerRange = new Interval<Integer>(0, tempVar[0] - 1);
                }

                Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_istream(new Kdu_istream_ref(), false, true));

                {
                    Kdu_coords coordRef = new Kdu_coords();
                    Kdu_tile tile = stream.Open_tile(coordRef);

                    // Retrieve the number of quality layers.
                    qLayerRange = new Interval<Integer>(1, tile.Get_num_layers());

                    // Cleanup
                    tile.Close();
                    tile = null;
                }

                // Retrieve the number of components
                {
                    // Since it gets tricky here I am just grabbing a bunch of
                    // values
                    // and taking the max of them. It is acceptable to think
                    // that an
                    // image is color when its not monochromatic, but not the
                    // other way
                    // around... so this is just playing it safe.
                    Kdu_channel_mapping cmap = new Kdu_channel_mapping();
                    cmap.Configure(stream);

                    int maxComponents = MathUtils.max(cmap.Get_num_channels(), cmap.Get_num_colour_channels(), stream.Get_num_components(true), stream.Get_num_components(false));

                    // numComponents = maxComponents == 1 ? 1 : 3;
                    numComponents = maxComponents; // With new file formats we
                    // may have 2 components

                    cmap.Clear();
                    cmap.Native_destroy();
                    cmap = null;
                }

                // Cleanup
                stream = null;
            }

            updateResolutionSet(0);

            // Remove the layer that was added
            compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);

        } catch (KduException ex) {
            ex.printStackTrace();
            throw new JHV_KduException("Failed to create Kakadu machinery: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sets the parent view of this image.
     * 
     * The parent view is used to determine the current frame when accessing
     * meta data.
     * 
     * @param _parentView
     *            The new parent view
     * @see #getParentView()
     * @see #getValueFromXML(String, String)
     */
    public void setParentView(JHVJP2View _parentView) {
        parentView = _parentView;
    }

    /**
     * Returns the parent view of this image.
     * 
     * The parent view is used to determine the current frame when accessing
     * meta data.
     * 
     * @return The current parent view
     * @see #setParentView(JHVJP2View)
     * @see #getValueFromXML(String, String)
     */
    public JHVJP2View getParentView() {
        return parentView;
    }

    /**
     * Returns true if the image is remote or if image is note open.
     * 
     * @return True if the image is remote image, false otherwise
     */
    public boolean isRemote() {
        return cache != null;
    }

    /**
     * Returns whether the image contains multiple frames.
     * 
     * A image consisting of multiple frames is also called a 'movie'.
     * 
     * @return True, if the image contains multiple frames, false otherwise
     */
    public boolean isMultiFrame() {
        int frameCount = getCompositionLayerRange().end - getCompositionLayerRange().start;
        return isJpx && frameCount > 1;
    }

    public Jp2_threadsafe_family_src getFamilySrc() {
        return familySrc;
    }

    /**
     * Method that executes getValueFromXML(_keyword, _box, <currentBoxNumber>).
     * This will get the xml box from the currently shown frame
     * 
     * @param _keyword
     * @param _box
     * @throws JHV_KduException
     */
    public String getValueFromXML(String _keyword, String _box) throws JHV_KduException {
        int boxNumber = 1;
        if (parentView != null && parentView.getImageViewParams() != null) {
            boxNumber = parentView.getImageViewParams().compositionLayer + 1;
        }
        return getValueFromXML(_keyword, _box, boxNumber);
    }

    /**
     * Method that returns value of specified _keyword from specified _box.
     * 
     * @param _keyword
     * @param _box
     * @param _boxNumber
     * @throws JHV_KduException
     */
    public String getValueFromXML(String _keyword, String _box, int _boxNumber) throws JHV_KduException {

        if (xmlCache[_boxNumber - 1] == null) {
            String xml = null;

            lock.lock();
            try {
                xml = KakaduUtils.getXml(familySrc, _boxNumber);
            } catch (JHV_KduException e) {
                throw e;
            } finally {
                lock.unlock();
            }

            if (xml == null) {
                throw new JHV_KduException("No XML data present");
            } else if (!xml.contains("</meta>")) {
                throw new JHV_KduException("XML data incomplete");
            }

            xml = xml.trim().replace("&", "&amp;").replace("$OBS", "");

            InputStream in = null;
            try {
                in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(in);

                xmlCache[_boxNumber - 1] = doc.getElementsByTagName("meta");
            } catch (Exception e) {
                throw new JHV_KduException("Failed parsing XML data", e);
            }
        }

        try {
            NodeList nodes = ((Element) xmlCache[_boxNumber - 1].item(0)).getElementsByTagName(_box);
            NodeList value = ((Element) nodes.item(0)).getElementsByTagName(_keyword);
            Element line = (Element) value.item(0);

            if (line == null)
                return null;

            Node child = line.getFirstChild();
            if (child instanceof CharacterData) {
                CharacterData cd = (CharacterData) child;
                return cd.getData();
            }
            return null;
        } catch (Exception e) {
            throw new JHV_KduException("Failed parsing XML data", e);
        }
    }

    /**
     * Returns the URI representing the location of the image.
     * 
     * @return URI representing the location of the image.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * Returns the download uri the image.
     * 
     * This is the uri from which the whole file can be downloaded and stored
     * locally
     * 
     * @return download uri
     */
    public URI getDownloadURI() {
        return downloadURI;
    }

    /**
     * Returns the socket, if in remote mode.
     * 
     * The socket is returned only one time. After calling this function for the
     * first time, it will always return null.
     * 
     * @return Socket connected to the server
     */
    public JPIPSocket getSocket() {
        if (socket == null)
            return null;

        JPIPSocket output = socket;
        socket = null;
        return output;
    }

    /** Returns the number of output components. */
    public int getNumComponents() {
        return numComponents;
    }

    /** Returns the an interval of the valid composition layer indices. */
    public Interval<Integer> getCompositionLayerRange() {
        return layerRange;
    }

    /** Returns the an interval of the valid quality layer values */
    public Interval<Integer> getQualityLayerRange() {
        return qLayerRange;
    }

    /**
     * Gets the ResolutionSet object that contains the Resolution level
     * information.
     */
    public ResolutionSet getResolutionSet() {
        return resolutionSet;
    }

    /**
     * {@inheritDoc}
     */
    public String get(String key) {
        try {
            String value = getValueFromXML(key, "fits");
            return value;
        } catch (JHV_KduException e) {
            if (e.getMessage() == "XML data incomplete" || e.getMessage().toLowerCase().contains("box not open")) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                }

                return get(key);
            } else if (e.getMessage() != "No XML data present") {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String get(String key, int frameNumber) throws IOException {
        try {
            String value = getValueFromXML(key, "fits", frameNumber + 1);
            return value;
        } catch (JHV_KduException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public double tryGetDouble(String key) {

        String string = get(key);
        if (string != null) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException while trying to parse value \"" + string + "\" of key " + key + " from meta data of\n" + getURI());
                return Double.NaN;
            }
        }
        return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    public int tryGetInt(String key) {

        String string = get(key);
        if (string != null) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException while trying to parse value \"" + string + "\" of key " + key + " from meta data of\n" + getURI());
                return 0;
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(String key) {
        return get(key) != null;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getPixelHeight() {
        return getResolutionSet().getResolutionLevel(0).getResolutionBounds().height;
    }

    /**
     * {@inheritDoc}
     */
    public int getPixelWidth() {

        return getResolutionSet().getResolutionLevel(0).getResolutionBounds().width;
    }

    /**
     * Increases the reference counter.
     * 
     * This counter is used to count all views, which are using this JP2Image as
     * their data source. The counter is decreased when calling
     * {@link #abolish()}.
     */
    public synchronized void addReference() {
        referenceCounter++;
    }

    /**
     * Closes the image out. Destroys all objects and performs cleanup
     * operations. I use the 'abolish' name to distinguish it from what the
     * Kakadu library uses.
     */
    public synchronized void abolish() {
        referenceCounter--;

        if (referenceCounter > 0)
            return;

        if (referenceCounter < 0) {
        	return;
            //throw new IllegalStateException("JP2Image abolished more than once: " + uri);
        }

        numJP2Images.decrementAndGet();

        APIResponseDump.getSingletonInstance().removeResponse(uri);

        try {
            if (compositor != null) {
                compositor.Set_thread_env(null,null);
                compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);
                compositor.Native_destroy();
            }
            if (jpxSrc != null) {
                jpxSrc.Close();
                jpxSrc.Native_destroy();
            }
            if (familySrc != null) {
                familySrc.Close();
                familySrc.Native_destroy();
            }
            if (cache != null) {
                cache.Close();
                cache.Native_destroy();

                JHV_Kdu_cache.updateCacheDirectory();
            }
        } catch (KduException ex) {
            ex.printStackTrace();
        } finally {
            compositor = null;
            jpxSrc = null;
            familySrc = null;
            cache = null;
        }
    }

    boolean updateResolutionSet(int compositionLayerCurrentlyInUse) {
        if (resolutionSetCompositionLayer == compositionLayerCurrentlyInUse)
            return false;

        resolutionSetCompositionLayer = compositionLayerCurrentlyInUse;

        try {
            Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_istream(new Kdu_istream_ref(), false, true));

            int maxDWT = stream.Get_min_dwt_levels();

            compositor.Set_scale(false, false, false, 1.0f);
            Kdu_dims dims = new Kdu_dims();
            if (!compositor.Get_total_composition_dims(dims))
                return false;

            Kdu_coords size = dims.Access_size();
            if (resolutionSet != null && size.Get_x() == getPixelWidth() && size.Get_y() == getPixelHeight())
                return false;

            resolutionSet = new ResolutionSet(maxDWT + 1);
            resolutionSet.addResolutionLevel(0, KakaduUtils.kdu_dimsToRect(dims));

            for (int i = 1; i <= maxDWT; i++) {
                compositor.Set_scale(false, false, false, 1.0f / (1 << i));
                dims = new Kdu_dims();
                if (!compositor.Get_total_composition_dims(dims))
                    break;
                resolutionSet.addResolutionLevel(i, KakaduUtils.kdu_dimsToRect(dims));
            }

        } catch (KduException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Deactivates the internal color lookup table for the given composition
     * layer.
     * 
     * It is not allowed to call this function for a layer, which is not loaded
     * yet.
     * 
     * @param numLayer
     *            composition layer to deactivate internal color lookup for
     */
    void deactivateColorLookupTable(int numLayer) {

        try {
            lock.lock();
            Jpx_codestream_source jpxStream = jpxSrc.Access_codestream(0);
            Jp2_palette palette = jpxStream.Access_palette();

            for (int i = 0; i < palette.Get_num_luts(); i++) {
                jpxSrc.Access_layer(numLayer).Access_channels().Set_colour_mapping(i, 0, -1, numLayer);
            }

        } catch (KduException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    Lock getLock() {
        return lock;
    }

    /** Returns the cache reference */
    public JHV_Kdu_cache getCacheRef() {
        return cache;
    }

    /** Sets the ImageCacheStatus */
    void setImageCacheStatus(ImageCacheStatus imageCacheStatus) {
        if (cache != null)
            cache.setImageCacheStatus(imageCacheStatus);
    }

    /** Returns the compositor reference */
    Kdu_region_compositor getCompositorRef() {
        return compositor;
    }

    /** Returns the jpx source */
    Jpx_source getJpxSource() {
        return jpxSrc;
    }

    /**
     * Returns the number of JP2Image instances currently in use.
     * 
     * @return Number of JP2Image instances currently in use
     */
    static int numJP2ImagesInUse() {
        return numJP2Images.get();
    }
}
