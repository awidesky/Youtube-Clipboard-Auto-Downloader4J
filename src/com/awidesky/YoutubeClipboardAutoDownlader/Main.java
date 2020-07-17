package com.awidesky.YoutubeClipboardAutoDownlader;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

/** Main class */
public class Main {

	private static ExecutorService executorService = Executors.newFixedThreadPool(1);
	private static boolean isOkToStart = false; /** I don't know why but when you copied something, <code>flavorsChanged</code> invoked twice and we should ignore the first one. */
	
	public static void main(String[] args) throws Exception {

		JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setDialogTitle("Choose directory for saving music!");
        jfc.showDialog(new JFrame(), null);
        File dir = jfc.getSelectedFile();
		
		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() { 
		
				@Override 
			    public void flavorsChanged(FlavorEvent e) {

					if (!isOkToStart) { isOkToStart = true; return; } 

					System.err.println("CLIPBOARD CHANGED");
					
					executorService.submit(() -> {
						
						try {
						
							Thread.sleep(100);
							
							String data = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
					
							if (data.startsWith("https://www.youtu")) {
			    	  
								log("Receved a link from your clipboard : " + data);
								YoutubeAudioDownloader.download(data, dir.getAbsolutePath());

							}
						
						} catch(Exception err) {
						
							log(err.toString());
						
						}
			   
					});
					
					isOkToStart = !isOkToStart;
					
				}
		
		});
		
		log("Listning clipboard...");
		
	}
	
	
	public static void log(String data) {
		
		System.out.println(data);
		
	}

}
