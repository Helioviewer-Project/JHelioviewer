package org.helioviewer.jhv.base.downloadmanager;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.ShutdownManager;
import org.helioviewer.jhv.base.Telemetry;

public class DownloadManager
{
	private static class Tuple
	{
		@Nullable Throwable preparedNotCancelledException;
		WeakReference<AbstractDownloadRequest> request;
		
		Tuple(AbstractDownloadRequest _adr)
		{
			request = new WeakReference<>(_adr);
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

	private static final int CONCURRENT_DOWNLOADS = 4;
	private static AtomicInteger activeDownloadCount = new AtomicInteger();
	private static HashMap<AbstractDownloadRequest, Thread> activeDownloads=new HashMap<>();

	static
	{
		for (int i = 0; i < CONCURRENT_DOWNLOADS; i++)
		{
			Thread thread = new Thread(() ->
				{
					//noinspection InfiniteLoopStatement
					for(;;)
					{
						try
						{
							Tuple t = taskDeque.take();
							AbstractDownloadRequest request = t.request.get();
							if (request != null)
								try
								{
									synchronized(activeDownloads)
									{
										activeDownloads.put(request, Thread.currentThread());
									}
									
									activeDownloadCount.incrementAndGet();
									request.execute();
									request.finished=true;
								}
								catch (Throwable e)
								{
									if(!request.cancelled)
									{
										System.err.println(request.url);
										e.printStackTrace();
									
										if (request.justTriedShouldTryAgain())
											addRequest(request);
										else
											request.setError(e);
									}
								}
								finally
								{
									activeDownloadCount.decrementAndGet();
									
									synchronized(activeDownloads)
									{
										activeDownloads.remove(request);
									}
								}
							else if(t.preparedNotCancelledException!=null)
								Telemetry.trackException(t.preparedNotCancelledException);
						}
						catch (InterruptedException e)
						{
						}
					}
				});
			
			thread.setName("Download thread #" + i);
			thread.setDaemon(true);
			thread.start();
			
			ShutdownManager.addShutdownHook(ShutdownManager.ShutdownPhase.STOP_WORK_1, () -> thread.interrupt());
		}
	}

	public static void addRequest(AbstractDownloadRequest _request)
	{
		if(_request==null)
			throw new IllegalArgumentException("_request==null");
		
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

	/*public static void debug()
	{
		System.out.println("--------------------------------------------");
		synchronized (activeDownloads)
		{
			for(Entry<AbstractDownloadRequest, Thread> r:activeDownloads.entrySet())
				System.out.println("* "+r.getKey().url+"      (interrupted: "+r.getValue().isInterrupted()+")");
			
			for (Tuple td : taskDeque)
				System.out.println(td.request.get().url);
		}
		System.out.println("--------------------------------------------");
	}*/
	
	public static void remove(@Nullable AbstractDownloadRequest request)
	{
		if(request==null)
			return;
		
		synchronized (activeDownloads)
		{
			request.interrupt();
			
			Thread t=activeDownloads.get(request);
			if(t!=null)
				t.interrupt();
			
			for (Tuple td : taskDeque)
				if(td.request.get()==request)
					if(taskDeque.remove(td))
						return;
		}
	}

	public synchronized static boolean areDownloadsActive()
	{
		return activeDownloadCount.get()>0 || !taskDeque.isEmpty();
	}
}
