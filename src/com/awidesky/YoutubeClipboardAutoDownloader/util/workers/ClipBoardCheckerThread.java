package com.awidesky.YoutubeClipboardAutoDownloader.util.workers;

import java.util.concurrent.LinkedBlockingQueue;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.util.Logger;

public class ClipBoardCheckerThread extends Thread {

	private static LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	private static Logger log = Main.getLogger("[ClipBoardChecker] ");
	
	public ClipBoardCheckerThread() {

		super(() -> {

			while(true) {
				
				 try {
				 	 queue.take().run();
				} catch (InterruptedException e) {
					 log.log("ClipBoardCheckerThread Interrupted!");
					 log.log(e);
				}
			}

		});
		
		this.setDaemon(true);

	}
	
	public void submit(Runnable r) {
		
		queue.offer(r);
		
	}


}
