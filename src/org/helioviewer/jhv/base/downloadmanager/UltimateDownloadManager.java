package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.helioviewer.jhv.JHVGlobals;

public class UltimateDownloadManager
{
	private static PriorityBlockingQueue<WeakReference<AbstractDownloadRequest>> taskDeque = new PriorityBlockingQueue<WeakReference<AbstractDownloadRequest>>(100, new Comparator<WeakReference<AbstractDownloadRequest>>()
	{
		@Override
		public int compare(WeakReference<AbstractDownloadRequest> o1, WeakReference<AbstractDownloadRequest> o2)
		{
			AbstractDownloadRequest oo1 = o1.get();
			AbstractDownloadRequest oo2 = o2.get();
			if (oo1 == null || oo2 == null)
				return 0;
			
			return oo2.priority.ordinal() - oo1.priority.ordinal();
		}
	});

	private static final int NUMBER_OF_THREADS = 6;
	private static AtomicInteger activeNormalAndHighPrioDownloads = new AtomicInteger();

	static
	{
		for (int i = 0; i < NUMBER_OF_THREADS; i++)
		{
			Thread thread = new Thread(new Runnable()
			{
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
									if(request.priority.ordinal()>DownloadPriority.LOW.ordinal())
										activeNormalAndHighPrioDownloads.incrementAndGet();
									request.execute();
								}
								catch (IOException e)
								{
									System.err.println(request.url);
									e.printStackTrace();
									if (request.justTriedShouldTryAgain())
										addRequest(request);
									else
										request.setError(e);
								}
								finally
								{
									if(request.priority.ordinal()>DownloadPriority.LOW.ordinal())
										activeNormalAndHighPrioDownloads.decrementAndGet();
								}
							else if(!JHVGlobals.isReleaseVersion())
								throw new RuntimeException("Request was not canceled");
						}
					}
					catch (InterruptedException e)
					{
					}
				}
			});
			thread.setName("Download thread #" + i);
			thread.setDaemon(true);
			thread.start();
		}
	}

	public static void addRequest(AbstractDownloadRequest request)
	{
		taskDeque.put(new WeakReference<>(request));
	}

	public static void remove(AbstractDownloadRequest request)
	{
		for (WeakReference<AbstractDownloadRequest> r : taskDeque)
			if(r.get()==request)
			{
				taskDeque.remove(r);
				return;
			}
	}

	public synchronized static boolean areDownloadsActive()
	{
		return activeNormalAndHighPrioDownloads.get()>0;
	}
}
