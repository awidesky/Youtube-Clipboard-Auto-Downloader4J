package com.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

/** Main class */
public class Main {

	private static ExecutorService executorService = Executors.newFixedThreadPool(1);
	private static String clipboardBefore = "";
	private static ConfigDTO properties;
	
	public static void main(String[] args) {
		
		YoutubeAudioDownloader.checkFiles();
		readProperties();
		
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
									
									YoutubeAudioDownloader.download(data);
									
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
		
		SwingUtilities.invokeLater(() -> { new GUI(); log("Listening clipboard..."); });
		
	}
	


	private static void readProperties() {
		
        try(BufferedReader br = new BufferedReader(new FileReader(new File(YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {
        	
            properties = new ConfigDTO(br.readLine().substring(9), br.readLine().substring(7), br.readLine().substring(8));
            
        } catch (IOException e) {
        	
            log("Error when reading config.txt : " + e.getMessage() + "\nInitiating config.txt ...");
    		
            properties = new ConfigDTO(".\\", "mp3", "0");
            
        }
        
	}


	public static void writeProperties() {
		
		/** Write <code>properties</code> */
		try(PrintWriter pw = new PrintWriter(new FileWriter(new File(YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {
			
			pw.println("SavePath=" + properties.getSaveto());
			pw.println("Format=" + properties.getFormat());
			pw.println("Quality=" + properties.getQuality());
			
		} catch (IOException e) {
			
			log("Error when writing config.txt file : " + e.getMessage());
			
		}
		
	}
	
	
	public static ConfigDTO getProperties() {
		
		return properties;
		
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
