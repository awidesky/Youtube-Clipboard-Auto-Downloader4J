package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.util.concurrent.LinkedBlockingQueue;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;

public class ClipBoardCheckerThread extends Thread {

	private static LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	
	public ClipBoardCheckerThread() {

		super(() -> {

			while(true) {
				
				 try {
				 	 queue.take().run();
				} catch (InterruptedException e) {
					 Main.log("ClipBoardCheckerThread Interrupted! : " + e.getMessage());
				}
			}

		});
		
		this.setDaemon(true);

	}
	
	public void submit(Runnable r) {
		
		queue.offer(r);
		
	}


}
