package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.io.IOException;

import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

public class NewReader {

    /** Whether IOExceptions should be shown on System.err or not */
    private static final boolean VERBOSE = false;

    /** The thread that this object runs on. */
    private volatile Thread myThread;

    /** A boolean flag used for stopping the thread. */
    private volatile boolean stop;

    /** A reference to the JP2Image this object is owned by. */
    private JP2Image jp2Image;

    /** The JPIPSocket used to connect to the server. */
    private JPIPSocket socket;

    /** The a reference to the cache object used by the run method. */
    private JHV_Kdu_cache cacheRef;

    /**
     * The time when the last response was received. It is used for performing
     * the flow control. A negative value means that there is not a previous
     * valid response to take into account.
     */
    private volatile long lastResponseTime = -1;

    /** The current length in bytes to use for requests */
    private volatile int JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

    /**
     * The constructor. Creates and connects the socket if image is remote.
     * 
     * @param _imageViewRef
     * @throws IOException
     * @throws JHV_KduException
     */
    NewReader(JP2Image jp2Image){
    	this.jp2Image = jp2Image;

    	cacheRef = jp2Image.getCacheRef();

        // Attempts to connect socket if image is remote.
        if (jp2Image.isRemote()) {

            socket = jp2Image.getSocket();

				try {
		            if (socket == null) {
		                socket = new JPIPSocket();
		                JPIPResponse res;
					res = (JPIPResponse) socket.connect(jp2Image.getURI());
	                cacheRef.addJPIPResponseData(res);
		            }
		            // Somehow it seems we need to update the server cache model for
		            // movies too
		            // otherwise there can be a weird bug where the meta data seems
		            // missing
		            // if (!parentImageRef.isMultiFrame())
		            KakaduUtils.updateServerCacheModel(socket, cacheRef, true);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JHV_KduException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            


        } else {
            socket = null;
        }

        myThread = null;
        stop = false;
    }

}
