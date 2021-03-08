package com.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	private static PrintWriter logTo;
	private static GUI gui;

	public static final String version = "v1.2.5-beta";

	public static void main(String[] args) {

		prepareLogFile();
		
		YoutubeAudioDownloader.checkFiles();
		readProperties();

		SwingUtilities.invokeLater(() -> {
			gui = new GUI();
		});

		if (args != null && args.length != 0) { // For test
			
			StringBuilder sb = new StringBuilder();

			for (String s : args) {
				sb.append(" " + s);
			}
			
			log("Extra arguments : " + sb.toString());
			YoutubeAudioDownloader.setArgsOptions(sb.toString());
			
		}

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

								TaskStatusViewerModel t = new TaskStatusViewerModel();
								t.setStatus("Preparing...");
								gui.addTaskModel(t);
								
								try {

									YoutubeAudioDownloader.download(data, t);

								} catch (Exception e1) {

									GUI.error("Error in downloading! : ", e1.getMessage());
									return;

								}

							}

						});

					} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {

						GUI.error("Error! : ", e1.getMessage());

					} // try end

				}); // submit end

			} // flavorsChanged end

		}); // FlavorListener end

		SwingUtilities.invokeLater(() -> {
			gui.showWindow();
		});
		
		log("Listening clipboard...");

	}

	private static void prepareLogFile() {
		try {
			
			File logFile = new File(YoutubeAudioDownloader.getProjectpath() + "\\logs\\log-"
					+ new SimpleDateFormat("yyyyMMddkkmmss").format(new Date()) + ".txt");
			logFile.getParentFile().mkdirs();
			logFile.createNewFile();
			logTo = new PrintWriter(logFile);
			
		} catch (IOException e) {
			
			GUI.error("Error when creating log flie", e.getMessage());
			logTo = new PrintWriter(System.out);
			
		}
	}

	private static void readProperties() {

		String p = YoutubeAudioDownloader.getProjectpath(); //Default path for save is project root path
		String f = "mp3";
		String q = "0";
		String l = "--no-playlist";
		
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(
				YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {

			p = br.readLine().substring(9);
			f = br.readLine().substring(7);
			q = br.readLine().substring(8);
			l = br.readLine().substring(9);

		} catch (FileNotFoundException e1) {

			GUI.warning("config.txt not exists!", e1.getMessage() + "\nDon't worry! I'll make one later...");

		} catch (IOException e) {

			GUI.warning("Exception occurred when reading config.txt",
					e.getMessage() + "\nInitiating config.txt anyway...");

		} catch (Exception e2) {

			GUI.warning(e2.getClass() + " occurred when reading config.txt",
					e2.getMessage() + "\nInitiating config.txt anyway...");

		} finally {
			
			Main.logProperties("Initial");
			properties = new ConfigDTO(p, f, q, l);
			
		}

	}

	public static void writeProperties() {

		/** Write <code>properties</code> */
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
				YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {

			File cfg = new File(
					YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt");
			if (!cfg.exists())
				cfg.createNewFile();

			bw.write("SavePath=" + properties.getSaveto() + "\n");
			bw.write("Format=" + properties.getFormat() + "\n");
			bw.write("Quality=" + properties.getQuality() + "\n");
			bw.write("Playlist=" + properties.getPlaylistOption() + "\n");
			
			Main.logProperties("Final");
			
		} catch (IOException e) {

			GUI.error("Error when writing config.txt file : ", e.getMessage());

		}

	}
	
	public static void logProperties(String status) {
		
		Main.log(String.format(status + " properties :\n downloadpath-%s\n format-%s\n quality-%s\n playlistoption-%s", Main.getProperties().getSaveto(), Main.getProperties().getFormat(), Main.getProperties().getQuality(), Main.getProperties().getPlaylistOption()));
		
	}

	public static ConfigDTO getProperties() {

		return properties;

	}

	private static void clearClipboardBefore() {

		clipboardBefore = ""; // System.out.println("clearclipboard called");

	}

	public static void log(String data) {

		logTo.println(data);

	}

}
