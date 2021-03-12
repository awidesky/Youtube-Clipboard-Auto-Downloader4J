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
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.ClipBoardCheckerThread;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.LoadingStatus;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusViewerModel;

/** Main class */
public class Main { 


	private static ExecutorService executorService;
	private static String clipboardBefore = "";
	private static ConfigDTO properties = null;
	private static boolean isSecondtime = false;
	private static ClipBoardCheckerThread clipChecker;
	private static PrintWriter logTo;
	private static GUI gui = new GUI();

	private static volatile int taskNum = 0;
	
	private static int exitcode;
	public static final String version = "v1.2.5-beta";

	public static void main(String[] args) {

		prepareLogFile();

		try {
			SwingUtilities.invokeAndWait(() -> {
				gui.initLoadingFrame();
			});
		} catch (InvocationTargetException | InterruptedException e2) {
			log("[init] failed wating EDT!");
			log(e2);
		}

		gui.setLoadingStat(LoadingStatus.CHECKING_YDL);
		YoutubeAudioDownloader.checkYoutubedl();
		gui.setLoadingStat(LoadingStatus.CHECKING_FFMPEG);
		YoutubeAudioDownloader.checkFfmpeg();
		
		gui.setLoadingStat(LoadingStatus.READING_PROPERTIES);
		readProperties();

		if (args != null && args.length != 0) { // For test
			
			StringBuilder sb = new StringBuilder("");

			for (String s : args) {
				sb.append(' ' + s);
			}
			
			log("Extra arguments : " + sb.toString());
			YoutubeAudioDownloader.setArgsOptions(sb.toString());
			
		}

		
		gui.setLoadingStat(LoadingStatus.PREPARING_THREADS);
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		clipChecker = new ClipBoardCheckerThread();
		clipChecker.start(); //A daemon thread that will check clipboard

		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(new FlavorListener() {

			@Override
			public void flavorsChanged(FlavorEvent e) { // This code is invoked in EDT!!

				// System.err.println("CLIPBOARD CHANGED");

				clipChecker.submit(() -> { //To avoid overhead in EDT, put task in clipCheckerThread.

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

						executorService.submit(() -> { //download worker thread

							if (data.startsWith("https://www.youtu")) {

								int num = taskNum++;
								log("[Task" + num + "] " + "Received a link from your clipboard : " + data);

								if (YoutubeAudioDownloader.checkURL(data, num)) {
									
									TaskStatusViewerModel t = new TaskStatusViewerModel(num);
									t.setStatus("Preparing...");
									gui.addTaskModel(t);

									try {

										YoutubeAudioDownloader.download(data, t);

									} catch (Exception e1) {

										GUI.error("[Task" + num + "] " +"Error when downloading!", "%e%", e1);
										return;

									}
									
								} else { GUI.error("[Task" + num + "] " +"Not a valid url!", data + "\nis not valid or unsupported url!", null); return; }

							}

						}); // submit to executor service end

					} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {

						GUI.error("Error! : ", "%e%", e1);

					} // try end

				}); // submit to clipChecker end

			} // flavorsChanged end

		}); // FlavorListener end

		gui.setLoadingStat(LoadingStatus.LOADING_WINDOW);
		SwingUtilities.invokeLater(() -> {
			gui.initMainFrame();
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

			logTo = new PrintWriter(System.out);
			GUI.error("Error when creating log flie", "%e%", e);
			
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

			GUI.warning("config.txt not exists!","%e%\nDon't worry! I'll make one later...", e1);

		} catch (IOException e) {

			GUI.warning("Exception occurred when reading config.txt",
					"%e%\nInitiating config.txt anyway...", e);

		} catch (Exception e2) {

			GUI.warning(e2.getClass() + " occurred when reading config.txt",
					"%e%\nInitiating config.txt anyway...", e2);

		} finally {
			
			properties = new ConfigDTO(p, f, q, l);
			Main.logProperties("Initial");
			
		}

	}

	
	/**
	 * Writes properties to file.
	 * This method should only invoked just before the termination of the program.
	 * */
	public static void writeProperties() {

		if(properties == null) return;
		
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

			GUI.error("Error when writing config.txt file", "%e%", e);

		} finally {
			
			logTo.close();
			
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
	
	public static void log(Exception e) {
		
		e.printStackTrace(logTo);
		
	}

	public static int getExitcode() {
		return exitcode;
	}

	public static void setExitcode(int exitcode) {
		Main.exitcode = exitcode;
	}
	
	
	public static ExecutorService getExecutorservice() {
		return executorService;
	}

	/*
	 * Close the window and kills the application.
	 * 
	 * */
	public static void kill() {
		
		Main.writeProperties();
		logTo.close();
		System.exit(Main.getExitcode());
		
	}

}
