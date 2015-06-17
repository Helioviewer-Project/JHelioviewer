package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

public class UltimateDownloadManager {	
	
		
	private static final Comparator<AbstractRequest> COMPARATOR = new Comparator<AbstractRequest>() {
		@Override
		public int compare(AbstractRequest o1, AbstractRequest o2) {
			if (o1.getPriority() == o2.getPriority()) return 0;
			return o2.getPriority().ordinal() < o1.getPriority().ordinal() ? 1 : -1;
		}
	};
	
	public static void main(String[] args) {
		new UltimateDownloadManager();
		
		while (true) {
		}
	}
	
	private static PriorityBlockingQueue<AbstractRequest> taskDeque = null;
	
	private static final int NUMBER_OF_THREAD = 6;
	
	
	
	static{
		taskDeque = new PriorityBlockingQueue<AbstractRequest>(1000, COMPARATOR);
		/*
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.HIGH, "a"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.HIGH, "b"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.MEDIUM, "f"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.MEDIUM, "g"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "k"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "l"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.HIGH, "c"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.HIGH, "d"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.MEDIUM, "h"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.MEDIUM, "i"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "m"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "n"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.MEDIUM, "j"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.HIGH, "e"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "o"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "p"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "q"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "r"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "s"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "t"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "u"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "v"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "w"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "x"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "y"));
		UltimateDownloadManager.addRequest(new Ttttt(PRIORITY.LOW, "z"));
		*/
		
		for (int i = 0; i < NUMBER_OF_THREAD; i++){
			UltimateDownloadManager.DownloadThread thread = new UltimateDownloadManager.DownloadThread();
			thread.setName("Download-Thread-" + i);
			thread.start();
		}		
		
	}
	
	public static void addRequest(AbstractRequest request){
		//taskDeque.offer(request);
		taskDeque.put(request);
	}	
	
	private static class DownloadThread extends Thread{
		
		private boolean stopped = false;
		
		public DownloadThread() {
		}
		
		@Override
		public void run() {
			while (!isStopped()){
				try {
					AbstractRequest request = taskDeque.take();
					try {
						request.execute();
					} catch (IOException e) {
						if (request.decrementRetries() > 0){
							System.out.println("retry");
							addRequest(request);
						}
						else
							e.printStackTrace();
					}
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
	
	private static class Ttttt extends AbstractRequest{
		private String name;
		
		public Ttttt(PRIORITY priority, String name) {
			super(priority);
			this.name = name;
		}

		@Override
		void execute() {
			System.out.println(name);
		}
		
	}

}
