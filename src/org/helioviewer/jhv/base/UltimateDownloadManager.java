package org.helioviewer.jhv.base;

import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UltimateDownloadManager {
	
	public enum PRIORITY{
		HIGH, MEDIUM, LOW, TIMEDEPEND;
	}
	
	
	
	private static final Comparator<DownloadTask> COMPARATOR = new Comparator<DownloadTask>() {
		@Override
		public int compare(DownloadTask o1, DownloadTask o2) {
			return o2.getPriority() < o1.getPriority() ? 1 : -1;
		}
	};
	
	public static void main(String[] args) {
		new UltimateDownloadManager();
	}
	
	public class DownloadTask implements Callable<Integer>{
		private final PRIORITY priority;
		public String name;
		
		public DownloadTask(PRIORITY priority, String name) {
			this.priority = priority;
			this.name = name;
		}
		
		@Override
		public Integer call() throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	
		public int getPriority(){
			return priority.ordinal();
		}
		
	}
	
	private PriorityBlockingQueue<DownloadTask> taskDeque = null;
	
	private static final int NUMBER_OF_THREAD = 1;
	
	//public static final UltimateDownloadManager SINGLETON = new UltimateDownloadManager();
	
	
	private UltimateDownloadManager() {
		taskDeque = new PriorityBlockingQueue<DownloadTask>(1000, COMPARATOR);
		taskDeque.offer(new DownloadTask(PRIORITY.HIGH, "h1"));
		taskDeque.offer(new DownloadTask(PRIORITY.LOW, "l1"));
		taskDeque.offer(new DownloadTask(PRIORITY.LOW, "l2"));
		taskDeque.offer(new DownloadTask(PRIORITY.MEDIUM, "m1"));
		taskDeque.offer(new DownloadTask(PRIORITY.HIGH, "h2"));
		taskDeque.offer(new DownloadTask(PRIORITY.MEDIUM, "m2"));
		taskDeque.offer(new DownloadTask(PRIORITY.LOW, "l3"));
		taskDeque.offer(new DownloadTask(PRIORITY.MEDIUM, "m3"));
		taskDeque.offer(new DownloadTask(PRIORITY.MEDIUM, "m4"));
		taskDeque.offer(new DownloadTask(PRIORITY.LOW, "l4"));
		taskDeque.offer(new DownloadTask(PRIORITY.HIGH, "h3"));
		for (int i = 0; i < NUMBER_OF_THREAD; i++){
			System.out.println(i);
			DownloadThread thread = new DownloadThread();
			thread.setName("Download-Thread-" + i);
			thread.start();
		}		
		
	}
	
	public void addTask(DownloadTask futureTask){
		taskDeque.add(futureTask);
	}
	
	
	private class DownloadThread extends Thread{
		
		private boolean stopped = false;
		
		public DownloadThread() {
		}
		
		@Override
		public void run() {
			while (!isStopped()){
				try {
					System.out.println("finished : " + taskDeque.take().name);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public synchronized void doStop(){
			stopped = true;
			this.interrupt();
		}
		
		private boolean isStopped() {
			return stopped;
		}
		
	}
}
