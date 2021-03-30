package com.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
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
import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskData;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;


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

		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener((e) -> { checkClipBoard(); }); 

		gui.setLoadingStat(LoadingStatus.LOADING_WINDOW);
		SwingUtilities.invokeLater(() -> {
			gui.initMainFrame();
		});
		
		log("Listening clipboard...");

	}
	
	private static void checkClipBoard() {
		
		clipChecker.submit(() -> { //To avoid overhead in EDT, put task in clipCheckerThread.

			try {

				Thread.sleep(50);
				final String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
						.getData(DataFlavor.stringFlavor);
				// System.out.println("data : " + data + "\nex-clipboard : " + clipboardBefore);

				if(isRedundant(data)) return;

				clipboardBefore = data;

				submitDownload(data);

			} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {

				GUI.error("Error! : ", "%e%", e1);

			} 

		});
		
	}
	
	private static boolean isRedundant(String data) throws InterruptedException {
		
		if (data.equals(clipboardBefore)) {

			if (isSecondtime) { //second time finding same data

				Thread.sleep(50);
				clearClipboardBefore();

			} else {
				
				isSecondtime = true;
				
			}
			
			return true;

		} else {
			
			return false;
			
		}
		
	}

	private static void submitDownload(String data) {
		
		executorService.submit(() -> { //download worker thread

			if (data.startsWith("https://www.youtu")) {

				int num = taskNum++;
				log("[Task" + num + "] " + "Received a link from your clipboard : " + data);

				if (YoutubeAudioDownloader.checkURL(data, num)) {
					
					TaskData t = new TaskData(num, data);
					TaskStatusModel.getinstance().addTask(t);
					t.setStatus("Preparing...");

					try {

						YoutubeAudioDownloader.download(data, t);

					} catch (Exception e1) {

						GUI.error("[Task" + num + "] " +"Error when downloading!", "%e%", e1);
						return;

					}
					
				} else { GUI.error("[Task" + num + "] " +"Not a valid url!", data + "\nis not valid or unsupported url!", null); return; }

			}

		});
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
		String n = "%(title)s.%(ext)s";
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(
				YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {

			p = br.readLine().split("=")[1];
			f = br.readLine().split("=")[1];
			q = br.readLine().split("=")[1];
			l = br.readLine().split("=")[1];
			n = br.readLine().split("=")[1];
			
		} catch (FileNotFoundException e1) {

			GUI.warning("config.txt not exists!","%e%\nDon't worry! I'll make one later...", e1);

		} catch (IOException e) {

			GUI.warning("Exception occurred when reading config.txt", "%e%\nI'll initiate config.txt with default...", e);

		} catch (NullPointerException e2) {
			
			GUI.warning("config.txt has no or invalid data!", "%e%\nI'll initiate config.txt with default...", e2);


		} finally {
			
			properties = new ConfigDTO(p, f, q, l, n);
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
			bw.write("FileNameFormat=" + properties.getFileNameFormat() + "\n");
			
			Main.logProperties("Final");
			
		} catch (IOException e) {

			GUI.error("Error when writing config.txt file", "%e%", e);

		} finally {
			
			logTo.close();
			
		}

	}
	
	public static void logProperties(String status) {
		
		Main.log(status + getProperties().toString());
		
	}

	public static ConfigDTO getProperties() {

		return properties;

	}

	private static void clearClipboardBefore() {

		clipboardBefore = ""; 

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
		System.exit(Main.getExitcode());
		
	}

}
