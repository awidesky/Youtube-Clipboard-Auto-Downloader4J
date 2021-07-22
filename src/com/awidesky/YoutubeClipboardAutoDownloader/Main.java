package com.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.Desktop;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

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
	private static LoggerThread logger = new LoggerThread();
	
	private static LinkedBlockingQueue<Runnable> loggerQueue = new LinkedBlockingQueue<>();
	
	private static volatile int taskNum = 0;
	
	public static final String version = "v1.6.0";

	public static void main(String[] args) {
		
		try {
			setup(args);
		} catch (Exception e) {
			GUI.error("Error when initializing application!", (e == null) ? "" : e.getClass() + " : %e%", e);
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
		
		log("\nistening clipboard...\n");

	}
	
	private static void checkClipBoard() {

		if (Config.getClipboardListenOption().equals("Stop listening clipboard")) {
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

				if (!Config.isLinkAcceptable(data))
					return;

				if (Config.getClipboardListenOption().equals("Ask when a link is found")) {

					if (!GUI.confirm("Download link in clipboard?", "Link : " + data)) {

						Main.log("\n[GUI.linkAcceptChoose] Download link " + data + "? : " + false + "\n");

					} else {

						Main.log("\n[GUI.linkAcceptChoose] Download link " + data + "? : " + true + "\n");
						Arrays.stream(data.split("\n")).forEach(Main::submitDownload);

					}

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
			t.setDest(Config.getSaveto());
			t.setStatus("Validating...");
			TaskStatusModel.getinstance().addTask(t);

			String url = "\"" + data + "\"";
			
			PlayListOption p = Config.getPlaylistOption();
			
			if (p == PlayListOption.ASK && data.contains("list=")) {
				p = (GUI.confirm("Download entire Playlist?", "PlayList Link : " + url)) ? PlayListOption.YES : PlayListOption.NO;
			}
					
			if (YoutubeAudioDownloader.validateAndSetName(url, t, p)) {

				t.setStatus("Preparing...");
				YoutubeAudioDownloader.download(url, t, p);

			} else {
				GUI.error("[Task" + num + "|validating] Not a valid url!",	data + "\nis not valid or unsupported url!", null);
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
			
		} finally {
			logger.start();
		}
		
	}

	private static void readProperties() {

		String p = YoutubeAudioDownloader.getProjectpath(); //Default path for save is project root path
		String f = "mp3";
		String q = "0";
		String l = "--no-playlist";
		String n = "%(title)s.%(ext)s"; 
		String c = "Download link automatically"; 
		
		Config.addAcceptableList("https://www.youtu");
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(
				YoutubeAudioDownloader.getProjectpath() + File.separator + "config.txt")))) {

			String p1 = Optional.of(br.readLine()).orElse("SavePath=" + p)				.split("=")[1];
			String f1 = Optional.of(br.readLine()).orElse("Format=" + f)				.split("=")[1];
			String q1 = Optional.of(br.readLine()).orElse("Quality=" + q)				.split("=")[1];
			String l1 = Optional.of(br.readLine()).orElse("Playlist=" + l)				.split("=")[1];
			String n1 = Optional.of(br.readLine()).orElse("FileNameFormat=" + n)		.split("=")[1];
			String c1 = Optional.of(br.readLine()).orElse("ClipboardListenOption=" + c)	.split("=")[1];

			p = p1.equals("null") ?  p : p1;
			f = f1.equals("null") ?  f : f1;
			q = q1.equals("null") ?  q : q1;
			l = l1.equals("null") ?  l : l1;
			n = n1.equals("null") ?  n : n1;
			c = c1.equals("null") ?  c : c1;
			
			
			String s;
			while((s = br.readLine()) != null) {
				if(s.equals("") || s.startsWith("#") || s.equals("https://www.youtu")) continue;
				Config.addAcceptableList(s);
			}
			
		} catch (FileNotFoundException e1) {

			GUI.warning("config.txt not exists!","%e%\nDon't worry! I'll make one later...", e1);

		} catch (NullPointerException e2) {
			
			GUI.warning("config.txt has no or invalid data!", "NullPointerException : %e%\nI'll initiate config.txt with default...", e2);

		} catch (Exception e) {

			GUI.warning("Exception occurred when reading config.txt", "%e%\nI'll initiate config.txt with default...", e);

		} finally {
			
			Config.setSaveto(p);
			Config.setFormat(f);
			Config.setQuality(q);
			Config.setPlaylistOption(l);
			Config.setFileNameFormat(n);
			Config.setClipboardListenOption(c);
			
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
				YoutubeAudioDownloader.getProjectpath() + File.separator + "config.txt"), false))) {

			File cfg = new File(
					YoutubeAudioDownloader.getProjectpath() + File.separator + "config.txt");
			
			if (!cfg.exists()) cfg.createNewFile();

			bw.write("SavePath=" + Config.getSaveto() + "\n");
			bw.write("Format=" + Config.getFormat() + "\n");
			bw.write("Quality=" + Config.getQuality() + "\n");
			bw.write("Playlist=" + Config.getPlaylistOption().toComboBox() + "\n");
			bw.write("FileNameFormat=" + Config.getFileNameFormat() + "\n");
			bw.write("ClipboardListenOption=" + Config.getClipboardListenOption() + "\n");
			
			bw.newLine();
			bw.write("#If you know a type of link that youtube-dl accepts (listed in https://github.com/ytdl-org/youtube-dl/blob/master/docs/supportedsites.md),\n");
			bw.write("#and wish YoutubeAudioDownloader detact & download it, you can write how does the link starts(e.g. in youtube, \"https://www.youtu\")\n");
			bw.write("#Every line starting with # will be ignored, but DO NOT CHANGE lines before these comments.\n");
			bw.write("#If you want to modify those, please do it in YoutubeAudioDownloader GUI,\n");
			bw.write("#and let my spaghetti handle that hardcoded shit. :)\n");
			bw.newLine();
			
			bw.write(Config.getAcceptedLinkStr());
			bw.flush();
			
			Main.logProperties("\n\nFinal");
			
		} catch (IOException e) {

			GUI.error("Error when writing config.txt file", "%e%", e);

		} 

	}
	
	public static void logProperties(String status) {
		
		Main.log(status + Config.status());
		
	}



	public static void log(String data) {

		loggerQueue.offer(() -> {
			logTo.println(data);
		});
		
	}
	
	public static void log(Exception e) {
		
		loggerQueue.offer(() -> {
			e.printStackTrace(logTo);
		});
		
	}

	
	public static ExecutorService getExecutorservice() {
		return executorService;
	}

	
	/*
	 * Kills the application NOW.
	 * 
	 * */
	public static void kill(int exitcode) {
		
		log("YoutubeAudioAutoDownloader exit code : " + exitcode);
		
		if (executorService != null && !executorService.isShutdown()) executorService.shutdownNow();

		Main.writeProperties();
		
		LoggerThread.isStop = true;
		
		try {
			logger.join(5000);
		} catch (InterruptedException e) {
			e.printStackTrace(logTo);
		}
		
		logTo.close();
		
		gui.disposeAll();
		
		System.exit(exitcode);
		
	}


	public static void webBrowse(String link) {

		try {
			if(Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(link));
			} else {
          		try {
              		Runtime.getRuntime().exec("xdg-open " + link);
           		} catch (IOException e) {
               		throw new IOException("Desktop.isDesktopSupported() is false and xdg-open doesn't work");
            	}
			}

		} catch (IOException e) {
			GUI.warning("Cannot open default web browser!", "Please visit" + link + "\n%e%", e);
		} catch (URISyntaxException e) {
			GUI.error("Invalid url!", link + "\n%e%", e);
		}

	}
	
	private static class LoggerThread extends Thread {

		public static volatile boolean isStop = false;
		
		public LoggerThread() {

			super(() -> {

				while(true) {
					
					if(loggerQueue.isEmpty() && isStop) {
						return;
					}
					
					 try {
					 	 loggerQueue.take().run();
					} catch (InterruptedException e) {
						 logTo.println("LoggerThread Interrupted! : " + e.getMessage());
					}
				}

			});
			
		}
		
	}

	public static void openConfig() {

		File f = new File(YoutubeAudioDownloader.getProjectpath() + File.separator + "config.txt");
		try {
			Desktop.getDesktop().open(f);
		} catch (IOException e) {
			GUI.warning("Cannot open default text file editor!", "Please open" + f.getAbsolutePath() + "\n%e%", e);
		}
		
	}

}


