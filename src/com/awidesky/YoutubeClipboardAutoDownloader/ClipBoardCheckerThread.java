package com.awidesky.YoutubeClipboardAutoDownloader;

import java.util.concurrent.LinkedBlockingQueue;

public class ClipBoardCheckerThread extends Thread {

	private static LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	
	public ClipBoardCheckerThread() {

		super(() -> {

			try {

				queue.take().run();

			} catch (InterruptedException e) {
				Main.log("ClipBoardCheckerThread Interrupted! : " + e.getMessage());
			}

		});
		
		this.setDaemon(true);
	}
	
	public void submit(Runnable r) {
		
		queue.offer(r);
		
	}


}
