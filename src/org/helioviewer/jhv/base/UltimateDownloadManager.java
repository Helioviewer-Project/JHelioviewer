package org.helioviewer.jhv.base;

import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;

public class UltimateDownloadManager {
	private PriorityBlockingQueue<FutureTask<?>> taskDeque = null;
	
	private static final int NUMBER_OF_THREAD = 4;
	
	public static final UltimateDownloadManager SINGLETON = new UltimateDownloadManager();
	
	
	private UltimateDownloadManager() {
		taskDeque = new PriorityBlockingQueue<FutureTask<?>>();
		for (int i = 0; i < NUMBER_OF_THREAD; i++){
			DownloadThread thread = new DownloadThread(taskDeque);
			thread.setName("Download-Thread-" + i);
			thread.start();
		}		
		
	}
	
	public void addTask(FutureTask<?> futureTask){
		taskDeque.add(futureTask);
	}
	
	public void setHighPriority(FutureTask<?> futureTask){
		for (FutureTask<?> task : taskDeque){
			if (task.equals(futureTask)){
				taskDeque.remove(task);
				taskDeque.add(task);
				return;
			}
		}
	}
	
	
	private class DownloadThread extends Thread{
		
		private PriorityBlockingQueue<FutureTask<?>> taskDeque;
		private boolean stopped = false;
		
		public DownloadThread(PriorityBlockingQueue<FutureTask<?>> taskDeque) {
			this.taskDeque = taskDeque;	
		}
		
		@Override
		public void run() {
			while (!isStopped()){
				try {
					taskDeque.take().run();
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
