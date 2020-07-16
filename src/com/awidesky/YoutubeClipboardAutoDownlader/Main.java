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

	private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	public static void main(String[] args) throws Exception {

		JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setDialogTitle("Choose directory for saving music!");
        jfc.showDialog(new JFrame(), null);
        File dir = jfc.getSelectedFile();
		
		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() { 
		
				@Override 
			    public void flavorsChanged(FlavorEvent e) {

					executorService.submit(() -> {
						
						try {
						
							Thread.sleep(50);
							//TODO : https://stackoverflow.com/questions/14242719/copying-to-global-clipboard-does-not-work-with-java-in-ubuntu
							String data = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
					
							if (data.startsWith("https://www.youtu")) {
			    	  
								log("Receved a link from your clipboard : " + data);
								YoutubeAudioDownloader.download(data, dir.getAbsolutePath());

							}
						
						} catch(Exception err) {
						
							log(err.toString());
						
						}
			   
					});
				}
		
		});
		
		log("Listning clipboard...");
		
	}
	
	
	public static void log(String data) {
		
		System.out.println(data);
		
	}

}
