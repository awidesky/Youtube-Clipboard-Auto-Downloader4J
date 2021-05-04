package com.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.ClipBoardCheckerThread;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;


/** Main class */
public class Main { 

	private static ExecutorService executorService;
	private static String clipboardBefore = "";
	private static boolean isSecondtime = false;
	private static ClipBoardCheckerThread clipChecker;
	private static PrintWriter logTo;
	private static GUI gui = new GUI();

	private static volatile int taskNum = 0;
	
	public static final String version = "v1.5.7";

	public static void main(String[] args) {
		
		try {
			setup(args);
		} catch (Exception e) {
			GUI.error("Error when initializing application!", e.getClass() + " : %e%", e);
			kill(1);
		}

	}
	
	
	private static void setup(String[] args) throws Exception {
		
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

		if (ConfigDTO.getClipboardListenOption().equals("Stop listening clipboard")) {
			log("[debug] clipboard ignored due to ClipboardListenOption == \"Stop listening clipboard\"");
			return;
		}

		clipChecker.submit(() -> { // To avoid overhead in EDT, put task in clipCheckerThread.

			try {

				Thread.sleep(50);

				final String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
						.getData(DataFlavor.stringFlavor);

				log("[debug] clipboard : " + data);
				
				if (isRedundant(data)) return;
				
				clipboardBefore = data;

				if (!data.startsWith("https://www.youtu"))
					return;

				if (ConfigDTO.getClipboardListenOption().equals("Ask when a link is found")) {
				
					SwingUtilities.invokeLater(() -> {
						
						if (!GUI.confirm("Download link in clipboard?", "Download link : " + data)) {
							
							Main.log("\n[GUI.linkAcceptChoose] Download link " + data + "? : " + false + "\n");
							
						} else {
							
							Main.log("\n[GUI.linkAcceptChoose] Download link " + data + "? : " + true + "\n");	
							Arrays.stream(data.split("\n")).forEach(Main::submitDownload);
							
						}
						
					});
					
					return;
				}

				Arrays.stream(data.split("\n")).forEach(Main::submitDownload);

			} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {

				GUI.error("Error when checking clipboard!", "%e%", e1);

			}

		});

	}
	
	private static boolean isRedundant(String data) throws InterruptedException {
		
		if (clipboardBefore.equals(data)) {

			if (isSecondtime) { //second time finding same data
				Thread.sleep(50);
				clipboardBefore = ""; 
			} else {
				isSecondtime = true; 
			}
			
			return true;

		} else {
			return false; 
		}
		
	}

	private static void submitDownload(String data) {

		int num = taskNum++;
		log("\n");
		log("[Task" + num + "] " + "Received a link from your clipboard : " + data);

		TaskData t = new TaskData(num);
		
		t.setFuture( executorService.submit(() -> { // download worker thread

			t.setVideoName(data); // temporarily set video name as url
			t.setDest(ConfigDTO.getSaveto());
			t.setStatus("Validating...");
			TaskStatusModel.getinstance().addTask(t);

			String url = "\"" + data + "\"";

			if (YoutubeAudioDownloader.validateAndSetName(url, t)) {

				t.setStatus("Preparing...");

				try {

					YoutubeAudioDownloader.download(url, t);

				} catch (Exception e1) {

					GUI.error("[Task" + num + "|downloading] " + "Error when downloading!", "%e%", e1);
					return;

				}

			} else {
				GUI.error("[Task" + num + "|validating] " + "Not a valid url!",
						data + "\nis not valid or unsupported url!", null);
				return;
			}

		}));
	}
	
	private static void prepareLogFile() {
		
		try {
			
			File logFolder = new File(YoutubeAudioDownloader.getProjectpath() + File.separator + "logs");
			File logFile = new File(logFolder.getAbsolutePath() + File.separator + "log-" + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + ".txt");
			logFolder.mkdirs();
			logFile.createNewFile();
			
			logTo = new PrintWriter(new FileOutputStream(logFile), true);
			
		} catch (IOException e) {

			logTo = new PrintWriter(System.out, true);
			GUI.error("Error when creating log flie", "%e%", e);
			
		}
		
	}

	private static void readProperties() {

		String p = YoutubeAudioDownloader.getProjectpath(); //Default path for save is project root path
		String f = "mp3";
		String q = "0";
		String l = "--no-playlist";
		String n = "%(title)s.%(ext)s"; 
		String c = "Download link automatically"; 
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(
				YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {

			p = Optional.of(br.readLine()).get().split("=")[1];
			f = Optional.of(br.readLine()).get().split("=")[1];
			q = Optional.of(br.readLine()).get().split("=")[1];
			l = Optional.of(br.readLine()).get().split("=")[1];
			n = Optional.of(br.readLine()).get().split("=")[1];
			c = Optional.of(br.readLine()).get().split("=")[1];

		} catch (FileNotFoundException e1) {

			GUI.warning("config.txt not exists!","%e%\nDon't worry! I'll make one later...", e1);

		} catch (IOException e) {

			GUI.warning("Exception occurred when reading config.txt", "%e%\nI'll initiate config.txt with default...", e);

		} catch (NullPointerException e2) {
			
			GUI.warning("config.txt has no or invalid data!", "NullPointerException : %e%\nI'll initiate config.txt with default...", e2);

		} finally {
			
			ConfigDTO.setSaveto(p);
			ConfigDTO.setFormat(f);
			ConfigDTO.setQuality(q);
			ConfigDTO.setPlaylistOption(l);
			ConfigDTO.setFileNameFormat(n);
			ConfigDTO.setClipboardListenOption(c);
			
			Main.logProperties("\n\nInitial");
			
		}

	}

	
	/**
	 * Writes properties to file.
	 * This method should only invoked just before the termination of the program.
	 * */
	public static void writeProperties() {

		/** Write <code>properties</code> */
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
				YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt")))) {

			File cfg = new File(
					YoutubeAudioDownloader.getProjectpath() + "\\YoutubeAudioAutoDownloader-resources\\config.txt");
			
			if (!cfg.exists()) cfg.createNewFile();

			bw.write("SavePath=" + ConfigDTO.getSaveto() + "\n");
			bw.write("Format=" + ConfigDTO.getFormat() + "\n");
			bw.write("Quality=" + ConfigDTO.getQuality() + "\n");
			bw.write("Playlist=" + ConfigDTO.getPlaylistOption().toComboBox() + "\n");
			bw.write("FileNameFormat=" + ConfigDTO.getFileNameFormat() + "\n");
			bw.write("ClipboardListenOption=" + ConfigDTO.getClipboardListenOption() + "\n");
			
			Main.logProperties("\n\nFinal");
			
		} catch (IOException e) {

			GUI.error("Error when writing config.txt file", "%e%", e);

		} finally {
			
			logTo.close();
			
		}

	}
	
	public static void logProperties(String status) {
		
		Main.log(status + ConfigDTO.status());
		
	}



	public static void log(String data) {

		logTo.println(data);

	}
	
	public static void log(Exception e) {
		
		e.printStackTrace(logTo);
		
	}

	
	public static ExecutorService getExecutorservice() {
		return executorService;
	}

	/*
	 * Close the window and kills the application.
	 * 
	 * */
	public static void kill(int exitcode) {
		
		Main.log("YoutubeAudioAutoDownloader exit code : " + exitcode);
		if (executorService != null) executorService.shutdownNow();
		Main.writeProperties();
		System.exit(exitcode);
		
	}

}
