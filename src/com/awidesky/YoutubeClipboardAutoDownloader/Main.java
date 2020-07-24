package com.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.awidesky.YoutubeClipboardAutoDownloader.view.SettingGUI;

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
									
									YoutubeAudioDownloader.download(data, properties.getSaveto());
									
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

		SettingGUI.launch(args);
		
	}
	
	



	private static void readProperties() {
		
        try(ObjectInputStream oi = new ObjectInputStream(new FileInputStream(new File(YoutubeAudioDownloader.projectpath + "\\YoutubeAudioAutoDownloader-resources\\config.bin")))) {
        	
            properties = (ConfigDTO)oi.readObject();
            
        } catch (IOException | ClassNotFoundException e) {
        	
            log("Error when reading config.ini : " + e.getMessage() + "\nResetting config.ini ...");
    		
            properties.setSaveto(".\\");
    		properties.setExtension("mp3");
    		properties.setQuality("0");
    		
        }
        
	}


	public static void writeProperties() {
		
		/** Write <code>properties</code> */
		try(ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(new File(YoutubeAudioDownloader.projectpath + "\\YoutubeAudioAutoDownloader-resources\\config.bin")))) {
			
			oo.writeObject(properties);
			
		} catch (IOException e) {
			
			log("Error when writing config file : " + e.getMessage());
			
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
