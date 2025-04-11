package io.github.awidesky.YoutubeClipboardAutoDownloader.util.workers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.awidesky.YoutubeClipboardAutoDownloader.Main;
import io.github.awidesky.guiUtil.Logger;

public class DownloadThreadPool {

	private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); // TODO : download pool size 
	private static Logger log = Main.getLogger("[DownloadThreadPool] ");
	
	public static Future<?> submit(Runnable task) { return executorService.submit(task); }
	public static void kill(long timeout) {
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdownNow();
			try {
				executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				log.log("Failed to wait worker Thread to shutdown!");
				log.log(e);
			}
		}
	}
}
