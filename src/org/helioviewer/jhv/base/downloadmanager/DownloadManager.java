package org.helioviewer.jhv.base.downloadmanager;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;

public class DownloadManager
{
	private static class Tuple
	{
		@Nullable Throwable preparedNotCancelledException;
		WeakReference<AbstractDownloadRequest> request;
		
		Tuple(AbstractDownloadRequest _adr)
		{
			request= new WeakReference<>(_adr);
		}
	}
	
	
	private static PriorityBlockingQueue<Tuple> taskDeque = new PriorityBlockingQueue<>(100, new Comparator<Tuple>()
	{
		@Override
		public int compare(@Nullable Tuple o1, @Nullable Tuple o2)
		{
			if (o1 == null || o2 == null)
				return 0;

			AbstractDownloadRequest oo1 = o1.request.get();
			AbstractDownloadRequest oo2 = o2.request.get();
			if (oo1 == null || oo2 == null)
				return 0;

			return oo2.priority.ordinal() - oo1.priority.ordinal();
		}
	});

	private static final int CONCURRENT_DOWNLOADS = 2;
	private static AtomicInteger activeDownloads = new AtomicInteger();

	static
	{
		for (int i = 0; i < CONCURRENT_DOWNLOADS; i++)
		{
			Thread thread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						//noinspection InfiniteLoopStatement
						for(;;)
						{
							Tuple t = taskDeque.take();
							AbstractDownloadRequest request = t.request.get();
							if (request != null)
								try
								{
									//if(request.priority.ordinal()>DownloadPriority.LOW.ordinal())
										activeDownloads.incrementAndGet();
									request.execute();
								}
								catch(InterruptedException e)
								{
									throw e;
								}
								catch (Throwable e)
								{
									System.err.println(request.url);
									Telemetry.trackException(e);
									if (request.justTriedShouldTryAgain())
										addRequest(request);
									else
										request.setError(e);
								}
								finally
								{
									//if(request.priority.ordinal()>DownloadPriority.LOW.ordinal())
										activeDownloads.decrementAndGet();
								}
							else if(t.preparedNotCancelledException!=null)
								Telemetry.trackException(t.preparedNotCancelledException);
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

	public static void addRequest(AbstractDownloadRequest _request)
	{
		Tuple t=new Tuple(_request);
		
		try
		{
			throw new RuntimeException("Request for was not canceled properly: "+_request.url);
		}
		catch(RuntimeException _t)
		{
			t.preparedNotCancelledException=_t;
		}
		
		taskDeque.put(t);
	}

	public static void remove(@Nullable AbstractDownloadRequest request)
	{
		for (Tuple t : taskDeque)
			if(t.request.get()==request)
			{
				taskDeque.remove(t);
				return;
			}
	}

	public synchronized static boolean areDownloadsActive()
	{
		return activeDownloads.get()>0 || !taskDeque.isEmpty();
	}
}
