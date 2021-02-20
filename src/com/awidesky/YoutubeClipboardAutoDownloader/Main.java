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
import java.nio.file.NoSuchFileException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

/** Main class */
public class Main {

	private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private static String clipboardBefore = "";
	private static ConfigDTO properties;
	private static boolean isSecondtime = false;
	private static ClipBoardCheckerThread clipChecker = new ClipBoardCheckerThread();

	private static GUI gui;
	
	public static final String version = "v1.2.5-beta";
	

	public static void main(String[] args) {

		YoutubeAudioDownloader.checkFiles(); //TODO : check another files like ffmpeg
		readProperties();

		SwingUtilities.invokeLater(() -> {
			gui = new GUI();
		});

		StringBuilder sb = new StringBuilder("--newline");
		
		for(String s : args) { //for test
			sb.append(" " + s);
		}
		YoutubeAudioDownloader.setArgsOptions(sb.toString());
		
		clipChecker.start();

		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() {

			@Override
			public void flavorsChanged(FlavorEvent e) { // This code is invoked in EDT!!

				// System.err.println("CLIPBOARD CHANGED");

				clipChecker.submit(() -> {
					
					try {

						Thread.sleep(50);
						final String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
								.getData(DataFlavor.stringFlavor);
						// System.out.println("data : " + data + "\nex-clipboard : " + clipboardBefore);

						if (data.equals(clipboardBefore)) {

							if (!isSecondtime) {

								Thread.sleep(50);
								clearClipboardBefore();
								return;

							} else {

								return;

							}

						}

						clipboardBefore = data;

						executorService.submit(() -> {

							if (data.startsWith("https://www.youtu")) {

								log("Received a link from your clipboard : " + data);

								TaskStatusViewerModel t = new TaskStatusViewerModel("", "Starting", 0, "");
								
								try {
									
									YoutubeAudioDownloader.download(data, t);
								
								} catch (Exception e1) {

									GUI.error("Error in downloading! : " , e1.getMessage());
									return;
									
								}
								
								gui.addTaskModel(t);
								
							}

						});

						} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {

							 GUI.error("Error! : " , e1.getMessage());

						} //try end

					}); //submit end

				} //flavorsChanged end

			}); //FlavorListener end

			SwingUtilities.invokeLater(() -> { gui.show(); }
			log("Listening clipboard...");

	}

	private static void readProperties() {
		
                 try(BufferedReader br = new BufferedReader(new FileReader(new File(YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {
        	
                    properties = new ConfigDTO(br.readLine().substring(9), br.readLine().substring(7), br.readLine().substring(8), br.readLine().substring(17));
                    YoutubeAudioDownloader.setDownloadPath(properties.getSaveto()); 
            
                 } catch (NoSuchFileException e1) { 
		
        	    GUI.warning("config.txt not exists!", e1.getMessage() + "\nDon't worry! I'll make one later...");
	    
		    properties = new ConfigDTO(new File(".\\").getAbsolutePath(), "mp3", "0", "false");
		
                } catch (IOException e) {
        	
                    GUI.warning("Error when reading config.txt", e.getMessage() + "\nInitiating config.txt anyway...");
    		
                    properties = new ConfigDTO(new File(".\\").getAbsolutePath(), "mp3", "0", "false");
            
                }
        
	}


	public static void writeProperties() {
		
		/** Write <code>properties</code> */
		try(PrintWriter pw = new PrintWriter(new FileWriter(new File(YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {
			
			File cfg = new File(YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt");
			if(!cfg.exists()) cfg.createNewFile();
			
			pw.println("SavePath=" + properties.getSaveto());
			pw.println("Format=" + properties.getFormat());
			pw.println("Quality=" + properties.getQuality());
			
		} catch (IOException e) {
			
			GUI.error("Error when writing config.txt file : ", e.getMessage());
			
		}
		
	}
	
	
	public static ConfigDTO getProperties() {
		
		return properties;
		
	}


	
	private static void clearClipboardBefore() {
		
		clipboardBefore = ""; //System.out.println("clearclipboard called");
		
	}


	public static void log(String data) {
		
		System.out.println(data);
		
	}

}
