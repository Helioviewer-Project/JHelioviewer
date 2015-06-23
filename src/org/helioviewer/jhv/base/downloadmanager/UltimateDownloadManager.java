package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.Cache;

public class UltimateDownloadManager {

	private static final Comparator<WeakReference<AbstractRequest>> COMPARATOR = new Comparator<WeakReference<AbstractRequest>>() {
		@Override
		public int compare(WeakReference<AbstractRequest> o1,
				WeakReference<AbstractRequest> o2) {
			AbstractRequest oo1 = o1.get();
			AbstractRequest oo2 = o2.get();
			if (oo1 == null || oo2 == null) return 0;
			if (oo1.getPriority() == oo2.getPriority())
				return 0;
			return oo2.getPriority().ordinal() < oo1.getPriority().ordinal() ? 1
					: -1;
		}
	};

	private static PriorityBlockingQueue<WeakReference<AbstractRequest>> taskDeque = null;

	private static final int NUMBER_OF_THREAD = 6;

	static {
		taskDeque = new PriorityBlockingQueue<WeakReference<AbstractRequest>>(
				1000, COMPARATOR);

		for (int i = 0; i < NUMBER_OF_THREAD; i++) {
			UltimateDownloadManager.DownloadThread thread = new UltimateDownloadManager.DownloadThread();
			thread.setName("Download-Thread-" + i);
			thread.start();
		}

	}

	public static void addRequest(AbstractRequest request) {
		WeakReference<AbstractRequest> weakRequest = new WeakReference<AbstractRequest>(
				request);
		// taskDeque.offer(request);
		taskDeque.put(weakRequest);
	}

	public static void remove(AbstractRequest request) {
		taskDeque.remove(request);
	}

	private static class DownloadThread extends Thread {

		private boolean stopped = false;

		public DownloadThread() {
		}

		@Override
		public void run() {
			while (!isStopped()) {
				try {
					AbstractRequest request = taskDeque.take().get();
					if (request != null)
					try {
						request.execute();
					} catch (IOException e) {
						if (request.hasRetry()) {
							System.out.println("couldn't connect to : " + request.url);
							addRequest(request);
						}
						else{
							System.out.println("couldn't connect to : " + request.url);
							request.addError(e);
						}
					}
					else if(!JHVGlobals.isReleaseVersion()){
						throw new RuntimeException("Request is not canceled");
					}
					if (Cache.checkSize()){
						switchJpipDownloadRequest();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public synchronized void doStop() {
			stopped = true;
			this.interrupt();
		}

		private boolean isStopped() {
			return stopped;
		}

	}
	
	private static void switchJpipDownloadRequest(){
		for (WeakReference<AbstractRequest> weakRequest : taskDeque){
			AbstractRequest request = weakRequest.get();
			if (request != null && request.getPriority() == PRIORITY.LOW){
				request.setPriority(PRIORITY.URGENT);
			}
		}
	}

	public static boolean checkLoading() {
		for (WeakReference<AbstractRequest> request : taskDeque){
			if (request.get().getPriority().ordinal() < PRIORITY.LOW.ordinal()){
				return true;
			}
		}
		return false;
	}
}
