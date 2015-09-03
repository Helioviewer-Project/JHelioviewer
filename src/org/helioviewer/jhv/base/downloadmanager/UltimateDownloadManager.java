package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.helioviewer.jhv.JHVGlobals;

public class UltimateDownloadManager
{
	private static PriorityBlockingQueue<WeakReference<AbstractDownloadRequest>> taskDeque = null;

	private static final int NUMBER_OF_THREADS = 6;

	static
	{
		taskDeque = new PriorityBlockingQueue<WeakReference<AbstractDownloadRequest>>(100, new Comparator<WeakReference<AbstractDownloadRequest>>()
				{
					@Override
					public int compare(WeakReference<AbstractDownloadRequest> o1, WeakReference<AbstractDownloadRequest> o2)
					{
						AbstractDownloadRequest oo1 = o1.get();
						AbstractDownloadRequest oo2 = o2.get();
						if (oo1 == null || oo2 == null)
							return 0;
						
						return oo1.getPriority().ordinal() - oo2.getPriority().ordinal();
					}
				});

		for (int i = 0; i < NUMBER_OF_THREADS; i++)
		{
			UltimateDownloadManager.DownloadThread thread = new UltimateDownloadManager.DownloadThread();
			thread.setName("Download thread-" + i);
			thread.setDaemon(true);
			thread.start();
		}

	}

	public static void addRequest(AbstractDownloadRequest request)
	{
		WeakReference<AbstractDownloadRequest> weakRequest = new WeakReference<AbstractDownloadRequest>(request);
		taskDeque.put(weakRequest);
	}

	public static void remove(AbstractDownloadRequest request)
	{
		taskDeque.remove(request);
	}

	private static class DownloadThread extends Thread
	{
		public DownloadThread()
		{
		}

		@Override
		public void run()
		{
			try
			{
				for(;;)
				{
					AbstractDownloadRequest request = taskDeque.take().get();
					if (request != null)
						try
						{
							request.execute();
						}
						catch (IOException e)
						{
							request.justRetried();
							if (request.shouldRetry())
								addRequest(request);
							else
								request.addError(e);
						}
					else if(!JHVGlobals.isReleaseVersion())
						throw new RuntimeException("Request was not canceled");
				}
			}
			catch (InterruptedException e)
			{
			}
		}

		public void doStop()
		{
			this.interrupt();
		}
	}
	
	public static boolean areDownloadsActive()
	{
		for (WeakReference<AbstractDownloadRequest> request : taskDeque)
			if (request.get() != null && request.get().getPriority().ordinal() < DownloadPriority.LOW.ordinal())
				return true;
		
		return false;
	}
}
