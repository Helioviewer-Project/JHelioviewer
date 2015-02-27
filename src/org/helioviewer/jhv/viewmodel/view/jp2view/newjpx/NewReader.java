package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequest;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;

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

    private ConcurrentLinkedQueue<JPIPRequest> requests;
	private URI uri;

    /**
     * The constructor. Creates and connects the socket if image is remote.
     * 
     * @param _imageViewRef
     * @throws IOException
     * @throws JHV_KduException
     */
    public NewReader(URI uri){
    	requests = new ConcurrentLinkedQueue<JPIPRequest>();
    	this.uri = uri;
    	socket = new JPIPSocket();
    	openSocket();
    	
    	Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true){
					if (requests.isEmpty()){
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					else receiveData();
				}
			}
		});
    }


	private void openSocket(){
		try {
            socket.connect(uri);
        } catch (IOException e) {
            e.printStackTrace();
            
            try {
                socket.close();
            } catch (IOException ioe) {
                System.err.println(">> J2KReader.run() > Error closing socket.");
                ioe.printStackTrace();
            }
            
            if(Thread.currentThread().isInterrupted())
                return;

            if(Thread.currentThread().isInterrupted())
                return;

        }

	}
	
	public void receiveData(){
		if (socket.isClosed()) openSocket();
		try {
			socket.send(requests.poll());
			JPIPResponse response = socket.receive();
			// Update optimal package size
            flowControl();
			if (response != null && response.getResponseSize() > 0){
				
			}
		} catch (IOException e) {
			System.out.println("not correct request");
		}
		
	}
	
    /**
     * This method perfoms the flow control, that is, adjusts dynamically the
     * value of the variable <code>JPIP_REQUEST_LEN</code>. The used algorithm
     * is the same as the used one by the viewer kdu_show of Kakadu (if
     * something works... why not to use it?)
     */
    private void flowControl() {
        int adjust = 0;
        int receivedBytes = socket.getReceivedData();
        long replyTextTime = socket.getReplyTextTime();
        long replyDataTime = socket.getReplyDataTime();

        long tdat = replyDataTime - replyTextTime;

        if (((receivedBytes - JpipRequestLen) < (JpipRequestLen >> 1)) && (receivedBytes > (JpipRequestLen >> 1))) {
            if (tdat > 10000)
                adjust = -1;
            else if (lastResponseTime > 0) {
                long tgap = replyTextTime - lastResponseTime;

                if ((tgap + tdat) < 1000)
                    adjust = +1;
                else {
                    double gapRatio = ((double) tgap) / ((double) (tgap + tdat));
                    double targetRatio = ((double) (tdat + tgap)) / 10000.0;

                    if (gapRatio > targetRatio)
                        adjust = +1;
                    else
                        adjust = -1;
                }
            }
        }

        JpipRequestLen += (JpipRequestLen >> 2) * adjust;

        if (JpipRequestLen > JPIPConstants.MAX_REQUEST_LEN)
            JpipRequestLen = JPIPConstants.MAX_REQUEST_LEN;

        if (JpipRequestLen < JPIPConstants.MIN_REQUEST_LEN)
            JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

        lastResponseTime = replyDataTime;
    }
}
