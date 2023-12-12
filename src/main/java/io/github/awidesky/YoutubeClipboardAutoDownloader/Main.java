package io.github.awidesky.YoutubeClipboardAutoDownloader;

import java.awt.Desktop;
import java.awt.Toolkit;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.ExitCodes;
import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.LoadingStatus;
import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;
import io.github.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;
import io.github.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec.OSUtil;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.workers.ClipBoardListeningThread;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.workers.TaskThreadPool;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.LoggerThread;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.guiUtil.TaskLogger;

/** Main class */
public class Main {

	private static LoggerThread loggerThread = new LoggerThread();
	private static TaskLogger logger = loggerThread.getLogger("[Main] ");

	private static ClipBoardListeningThread clipChecker;
	
	public static final Charset NATIVECHARSET = Charset.forName(System.getProperty("native.encoding"));
	
	public static volatile AtomicBoolean audioMode = new AtomicBoolean(true);
	
	private static GUI gui = new GUI();
	private static Function<String, TaskLogger> taskLogGetter;
	
	private static volatile int taskNum = 0;
	private static String[] ytdlpAdditionalOptions = new String[0];
	
	public static final String version = "v2.0.0";

	public static void main(String[] args) {
		
		boolean verbose = false, datePrefix = false, logbyTask = false, logOnConsole = false;
		for (String arg : args) {
			if ("--help".equals(arg)) {
				System.out.println("YoutubeClipboardAutoDownloader " + version);
				System.out.println("Copyright (c) 2020-2023. Eugene Hong. All Rights Reserved.");
				System.out.println();
				System.out.println("Usage : java -jar YoutubeClipboardAutoDownloader " + version + ".jar [options]");
				System.out.println();
				System.out.println("Options :");
				System.out.println("\t--help : show this help info.");
				System.out.println("\t--version : show version info.");
				System.out.println("\t--logbyTask : Logs from a task is gathered till the task is done/terminated.");
				System.out.println("\t              Useful when you don't want to see dirty log file when multiple tasks running.");
				System.out.println("\t--logTime : Log with TimeStamps");
				System.out.println("\t--logOnConsole : Write log in command line console, not in a log file.");
				System.out.println("\t--verbose : Print verbose logs(like GUI Windows or extra debug info, etc.)");
				System.out.println("\t--ytdlpArgs=<options...> : Add additional yt-dlp options(listed at https://github.com/yt-dlp/yt-dlp#usage-and-options)");
				System.out.println("\t                           that will be appended at the end(but before the url) of yt-dlp execution.");
				System.out.println("\t                           If your options contains space, wrap them with \"\"");
				System.out.println("\t                           If you need multiple options, wrap them with \"\"");
				System.out.println(); System.out.println();
				System.out.println("exit codes :");
				Arrays.stream(ExitCodes.values()).forEach(code -> {
					System.out.printf("\t%3d : %s\n", code.getCode(), code.getMsg());
				});
				Main.kill(ExitCodes.SUCCESSFUL);
			} else if ("--version".equals(arg)) {
				System.out.println("YoutubeClipboardAutoDownloader " + version);
				System.out.println("Copyright (c) 2020-2023 Eugene Hong. All Rights Reserved.");
				System.out.println();
				System.out.println("This software is distributed under MIT licence.");
				System.out.println("Please refer to : https://github.com/awidesky/Youtube-Audio-Auto-Downloader4J/blob/master/LICENSE");
				Main.kill(ExitCodes.SUCCESSFUL);
			} else if ("--verbose".equals(arg)) {
				verbose = true;
			} else if ("--logTime".equals(arg)) {
				datePrefix = true;
			} else if ("--logbyTask".equals(arg)) {
				logbyTask = true;
			} else if ("--logOnConsole".equals(arg)) {
				logOnConsole = true;
			} else if (arg.startsWith("--ytdlpArgs")) {
				ytdlpAdditionalOptions = arg.split("=")[1].split(" ");
			} else {
				System.err.println("Invaild option : \"" + arg + "\"");
				System.err.println("If you want to find usage, use --help");
				Main.kill(ExitCodes.INVALIDCOMMANDARGS);
			}
				
		}
		
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e) {
				SwingDialogs.error("Cannot use native system look&feel", "%e%", e, false);
			}
		});
		
		prepareLogFile(verbose, datePrefix, logbyTask, logOnConsole);
		setup();
		
		if(args.length == 0) return;
		
	}
	
	private static void setup() {

		/** Set Default Uncaught Exception Handlers */
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			try {
				SwingDialogs.error("Unhandled exception in thread " + t.getName() + " : " + ((Exception)e).getClass().getName(), "%e%", (Exception)e , true);
				Main.kill(ExitCodes.UNKNOWNERROR);
			} catch(Exception err) {
				err.printStackTrace();
			}
		});
		SwingUtilities.invokeLater(() -> {
			Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
				try {
					SwingDialogs.error("Unhandled exception in EDT : " + ((Exception) e).getClass().getName(), "%e%", (Exception) e, true);
					Main.kill(ExitCodes.EDTFAILED);
				} catch (Exception err) {
					err.printStackTrace();
				}
			});
		});

		try {
			SwingUtilities.invokeAndWait(gui::initLoadingFrame);
			SwingUtilities.invokeAndWait(() -> gui.setLoadingStat(LoadingStatus.PREPARING_THREADS));
			loggerThread.start();
			TaskThreadPool.setup();
			clipChecker = new ClipBoardListeningThread(OSUtil.isMac() ? 150 : -1); // A daemon thread that will keep checking clipboard
			Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(clipChecker::submit);
			logger.newLine();
			logger.log("Listening clipboard...\n");

			SwingUtilities.invokeAndWait(() -> gui.setLoadingStat(LoadingStatus.CHECKING_FFMPEG));
			if (!YoutubeClipboardAutoDownloader.checkFfmpeg())
				Main.kill(ExitCodes.FFMPEGNOTEXISTS);
			
			SwingUtilities.invokeAndWait(() -> gui.setLoadingStat(LoadingStatus.CHECKING_YTDLP));
			if (!YoutubeClipboardAutoDownloader.checkYtdlp())
				Main.kill(ExitCodes.YOUTUBEDNOTEXISTS);

			SwingUtilities.invokeAndWait(() -> gui.setLoadingStat(LoadingStatus.READING_PROPERTIES));
			readProperties();

			SwingUtilities.invokeAndWait(() -> gui.setLoadingStat(LoadingStatus.LOADING_WINDOW));
			SwingUtilities.invokeAndWait(gui::initMainFrame);
			clipChecker.start();

		} catch (InterruptedException e1) {
			logger.log("[init] EDT failed while loading application!");
			SwingDialogs.error("EDT Interrupted!", "%e%", e1, true);
			Main.kill(ExitCodes.EDTFAILED);
		} catch (InvocationTargetException e2) {
			logger.log("[init] " + e2.getCause().getClass().getSimpleName() + " thrown while loading application!");
			SwingDialogs.error("Loading Failed!", "%e%", (Exception)e2.getCause(), true);
			Main.kill(ExitCodes.EDTFAILED);
		}

	}
	
	
	public static void submitDownload(String data) {

		int num = taskNum++;
		TaskLogger logTask = getTaskLogger("[Task" + num + "] ");
		logTask.log("Received a link from your clipboard : " + data);

		TaskData t = new TaskData(num, logTask, Main.audioMode.get());
		
		t.setUrl(data); 
		t.setVideoName(data); // temporarily set video name as url
		t.setDest(gui.getSavePath());
		t.setStatus("Validating...");
		
		if(TaskStatusModel.getinstance().isTaskExists(t) && !t.isFailed()) {
			// if duplicate task is also "Validating...", it is very likely that clipboard input was duplicated(e.g. long pressing the ctrl + c)
			if(TaskStatusModel.getinstance().isTaskExistsSameStatus(t)) return; 

			if(!SwingDialogs.confirm("Download same file in same directory?", data + "\nis already downloading/downloaded(by another Task) in\n" + Config.getSaveto() + "\ndownload anyway?")) {
				logTask.log(data + " is canceled because same task exists.");
				return;
			}
		}

		TaskStatusModel.getinstance().addTask(t);
		
		t.setFuture(TaskThreadPool.submit(() -> {

			String url = YoutubeClipboardAutoDownloader.ytdlpQuote + data + YoutubeClipboardAutoDownloader.ytdlpQuote;
			
			PlayListOption p = Config.getPlaylistOption();
			
			if (p == PlayListOption.ASK && data.contains("list=")) {
				p = (SwingDialogs.confirm("Download entire Playlist?", "PlayList Link : " + url)) ? PlayListOption.YES : PlayListOption.NO;
			}
					
			if (YoutubeClipboardAutoDownloader.validateAndSetName(url, t, p)) {
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
				YoutubeClipboardAutoDownloader.download(url, t, p, ytdlpAdditionalOptions);
			} else {
				t.failed();
				return;
			}

		}));
	}
	
	private static void prepareLogFile(boolean verbose, boolean datePrefix, boolean logbyTask, boolean logOnConsole) {
		try {
			if(logOnConsole) {
				loggerThread.setLogDestination(System.out, true);
			} else {
				File logFolder = new File(YoutubeClipboardAutoDownloader.getProjectpath() + File.separator + "logs");
				File logFile = new File(logFolder.getAbsolutePath() + File.separator + "log-" + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + ".txt");
				logFolder.mkdirs();
				logFile.createNewFile();
				loggerThread.setLogDestination(new FileOutputStream(logFile), true);
			}
		} catch (IOException e) {
			loggerThread.setLogDestination(System.out, true);
			SwingDialogs.error("Error when creating log flie, log in console instead...", "%e%", e, false);
		} finally {
			taskLogGetter = logbyTask ? loggerThread::getBufferedLogger : loggerThread::getLogger;
			loggerThread.setVerboseAllChildren(verbose);
			if (datePrefix) loggerThread.setDatePrefixAllChildren(new SimpleDateFormat("[kk:mm:ss.SSS]"));
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

		String p = Config.getDefaultSaveto();
		String f = Config.getDefaultFormat();
		String q = Config.getDefaultQuality();
		String l = Config.getDefaultPlaylistOption().toCommandArgm();
		String n = Config.getDefaultFileNameFormat(); 
		String c = Config.getDefaultClipboardListenOption().getString(); 
		
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(
				YoutubeClipboardAutoDownloader.getProjectpath() + File.separator + "config.txt")))) {

			UnaryOperator<String> read = str -> {
				try {
					return br.readLine().split(Pattern.quote("="))[1];
				} catch(Exception e) {
					SwingDialogs.warning("Exception occurred when reading config.txt", "%e%\nInitiate as default value : " + str, e, false);
					return str;
				}
			};
			
			p = read.apply(p); p = "%user.home%".equalsIgnoreCase(p) ? Config.getDefaultSaveto() : p;
			f = read.apply(f);
			q = read.apply(q);
			l = read.apply(l);
			n = read.apply(n);
			c = read.apply(c);
			
			String s;
			while((s = br.readLine()) != null) {
				if(s.equals("") || s.startsWith("#") || Config.isLinkAcceptable(s)) continue; //ignore if a line is empty, a comment or already registered
				Config.addAcceptableList(s);
			}
			
		} catch (FileNotFoundException e1) {
			SwingDialogs.warning("config.txt not exists!","%e%\nWill make one with default values later...", e1, false);
		} catch (Exception e) {
			SwingDialogs.error("Exception occurred when reading config.txt", "%e%\nInitiate as default values..", e, false);
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
	 * Writes properties to log file.
	 * This method should only invoked just before the termination of the program.
	 * */
	public static void writeProperties() {

		/** Write <code>properties</code> */
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
				YoutubeClipboardAutoDownloader.getProjectpath() + File.separator + "config.txt"), false))) {

			File cfg = new File(YoutubeClipboardAutoDownloader.getProjectpath() + File.separator + "config.txt");
			if (!cfg.exists()) cfg.createNewFile();

			UnaryOperator<String> savP = s -> Config.getDefaultSaveto().equalsIgnoreCase(s) ? "%user.home%" : s;
			
			bw.write("SavePath=" +	 savP.apply(Optional.ofNullable(Config.getSaveto())					.orElse(Config.getDefaultSaveto()))); 							bw.newLine();
			bw.write("Format=" + 				Optional.ofNullable(Config.getFormat())					.orElse(Config.getDefaultFormat()));							bw.newLine();
			bw.write("Quality=" +				Optional.ofNullable(Config.getQuality())				.orElse(Config.getDefaultQuality()));							bw.newLine();
			bw.write("Playlist=" + 				Optional.ofNullable(Config.getPlaylistOption())			.orElse(Config.getDefaultPlaylistOption()).toComboBox());		bw.newLine();
			bw.write("FileNameFormat=" + 		Optional.ofNullable(Config.getFileNameFormat())			.orElse(Config.getDefaultFileNameFormat()));					bw.newLine();
			bw.write("ClipboardListenOption=" + Optional.ofNullable(Config.getClipboardListenOption())	.orElse(Config.getDefaultClipboardListenOption()).getString());	bw.newLine();
			
			bw.newLine();
			bw.write("#If you know a type of link that yt-dlp accepts (listed in https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md),"); bw.newLine();
			bw.write("#and wish YoutubeAudioDownloader detect & download it, you can write how does the link starts(e.g. in youtube, \"https://www.youtu\")"); bw.newLine();
			bw.write("#Every line starting with # will be ignored, but DO NOT CHANGE lines before these comments."); bw.newLine();
			bw.write("#If you want to modify those configurations, please do it via the application."); bw.newLine();
			bw.write("#And let my hardcoded spaghetti shit handle that. :)"); bw.newLine();
			bw.newLine();
			
			bw.write(Config.getAcceptedLinkStr(System.lineSeparator()));
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
	 * This method can wait up to 5 seconds for <code>LoggerThread</code> and <code>executorService</code>(2.5seconds each) to terminated.
	 * */
	public static void kill(ExitCodes exitCode) {
		TaskThreadPool.kill(2500);
		writeProperties();
		if(logger != null) {
			logger.log("YoutubeClipboardAutoDownloader exit code : " + exitCode.getCode());
			logger.close();
		}
		loggerThread.shutdown(2500);
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
			SwingDialogs.warning("Cannot open default web browser!", "Please visit manually " + link + "\n%e%", e, false);
		} catch (URISyntaxException e) {
			SwingDialogs.error("Invalid url!", link + "\n%e%", e, false);
		}
	}
	

	public static void openConfig() {
		File f = new File(YoutubeClipboardAutoDownloader.getProjectpath() + File.separator + "config.txt");
		try {
			Desktop.getDesktop().open(f);
		} catch (IOException e) {
			SwingDialogs.warning("Cannot open default text file editor!", "Please open manullay " + f.getAbsolutePath() + "\n%e%", e, true);
		}
	}
	
	public static void openSaveFolder() {
		File f = new File(Config.getSaveto());
		try {
			Desktop.getDesktop().open(f);
		} catch (IOException e) {
			SwingDialogs.warning("Cannot open directory explorer!", "Please open manullay " + f.getAbsolutePath() + "\n%e%", e, true);
		}
	}

}