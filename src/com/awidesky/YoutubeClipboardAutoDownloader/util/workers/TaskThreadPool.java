package com.awidesky.YoutubeClipboardAutoDownloader.util.workers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.util.Logger;

public class TaskThreadPool {

	private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private static Logger log = Main.getLogger("[ClipBoardChecker] ");
	
	public static Future<?> submit(Runnable task) {
		return executorService.submit(task);
	}

	public static void kill() {
		
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdownNow();
			try {
				executorService.awaitTermination(2500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				log.log("Failed to wait worker Thread to shutdown!");
				log.log(e);
			}
		}
		
	}
}
