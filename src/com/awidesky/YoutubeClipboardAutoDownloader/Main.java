package com.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/** Main class */
public class Main {

	private static ExecutorService executorService = Executors.newFixedThreadPool(1);
	private static String clipboardBefore = "";
	public static void main(String[] args) {
		
		YoutubeAudioDownloader.checkFiles();
		
		JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setDialogTitle("Choose directory to save music!");
        
        if (jfc.showDialog(new JFrame(), null) != JFileChooser.APPROVE_OPTION) { JOptionPane.showMessageDialog(null, "Please choose a directory!","ERROR!",JOptionPane.WARNING_MESSAGE); System.exit(1);; }
        
        File dir = jfc.getSelectedFile();
		
		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() { 
		
				@Override 
			    public void flavorsChanged(FlavorEvent e) {
					
					//System.err.println("CLIPBOARD CHANGED");

					try {
						
						Thread.sleep(50);
						final String data = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
						//System.out.println("data : " + data + "\nex-clipboard : " + clipboardBefore);
						
						if (data.equals(clipboardBefore)) {	clearClipboardBefore();	return;	}
						
						clipboardBefore = data;
						
						executorService.submit(() -> {
						
						
							if (data.startsWith("https://www.youtu")) {
			    	  
								log("Received a link from your clipboard : " + data);
								
								try {
									
									YoutubeAudioDownloader.download(data, dir.getAbsolutePath());
									
								} catch (Exception e1) {
									
									log(e1);
									
								}

							}
						
						
						});
						
						
					} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {
						
						log(e1);
					
					}
					
					
				}
		
		});
		
		log("Listening clipboard...");
		
	}
	
	
	
	protected static void clearClipboardBefore() {
		
		clipboardBefore = ""; //System.out.println("clearclipboard called");
		
	}


	public static void log(String data) {
		
		System.out.println(data);
		
	}
	
	public static void log(Exception e) {
		
		System.out.println(e);
		
	}

}
