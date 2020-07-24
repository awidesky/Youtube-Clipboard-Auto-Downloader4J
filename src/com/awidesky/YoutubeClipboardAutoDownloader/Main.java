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

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.awidesky.YoutubeClipboardAutoDownloader.view.SettingGUI;

/** Main class */
public class Main {

	private static ExecutorService executorService = Executors.newFixedThreadPool(1);
	private static String clipboardBefore = "";
	private static File savePath;
	private static String extension;
	private static String quality;
	private static ConfigDTO properties;
	
	public static void main(String[] args) {
		
		YoutubeAudioDownloader.checkFiles();
		readProperties();
		
		JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setCurrentDirectory(Main.getSavePath());
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
		
		savePath = dir;
		
		log("Listening clipboard...");

		SettingGUI.launch(args);
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> { writeProperties(); }));
		
	}
	
	



	private static void readProperties() {
		
        try(ObjectInputStream oi = new ObjectInputStream(new FileInputStream(new File(YoutubeAudioDownloader.projectpath + "\\YoutubeAudioAutoDownloader-resources\\config.bin")))) {
        	
            properties = (ConfigDTO)oi.readObject();
            
            savePath = new File(properties.getSaveto());
            extension = properties.getExtension();
            quality = properties.getQuality();
            
        } catch (IOException | ClassNotFoundException e) {
        	
            log("Error when reading config.ini : " + e.getMessage() + "\nResetting config.ini ...");
    		
            properties.setSaveto(".\\");
    		properties.setExtension("mp3");
    		properties.setQuality("0");
    		
    		savePath = new File(".\\");
            extension = "mp3";
            quality = "0";
            
        }
        
	}


	private static void writeProperties() {
		
		/** Save current properties to DTO instance */
		properties.setSaveto(savePath.getAbsolutePath());
		properties.setExtension(extension);
		properties.setQuality(quality);
		
		
		/** Write it */
		try(ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(new File(YoutubeAudioDownloader.projectpath + "\\YoutubeAudioAutoDownloader-resources\\config.bin")))) {
			
			oo.writeObject(properties);
			
		} catch (IOException e) {
			
			log("Error when writing config file : " + e.getMessage());
			
		}
		
	}

	public static File getSavePath() {
		
		return savePath;
	}
	



	public static String getExtension() {
		
		return extension;
		
	}



	public static String getQuality() {
		
		return quality;
		
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
