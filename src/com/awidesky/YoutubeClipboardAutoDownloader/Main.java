package com.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.awidesky.YoutubeClipboardAutoDownloader.enums.ClipBoardOption;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.ExitCodes;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.LoadingStatus;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;
import com.awidesky.YoutubeClipboardAutoDownloader.util.Logger;
import com.awidesky.YoutubeClipboardAutoDownloader.util.LoggerThread;
import com.awidesky.YoutubeClipboardAutoDownloader.util.SwingDialogs;
import com.awidesky.YoutubeClipboardAutoDownloader.util.TaskLogger;
import com.awidesky.YoutubeClipboardAutoDownloader.util.workers.ClipBoardCheckerThread;
import com.awidesky.YoutubeClipboardAutoDownloader.util.workers.TaskThreadPool;

/** Main class */
public class Main {
	
	private static LoggerThread loggerThread = new LoggerThread();
	private static TaskLogger logger = loggerThread.getLogger("[Main] ");

	private static String clipboardBefore = "";
	private static ClipBoardCheckerThread clipChecker;
	
	public static final Charset NATIVECHARSET = Charset.forName(System.getProperty("native.encoding"));
	
	public static volatile boolean audioMode = true;
	
	private static GUI gui = new GUI();
	private static Function<String, TaskLogger> taskLogGetter;
	
	private static volatile int taskNum = 0;
	
	public static final String version = "v2.0.0";

	public static void main(String[] args) {
		
		boolean verbose = false, datePrefix = false, logbyTask = false;
		for (String arg : args) {
			if ("--help".equals(arg)) {
				System.out.println("usage : java -jar YoutubeAudioAutoDownloader " + version + ".jar [options]");
				System.out.println();
				System.out.println("options :");
				System.out.println("\t--logbyTask : Log lines from a task is gathered till the task is done/terminated.");
				System.out.println("\t              Useful when you don't want to see dirty log file when multiple tasks running.");
				System.out.println("\t--logTime : Every log line will printed with time");
				System.out.println("\t--verbose : Print verbose logs(like GUI Frmaes info, etc.)");
				System.out.println(); System.out.println();
				System.out.println("exit codes :");
				Arrays.stream(ExitCodes.values()).forEach(code -> {
					System.out.println("\t" + code.getCode() + " : " + code.getMsg());
				});
				Main.kill(ExitCodes.SUCCESSFUL);
			} else if ("--verbose".equals(arg)) {
				verbose = true;
			} else if ("--logTime".equals(arg)) {
				datePrefix = true;
			} else if ("--logbyTask".equals(arg)) {
				logbyTask = true;
			} else {
				System.err.println("Unknown option : \"" + arg + "\"");
			}
				
		}
		
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e) {
				SwingDialogs.error("Error while setting window look&feel", "%e%", e, false);
			}
		});
		
		prepareLogFile(verbose, datePrefix, logbyTask);
		setup(args);
		
		if(args.length == 0) return;
		
	}
	
	/**
	 * @return Whether the procedure went fine
	 * */
	private static void setup(String[] args) {

		try {
			SwingUtilities.invokeAndWait(() -> {
				gui.initLoadingFrame();
			});
		} catch (InvocationTargetException | InterruptedException e2) {
			logger.log("[init] failed wating EDT!");
			logger.log(e2);
			Main.kill(ExitCodes.INITLOADINGFRAMEFAILED);
		}
		
		
		gui.setLoadingStat(LoadingStatus.PREPARING_THREADS);
		loggerThread.start();
		TaskThreadPool.setup();
		clipChecker = new ClipBoardCheckerThread();
		clipChecker.start(); //A daemon thread that will check clipboard

		gui.setLoadingStat(LoadingStatus.CHECKING_FFMPEG);
		if (!YoutubeAudioDownloader.checkFfmpeg()) Main.kill(ExitCodes.FFMPEGNOTEXISTS);
		gui.setLoadingStat(LoadingStatus.CHECKING_YDL);
		if (!YoutubeAudioDownloader.checkYoutubedl()) Main.kill(ExitCodes.YOUTUBEDNOTEXISTS);
		
		gui.setLoadingStat(LoadingStatus.READING_PROPERTIES);
		readProperties();

		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener((e) -> { checkClipBoard(); }); 

		gui.setLoadingStat(LoadingStatus.LOADING_WINDOW);
		SwingUtilities.invokeLater(() -> {
			gui.initMainFrame();
		});
		
		logger.newLine();
		logger.log("Listening clipboard...\n");
		
	}
	
	private static void checkClipBoard() {

		if (Config.getClipboardListenOption() == ClipBoardOption.NOLISTEN) {
			logger.log("[debug] clipboard ignored due to ClipboardListenOption == \"" + ClipBoardOption.NOLISTEN.getString() + "\"");
			return;
		}
		clipChecker.submit(() -> { // To avoid overhead in EDT, put task in clipCheckerThread.

			try {

				Thread.sleep(50);

				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				if (!cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
					logger.log("[debug] Non-String Clipboard input!");
					logger.log("[debug] clipboard data Flavor(s) : " + Arrays.stream(cb.getAvailableDataFlavors()).map(DataFlavor::getHumanPresentableName).collect(Collectors.joining(", ")));
					logger.log("[debug] These can be represented as :");
					Arrays.stream(cb.getAvailableDataFlavors()).map(t -> {
						try {
							return cb.getData(t);
						} catch (UnsupportedFlavorException | IOException e) {
							logger.log(e);
							return null;
						}
					}).filter(o -> o != null).forEach(o -> logger.log("[debug] \t" + o));
					return;
				}
				final String data = (String)cb.getData(DataFlavor.stringFlavor);
				cb.setContents(new StringSelection(data), null);
				
				logger.log("[debug] clipboard : " + data);
				
				if (clipboardBefore.equals(data)) {
					logger.logVerbose("[debug] Duplicate input, ignore this input.");
					clipboardBefore = "";
					return;
				}
				clipboardBefore = data;

				if (!Config.isLinkAcceptable(data)) {
					logger.log("[debug] " + data + " is not acceptable!");
					return;
				}
				
				if (Config.getClipboardListenOption() == ClipBoardOption.ASK) {

					if (!SwingDialogs.confirm("Download link from clipboard?", "Link : " + data)) {

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
			} catch (Exception ee) {
				SwingDialogs.error("Unexpected clipboard error!", "%e%", ee, true);
			}

		});

	}
	
	private static void submitDownload(String data) {

		int num = taskNum++;
		TaskLogger logTask = getTaskLogger("[Task" + num + "] ");
		logTask.log("Received a link from your clipboard : " + data);

		TaskData t = new TaskData(num, logTask, Main.audioMode);
		
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
	
	private static void prepareLogFile(boolean verbose, boolean datePrefix, boolean logbyTask) {
		
		try {
			
			File logFolder = new File(YoutubeAudioDownloader.getProjectpath() + File.separator + "logs");
			File logFile = new File(logFolder.getAbsolutePath() + File.separator + "log-" + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + ".txt");
			logFolder.mkdirs();
			logFile.createNewFile();
			
			loggerThread.setLogDestination(new FileOutputStream(logFile), true);
			
		} catch (IOException e) {
			loggerThread.setLogDestination(System.out, true);
			SwingDialogs.error("Error when creating log flie, log in console instead...", "%e%", e, false);
		} finally {
			taskLogGetter = logbyTask ? loggerThread::getBufferedLogger : loggerThread::getLogger;
			loggerThread.setVerboseAllChildren(verbose);
			if (datePrefix) loggerThread.setDatePrefixAllChildren(new SimpleDateFormat("[kk:mm:ss]"));
			SwingDialogs.setLogger(loggerThread.getLogger());
		}
		
	}
	
	public static TaskLogger getTaskLogger(String prefix) {
		return taskLogGetter.apply(prefix);
	}
	public static TaskLogger getLogger(String prefix) {
		return loggerThread.getLogger(prefix);
	}
	
	/**
	 * note - this method should be Exception-proof.
	 * */
	private static void readProperties() {

		String p = Config.getSaveto();
		String f = Config.getFormat();
		String q = Config.getSaveto();
		String l = Config.getPlaylistOption().toCommandArgm();
		String n = Config.getFileNameFormat(); 
		String c = Config.getClipboardListenOption().getString(); 
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(
				YoutubeAudioDownloader.getProjectpath() + File.separator + "config.txt")))) {

			p = Optional.ofNullable(br.readLine()).orElse("SavePath=" + p)				.split(Pattern.quote("="))[1];
			f = Optional.ofNullable(br.readLine()).orElse("Format=" + f)				.split(Pattern.quote("="))[1];
			q = Optional.ofNullable(br.readLine()).orElse("Quality=" + q)				.split(Pattern.quote("="))[1];
			l = Optional.ofNullable(br.readLine()).orElse("Playlist=" + l)				.split(Pattern.quote("="))[1];
			n = Optional.ofNullable(br.readLine()).orElse("FileNameFormat=" + n)		.split(Pattern.quote("="))[1];
			c = Optional.ofNullable(br.readLine()).orElse("ClipboardListenOption=" + c)	.split(Pattern.quote("="))[1];
			
			String s;
			while((s = br.readLine()) != null) {
				if(s.equals("") || s.startsWith("#") || Config.isLinkAcceptable(s)) continue;
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
			bw.write("#If you want to modify those configurations, please do it in YoutubeAudioDownloader app."); bw.newLine();
			bw.write("#And let my hardcoded spaghetti shit handle that. :)"); bw.newLine();
			bw.newLine();
			
			bw.write(Config.getAcceptedLinkStr());
			bw.flush();
			
			logger.newLine();
			logProperties(logger, "Final properties :");
			
		} catch (IOException e) {

			SwingDialogs.error("Error when writing config.txt file", "%e%", e,  true);

		} 

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
	public static void kill(ExitCodes exitCode) {
		
		TaskThreadPool.kill();

		writeProperties();

		if(logger != null) {
			logger.log("YoutubeAudioAutoDownloader exit code : " + exitCode.getCode());
			logger.close();
		}
		
		loggerThread.kill(2500);
		
		System.exit(exitCode.getCode());
		
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


