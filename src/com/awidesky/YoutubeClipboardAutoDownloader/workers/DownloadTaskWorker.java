package com.awidesky.YoutubeClipboardAutoDownloader.workers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;

public class DownloadTaskWorker {

	private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	public static Future<?> submit(Runnable task) {
		return executorService.submit(task);
	}

	public static void kill() {
		
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdownNow();
			try {
				executorService.awaitTermination(2500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Main.log("Failed to wait worker Thread to shutdown!");
				Main.log(e);
			}
		}
		
	}
}
