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
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.awidesky.YoutubeClipboardAutoDownloader.enums.ClipBoardOption;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.LoadingStatus;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;
import com.awidesky.YoutubeClipboardAutoDownloader.util.Logger;
import com.awidesky.YoutubeClipboardAutoDownloader.util.LoggerThread;
import com.awidesky.YoutubeClipboardAutoDownloader.util.SwingDialogs;
import com.awidesky.YoutubeClipboardAutoDownloader.util.workers.ClipBoardCheckerThread;
import com.awidesky.YoutubeClipboardAutoDownloader.util.workers.TaskThreadPool;

/** Main class */
public class Main { 

	private static String clipboardBefore = "";
	private static boolean isSecondtime = false;
	private static ClipBoardCheckerThread clipChecker;
	
	public static final Charset NATIVECHARSET = Charset.forName(System.getProperty("native.encoding"));
	
	public static volatile boolean audioMode = true;
	
	private static GUI gui = new GUI();
	private static LoggerThread loggerThread;
	private static Logger logger;
	public static boolean verbose;
	public static boolean logbyTask;
	public static boolean logTime;
	
	private static volatile int taskNum = 0;
	
	public static final String version = "v1.7.0";

	public static void main(String[] args) {
		
		boolean setup = false;
		try {
			
			logbyTask = Arrays.stream(args).anyMatch("--logbyTask"::equals);
			logTime = Arrays.stream(args).anyMatch("--logTime"::equals);
			prepareLogFile();
			setup = setup(args);
			
		} catch (Exception e) {
			setup = false;
			SwingDialogs.error("Failed to initiate!", "Application Failed to initiate!\n%e%", e, true);
		}
		
		if(!setup) System.exit(1);
		
		if(args.length == 0) return;
		
		for (String arg : args) {
			if ("--help".equals(arg)) {
				System.out.println("usage : java -jar YoutubeAudioAutoDownloader " + version + ".jar [options]");
				System.out.println();
				System.out.println("options :");
				System.out.println("\t--logbyTask : Log lines from a task is gathered till the task is done/terminated.");
				System.out.println("\t              Useful when you don't want to see dirty log file when multiple tasks running.");
				System.out.println("\t--logTime : Every log line will printed with time");
				System.out.println("\t--verbose : Print verbose logs(like GUI Frmaes info, etc.");
			} else if ("--verbose".equals(arg)) {
				verbose = true;
			}
		}
	}
	
	/**
	 * @return Whether the procedure went fine
	 * */
	private static boolean setup(String[] args) {

		try {
			SwingUtilities.invokeAndWait(() -> {
				gui.initLoadingFrame();
			});
		} catch (InvocationTargetException | InterruptedException e2) {

			logger.log("[init] failed wating EDT!");
			logger.log(e2);
			return false;
		}

		gui.setLoadingStat(LoadingStatus.CHECKING_FFMPEG);
		if (!YoutubeAudioDownloader.checkFfmpeg()) return false;
		gui.setLoadingStat(LoadingStatus.CHECKING_YDL);
		if (!YoutubeAudioDownloader.checkYoutubedl()) return false;
		
		gui.setLoadingStat(LoadingStatus.READING_PROPERTIES);
		readProperties();

		
		gui.setLoadingStat(LoadingStatus.PREPARING_THREADS);
		clipChecker = new ClipBoardCheckerThread();
		clipChecker.start(); //A daemon thread that will check clipboard

		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener((e) -> { checkClipBoard(); }); 

		gui.setLoadingStat(LoadingStatus.LOADING_WINDOW);
		SwingUtilities.invokeLater(() -> {
			try {
				gui.initMainFrame();
			} catch (Exception e) {
				SwingDialogs.error("Failed to initiate!", "Application Failed to initiate!\n" + e.getClass().getName() + " : %e%", e, true);
				Main.kill(1);
			}
		});
		
		logger.newLine();
		logger.log("Listening clipboard...\n");
		return true;
		
	}
	
	private static void checkClipBoard() {

		if (Config.getClipboardListenOption() == ClipBoardOption.NOLISTEN) {
			logger.log("[debug] clipboard ignored due to ClipboardListenOption == \"" + ClipBoardOption.NOLISTEN.getString() + "\"");
			return;
		}

		clipChecker.submit(() -> { // To avoid overhead in EDT, put task in clipCheckerThread.

			try {

				Thread.sleep(50);

				final String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
						.getData(DataFlavor.stringFlavor); //what if file is selected?

				logger.log("[debug] clipboard : " + data);
				
				if (isRedundant(data)) return;
				
				clipboardBefore = data;

				if (!Config.isLinkAcceptable(data)) {
					logger.log("[debug] " + data + " is not acceptable!");
					return;
				}
				
				if (Config.getClipboardListenOption() == ClipBoardOption.ASK) {

					if (!SwingDialogs.confirm("Download link in clipboard?", "Link : " + data)) {

						logger.log("[GUI.linkAcceptChoose] Download link " + data + "? : " + false + "\n");

					} else {

						logger.log("[GUI.linkAcceptChoose] Download link " + data + "? : " + true + "\n");
						Arrays.stream(data.split(Pattern.quote("\n"))).forEach(Main::submitDownload);

					}

					return;
				}

				Arrays.stream(data.split(Pattern.quote("\n"))).forEach(Main::submitDownload);

			} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {

				SwingDialogs.error("Error when checking clipboard!", "%e%", e1, true);

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
		Logger logTask = loggerThread.getLogger("[Task" + num + "] ");
		logTask.log("Received a link from your clipboard : " + data);

		TaskData t = new TaskData(num, logTask);
		
		t.setUrl(data); 
		t.setVideoName(data); // temporarily set video name as url
		t.setDest(gui.getSavePath());
		t.setStatus("Validating...");
		
		if(TaskStatusModel.getinstance().isTaskExists(t)) {
			if(!TaskStatusModel.getinstance().isTaskDone(t)) {
				return;
			}
			if(!SwingDialogs.confirm("Download same file in same directory?", data + "\nis already downloaded (by another Task) in\n" + Config.getSaveto() + "\ndownload anyway?")) {
				logTask.log("is cancelled because same download exists.");
				return;
			}
		}

		TaskStatusModel.getinstance().addTask(t);
		
		
		t.setFuture(TaskThreadPool.submit(() -> {

			String url = "\"" + data + "\"";
			
			PlayListOption p = Config.getPlaylistOption();
			
			if (p == PlayListOption.ASK && data.contains("list=")) {
				p = (SwingDialogs.confirm("Download entire Playlist?", "PlayList Link : " + url)) ? PlayListOption.YES : PlayListOption.NO;
			}
					
			if (YoutubeAudioDownloader.validateAndSetName(url, t, p)) {

				t.setStatus("Preparing...");
				String save = t.getDest();
				File file = new File(save);
				if((file.exists() || file.mkdirs()) && file.isDirectory()) {
					Config.setSaveto(save);
				} else {
					t.failed();
					SwingDialogs.error("Download Path is invalid!", "Invalid path : " + save, null, false);
					return;
				}
				YoutubeAudioDownloader.download(url, t, p);

			} else {
				t.failed();
				SwingDialogs.error("[Task" + num + "|validating] Not a valid url!",	data + "\nis unvalid or unsupported url!", null, true);
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
			
			loggerThread = new LoggerThread(new FileOutputStream(logFile), true);

			/*if(logbyTask) {
				
				logger = new LoggerThread(new PrintWriter(new FileOutputStream(logFile), true)) {
					
					private Map<Integer, StringBuilder> tasklog = new HashMap<>();
					private Pattern numPtn = Pattern.compile("\\d+");
					private Pattern taskTerminatePtn = Pattern.compile(Pattern.quote("[Task") + "\\d+(" + Pattern.quote("|Finished]") + "|" + Pattern.quote("|Canceled]") + ")");

					@Override
					public void log(String data) {

						if(data.strip().startsWith("[Task")) {
							Matcher m = numPtn.matcher(data);
							m.find();
							int key = Integer.parseInt(m.group());
							tasklog.computeIfAbsent(key, s -> new StringBuilder()).append(data + "\n");
							if(taskTerminatePtn.matcher(data).find()) {
								super.log(tasklog.get(key).toString());
								tasklog.remove(key);
							}
						} else {
							super.log(data);
						}
						
					}
					
					@Override
					public void kill(int timeOut) {
						if(!tasklog.isEmpty()) {
							log("Following logs are from task(s) that are not done yet.");
							tasklog.values().stream().map(StringBuilder::toString).forEach(this::log);
						}
						super.kill(timeOut);
					}
					
				};
				
			} else {
				logger = new LoggerThread(new PrintWriter(new FileOutputStream(logFile), true));
			}*/
			
			if(logTime) {
				logger.setDatePrefix(new SimpleDateFormat("kk-mm-ss"));
			}
			
		} catch (IOException e) {

			loggerThread = new LoggerThread(System.out, true);
			SwingDialogs.error("Error when creating log flie", "%e%", e, false);
			
		} finally {
			SwingDialogs.setLogger(loggerThread.getLogger());
			logger = loggerThread.getLogger("[Main] ");
			loggerThread.start();
		}
		
	}

	public Logger getLogger() {
		
		Logger ret;
		if(logbyTask) {
			ret = loggerThread.getBufferedLogger(null);
		} else {
			ret = loggerThread.getLogger(null);
		}
		if(logTime) {
			ret.setDatePrefix(new SimpleDateFormat("kk-mm-ss"));
		}
		return ret;
		
	}
	
	/**
	 * note - this method should be Exception-proof.
	 * */
	private static void readProperties() {

		String p = YoutubeAudioDownloader.getProjectpath(); //Default path for save is project root path
		String f = "mp3";
		String q = "0";
		String l = "--no-playlist";
		String n = "%(title)s.%(ext)s"; 
		String c = "Download link automatically"; 
		
		Config.addAcceptableList("https://www.youtu");
		Config.addAcceptableList("youtube.com");
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(
				YoutubeAudioDownloader.getProjectpath() + File.separator + "config.txt")))) {

			String p1 = Optional.ofNullable(br.readLine()).orElse("SavePath=" + p)				.split(Pattern.quote("="))[1];
			String f1 = Optional.ofNullable(br.readLine()).orElse("Format=" + f)				.split(Pattern.quote("="))[1];
			String q1 = Optional.ofNullable(br.readLine()).orElse("Quality=" + q)				.split(Pattern.quote("="))[1];
			String l1 = Optional.ofNullable(br.readLine()).orElse("Playlist=" + l)				.split(Pattern.quote("="))[1];
			String n1 = Optional.ofNullable(br.readLine()).orElse("FileNameFormat=" + n)		.split(Pattern.quote("="))[1];
			String c1 = Optional.ofNullable(br.readLine()).orElse("ClipboardListenOption=" + c)	.split(Pattern.quote("="))[1];

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

			SwingDialogs.warning("config.txt not exists!","%e%\nDon't worry! I'll make one later...", e1, false);

		} catch (NullPointerException e2) {
			
			SwingDialogs.warning("config.txt has no or invalid data!", "NullPointerException : %e%\nI'll initiate config.txt with default...", e2, false);

		} catch (Exception e) {

			SwingDialogs.warning("Exception occurred when reading config.txt", "%e%\nI'll initiate config.txt with default...", e, false);

		} finally {
			
			Config.setSaveto(p);
			Config.setFormat(f);
			Config.setQuality(q);
			Config.setPlaylistOption(l);
			Config.setFileNameFormat(n);
			Config.setClipboardListenOption(c);
			
			logger.newLine();
			logProperties(logger, "Initial properties :");
			
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

			bw.write("SavePath=" + 				Optional.ofNullable(Config.getSaveto())						.orElse(YoutubeAudioDownloader.getProjectpath())); 	bw.newLine();
			bw.write("Format=" + 				Optional.ofNullable(Config.getFormat())						.orElse("mp3"));									bw.newLine();
			bw.write("Quality=" +				Optional.ofNullable(Config.getQuality())					.orElse("0"));										bw.newLine();
			bw.write("Playlist=" + 				Optional.ofNullable(Config.getPlaylistOption())				.orElse(PlayListOption.NO).toComboBox());			bw.newLine();
			bw.write("FileNameFormat=" + 		Optional.ofNullable(Config.getFileNameFormat())				.orElse("%(title)s.%(ext)s"));						bw.newLine();
			bw.write("ClipboardListenOption=" + Optional.ofNullable(Config.getClipboardListenOption())		.orElse(ClipBoardOption.AUTOMATIC).getString());	bw.newLine();
			
			bw.newLine();
			bw.write("#If you know a type of link that youtube-dl accepts (listed in https://github.com/ytdl-org/youtube-dl/blob/master/docs/supportedsites.md),"); bw.newLine();
			bw.write("#and wish YoutubeAudioDownloader detact & download it, you can write how does the link starts(e.g. in youtube, \"https://www.youtu\")"); bw.newLine();
			bw.write("#Every line starting with # will be ignored, but DO NOT CHANGE lines before these comments."); bw.newLine();
			bw.write("#If you want to modify those, please do it in YoutubeAudioDownloader GUI,"); bw.newLine();
			bw.write("#and let my spaghetti handle that hardcoded shit. :)"); bw.newLine();
			bw.newLine();
			
			bw.write(Config.getAcceptedLinkStr());
			bw.flush();
			
			logger.newLine();
			logProperties(logger, "Final properties :");
			
		} catch (IOException e) {

			SwingDialogs.error("Error when writing config.txt file", "%e%", e,  true);

		} 

	}
	
	public static Logger getLogger(String prefix) {
		return loggerThread.getLogger(prefix);
	}

	public static void logProperties(Logger logTo, String status) {
		logTo.log(status + Config.status());
	}

	
	public static void clearTasks() {
		try {
			if(SwingUtilities.isEventDispatchThread()) {
				TaskStatusModel.getinstance().clearAll();
			} else {
				SwingUtilities.invokeAndWait(TaskStatusModel.getinstance()::clearAll);
			}
		} catch (InvocationTargetException | InterruptedException e1) {
			SwingDialogs.error("Error when shutting GUI down!", "%e%", e1, false);;
		}
	}
	
	/**
	 * Kills the application.
	 * This method can wait up to 5seconds for <code>LoggerThread</code> and <code>executorService</code>(2.5seconds each) to terminated.
	 * 
	 * */
	public static void kill(int exitcode) {
		
		logger.log("YoutubeAudioAutoDownloader exit code : " + exitcode);
		
		TaskThreadPool.kill();

		writeProperties();
			
		loggerThread.kill(2500);
		
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
			SwingDialogs.warning("Cannot open default web browser!", "Please visit" + link + "\n%e%", e, false);
		} catch (URISyntaxException e) {
			SwingDialogs.error("Invalid url!", link + "\n%e%", e, false);
		}

	}
	

	public static void openConfig() {

		File f = new File(YoutubeAudioDownloader.getProjectpath() + File.separator + "config.txt");
		try {
			Desktop.getDesktop().open(f);
		} catch (IOException e) {
			SwingDialogs.warning("Cannot open default text file editor!", "Please open" + f.getAbsolutePath() + "\n%e%", e, true);
		}
		
	}
	
	public static void openSaveFolder() {
		
		File f = new File(Config.getSaveto());
		try {
			Desktop.getDesktop().open(f);
		} catch (IOException e) {
			SwingDialogs.warning("Cannot open directory explorer!", "Please open" + f.getAbsolutePath() + "\n%e%", e, true);
		}
		
	}

}


