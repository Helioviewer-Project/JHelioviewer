package org.helioviewer.jhv.base;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.FutureTask;

public class UltimateDownloadManager {
	private BlockingDeque<FutureTask<?>> taskDeque = null;
	
	private static final int NUMBER_OF_THREAD = 4;
	
	public static final UltimateDownloadManager SINGLETON = new UltimateDownloadManager();
	
	
	private UltimateDownloadManager() {
		for (int i = 0; i < NUMBER_OF_THREAD; i++){
			DownloadThread thread = new DownloadThread(taskDeque);
			thread.setName("Download-Thread-" + i);
			thread.start();
		}		
		
	}
	
	public void addTask(FutureTask<?> futureTask){
		taskDeque.addLast(futureTask);
	}
	
	public void setHighPriority(FutureTask<?> futureTask){
		for (FutureTask<?> task : taskDeque){
			if (task.equals(futureTask)){
				taskDeque.remove(task);
				taskDeque.addFirst(task);
				return;
			}
		}
	}
	
	
	private class DownloadThread extends Thread{
		
		private BlockingDeque<FutureTask<?>> taskDeque;
		private boolean stopped = false;
		
		public DownloadThread(BlockingDeque<FutureTask<?>> taskDeque) {
			this.taskDeque = taskDeque;	
		}
		
		@Override
		public void run() {
			while (!isStopped()){
				try {
					taskDeque.takeFirst().run();
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
