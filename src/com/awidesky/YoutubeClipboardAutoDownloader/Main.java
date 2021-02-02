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

	private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private static String clipboardBefore = "";
	private static ConfigDTO properties;
	private static boolean isSecondtime = false;
	private static ClipBoardCheckerThread clipChecker = new ClipBoardCheckerThread();

	private static GUI gui; //TODO: Do we need this?


	public static void main(String[] args) {

		YoutubeAudioDownloader.checkFiles(); //TODO : check another files like ffmpeg
		readProperties();

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

								
								try {
									TaskStatusViewerModel t = new TaskStatusViewerModel("", "Starting", 0, "");
									YoutubeAudioDownloader.download(data, t);
									gui.addTaskModel(t);
								
								} catch (Exception e1) {

									log("Error in downloading! : " + e1.getMessage());

								}
								
							}

						});

					} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {

						log("Error! : " + e1.getMessage());

					} //try end

				}); //submit end

			} //flavorsChanged end

		}); //FlavorListener enc

		SwingUtilities.invokeLater(() -> {
			new GUI();
			log("Listening clipboard...");
		});


	}

	private static void readProperties() {
		
        try(BufferedReader br = new BufferedReader(new FileReader(new File(YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {
        	
            properties = new ConfigDTO(br.readLine().substring(9), br.readLine().substring(7), br.readLine().substring(8), br.readLine().substring(17));
            YoutubeAudioDownloader.setDownloadPath(properties.getSaveto()); 
        } catch (IOException e) {
        	
            GUI.warning("Error when reading config.txt", e.getMessage() + "\nInitiating config.txt anyway...");
    		
            properties = new ConfigDTO(".\\", "mp3", "0", "false");
            
        }
        
	}


	public static void writeProperties() {
		
		/** Write <code>properties</code> */
		try(PrintWriter pw = new PrintWriter(new FileWriter(new File(YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {
			
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
