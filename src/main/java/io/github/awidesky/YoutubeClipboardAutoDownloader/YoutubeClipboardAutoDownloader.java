package io.github.awidesky.YoutubeClipboardAutoDownloader;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;
import io.github.awidesky.YoutubeClipboardAutoDownloader.gui.LogTextDialog;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.OSUtil;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec.ProcessExecutor;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec.ResourceInstaller;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec.YTDLPFallbacks;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.projectPath.JarPath;
import io.github.awidesky.projectPath.UserDataPath;

public class YoutubeClipboardAutoDownloader {


	private static final String projectPath = JarPath.getProjectPath(YoutubeClipboardAutoDownloader.class);
	private static final String appDataPath = UserDataPath.appLocalFolder("awidesky", "YoutubeClipboardAutoDownloader");
	private static String ytdlpPath;
	private static final Pattern percentPtn = Pattern.compile("[0-9]+\\.*[0-9]+%");
	private static final Pattern versionPtn = Pattern.compile("\\d{4}\\.\\d{2}\\.\\d{2}");
	private static final Pattern downloadFormatPtn = Pattern.compile("[\\s|\\S]+Downloading [\\d]+ format\\(s\\)\\:[\\s|\\S]+");
	private static final Pattern downloadIndexPtn = Pattern.compile("\\[download\\] Downloading item [\\d]+ of [\\d]+");
	
	public static final String ytdlpQuote = OSUtil.isWindows() ? "\"" : "";
	
	static {
		if(OSUtil.isWindows() || OSUtil.isLinux()) {
			ytdlpPath = appDataPath + File.separator + "ffmpeg" + File.separator + "bin" + File.separator;
		} else if(OSUtil.isMac()) {
			ytdlpPath = "/opt/homebrew/bin/";
		}
	}
	/**
	 * @return <code>true</code> if ffmpeg is found
	 * */
	public static boolean checkFFmpeg() {
		
		/* check ffmpeg */
		Logger log = Main.getLogger("[ffmepg check] ");

		// start process
		if (!checkFFmpegPath(ytdlpPath, log) && !checkFFmpegPath("", log)) {
			SwingDialogs.error("Error!", "no vaild ffmpeg installation in\n" + ytdlpPath + "\nor system %PATH%", null, true);
			String installPrompt = OSUtil.isMac() ? "Install ffmpeg via \"brew install ffmpeg\"?" : "Install ffmpeg in app resource folder?";
			if (ResourceInstaller.ffmpegAvailable() && SwingDialogs.confirm("ffmpeg installation invalid!", installPrompt)) {
				try {
					ResourceInstaller.getFFmpeg();
					log.log("ffmpeg installation success. re-checking ffmpeg...");
					return checkFFmpeg();
				} catch (Exception e) {
					if(e.getMessage().contains("denied")) { // "Permission denied" | "Access is denied"
						if(SwingDialogs.confirm("Error when installing ffmpeg : Access Denied!", "Change permission and try again?\n" + e.getLocalizedMessage())) {
							Path dir = Paths.get(appDataPath);
							log.log("Retry after change permission of : " + dir);
							return OSUtil.addDeletePermissionRecursive(dir, log) && checkFFmpeg();
						} else return false;
					}
					SwingDialogs.error("Failed to install ffmpeg! : " + e.toString(), "%e%", e, true);
					return false;
				}
			} else {
				Main.webBrowse("https://ffmpeg.org/download.html");
				return false;
			}
		}
		
		log.newLine();
		return true;
		
	}
	
	private static boolean checkFFmpegPath(String path, Logger log) {
		log.log("ffmpeg installation check command : \"" + path + "ffmpeg -version" + "\"");
		try {
			int ret = ProcessExecutor.runNow(log, null, path + "ffmpeg", "-version");
			log.log("ffmpeg installation check command terminated with exit code : " + ret);
			return ret == 0;
		} catch (Exception e) {
			log.log("Error when checking ffmpeg : " + e.getClass().getName());
			log.log(e);
			
			if(e.getMessage().contains("denied")) { // "Permission denied" | "Access is denied"
				if(SwingDialogs.confirm("Error when checking ffmpeg : Access Denied!", "Change permission and try again?\n" + e.getLocalizedMessage())) {
					log.log("Retry after changing executable permission...");
					OSUtil.addExecutePermission(Paths.get(path, "ffmpeg"), log);
					return checkFFmpegPath(path, log);
				} else return false;
			}
			return false;
		} finally { log.newLine(); }
	}
	
	/**
	 * @return Whether the procedure went fine
	 * */
	public static boolean checkYtdlp() {
		
		Logger log = Main.getLogger("[yt-dlp check] ");
		
		/* check yt-dlp */
		if (!checkYtdlpPath(ytdlpPath + "yt-dlp", log)) {
			if (checkYtdlpPath("yt-dlp", log)) {
				 ytdlpPath = "";
			} else {
				SwingDialogs.error("Error!", "no vaild yt-dlp installation in\n" + ytdlpPath + "\nor system %PATH%", null, true);
				if (ResourceInstaller.ytdlpAvailable() && SwingDialogs.confirm("yt-dlp installation invalid!", "Install yt-dlp in app resource folder?")) {
					try {
						ResourceInstaller.getYtdlp();
						log.log("yt-dlp installation success. re-checking yt-dlp...");
						return checkYtdlp();
					} catch (IOException | InterruptedException | ExecutionException e) {
						SwingDialogs.error("Failed to install yt-dlp! : " + e.getClass().getName(), "%e%", e, true);
						return false;
					}

				} else {
					Main.webBrowse("https://github.com/yt-dlp/yt-dlp#installation");
					return false;
				}
			}
		}
			
		log.log("projectPath = " + projectPath);
		log.log("ytdlpPath = " + (ytdlpPath.equals("") ? "system %PATH%" : ytdlpPath) + "\n");
		return true;
		
	}

	private static boolean checkYtdlpPath(String ydlfile, Logger log) {

		log.log("Check if yt-dlp path is in " + ydlfile);
		List<String> args = Arrays.asList(ydlfile, "--version");
		log.log("yt-dlp installation check command : \"" + args.stream().collect(Collectors.joining(" ")).trim() + "\"");

		StringBuffer stderr = new StringBuffer();
		
		// start process
		try {
			int ret = ProcessExecutor.run(args, null, br -> {
				String line = null;
				try {
					if (versionPtn.matcher(line = br.readLine()).matches()) { // valid path

						log.log("Found valid command to execute yt-dlp : \"" + ydlfile + "\"");
						log.log("yt-dlp version : " + line);

						if (checkYtdlpUpdateReleased(line, log)) { 
							log.log("yt-dlp update process start...");
							String[] updateCommands = getYtdlpUpdateCommands(ydlfile);
							LogTextDialog upDiag = new LogTextDialog(updateCommands, Main.getLogger("[yt-dlp update] "));
							log.log("Update yt-dlp with : " + Arrays.stream(updateCommands).collect(Collectors.joining(" ")));
							upDiag.setVisible(true);
							try {
								int e;
								if ((e = ProcessExecutor.runNow(upDiag.getLogger(), null, updateCommands)) != 0)
									throw new Exception("Error code : " + e);
							} catch (Exception e) {
								SwingDialogs.warning("Failed to update yt-dlp : " + e.getClass().getName(), "%e%\nCannot update yt-dlp!\nUsing version " + line + " instead...",
										e, true);
							} finally {
								try { Thread.sleep(3000); } catch (InterruptedException e) {
									e.printStackTrace();
								}
								upDiag.dispose();
							}
							
						}
					} else {
						SwingDialogs.error("Unexpected output from yt-dlp", line, null, true);
					}
				} catch (NumberFormatException e) {
					SwingDialogs.error("Unexpected output from yt-dlp : " + e.getClass().getName(), "%e%\nOutput : " + line, e, true);
				} catch (IOException e) {
					SwingDialogs.error("Error when redirecting output of yt-dlp : " + e.getClass().getName(), "%e%", e, true);
				}
			}, br -> br.lines().forEach(str -> {
				stderr.append(str).append('\n');
				log.log("yt-dlp stderr : " + str);
			})).wait_all();
			
			log.log("yt-dlp installation check command terminated with exit code : " + ret);
			if(!stderr.isEmpty()) SwingDialogs.error("yt-dlp Error! code : " + ret, stderr.toString(), null, false);
			return ret == 0;
		} catch (InterruptedException | IOException | ExecutionException e) {
			log.log("Error when checking yt-dlp : " + e.getClass().getName() + "\n" + e.getMessage());
			log.log(e);
			
			if(e.getMessage().contains("denied")) { // "Permission denied" | "Access is denied"
				if(SwingDialogs.confirm("Error when checking yt-dlp : Access Denied!", "Change permission and try again?\n" + e.getLocalizedMessage())) {
					log.log("Retry after changing executable permission...");
					OSUtil.addExecutePermission(Paths.get(ydlfile), log);
					return checkYtdlpPath(ydlfile, log);
				} else return false;
			}
			return false;
		} finally { log.newLine(); }

	}
	
	private static boolean checkYtdlpUpdateReleased(String ytdlpVersion, Logger log) {
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");
		LocalDate today = LocalDate.now();	
		LocalDate currentDay = LocalDate.parse(ytdlpVersion, dateFormat);
		
		log.log("Today : " + dateFormat.format(today) + ", current yt-dlp release day : " + ytdlpVersion);
		
		if(ChronoUnit.DAYS.between(currentDay, today) <= Config.getYtdlpUpdateDuration()) {
			log.log("yt-dlp version is not older than " + Config.getYtdlpUpdateDuration() + "days. update process skipped...");
			return false;
		}
		
		String message = "yt-dlp version is older than " + Config.getYtdlpUpdateDuration() + "days.";
		String releaseDate = ResourceInstaller.ytdlpLatestReleaseDate();
		if(releaseDate != null) {
			log.log("Latest yt-dlp release date from github : " + releaseDate);
			if (LocalDate.parse(releaseDate, dateFormat).isAfter(currentDay)) {
				message = "Latest yt-dlp version " + releaseDate + " is found.";
			} else {
				log.log("Latest : " + releaseDate + ", current : " + ytdlpVersion + ". Update is not needed...");
				return false;
			}
		} else log.log("Cannot find latest release date of yt-dlp!");

		if(!SwingDialogs.confirm("Update yt-dlp?", message + "\nUpdate yt-dlp?")) {
			log.log("User does not want to update it. update process skipped...");
			return false;
		}
		return true;
	}

	private static String[] getYtdlpUpdateCommands(String ydlfile) {
		if (ytdlpPath.contains("homebrew"))
			return new String[] { "/bin/zsh", "-c", "/opt/homebrew/bin/brew update; /opt/homebrew/bin/brew upgrade yt-dlp" };
		else
			return new String[] { ydlfile, "--update" };
	}

	public static String getYtdlpPath() { return ytdlpPath; }
	public static String getProjectPath() { return projectPath; }
	public static String getAppdataPath() { return appDataPath;	}

	/** get video name */
	public static boolean validateAndSetName(String url, TaskData task, PlayListOption playListOption) {
		try {
			Instant startTime = Instant.now();
			
			LinkedList<String> args = new LinkedList<>(Arrays.asList(new String[] { ytdlpPath + "yt-dlp", "--ffmpeg-location", ytdlpPath, "--get-filename", "-o",
					ytdlpQuote + Config.getFileNameFormat().replace("%(ext)s", Config.getFormat()) + ytdlpQuote, url }));
			if(playListOption.toCommandArgm() != null) args.add(1, playListOption.toCommandArgm());
			
			// retrieve command line argument
			task.logger.newLine(); task.logger.newLine();
			task.logger.log("[validating] Video name command : \"" + args.stream().collect(Collectors.joining(" ")) + "\"");

			// start process
			ProcessExecutor.ProcessHandle p1 = ProcessExecutor.run(args, null, br -> {
				try {
					String name = br.readLine();
					task.logger.log("[validating] yt-dlp stdout : " + name);
					if (playListOption == PlayListOption.YES) {
						int vdnum = 1;
						String str;
						while ((str = br.readLine()) != null) {
							task.logger.log("[validating] yt-dlp stdout : " + str);							
							vdnum++;
						}
						
						if(vdnum > 1) {
							name = "\"" + name + "\" and whole playlist";// check numbers instead
							task.setTotalNumVideo(vdnum);
						}
					}
					task.setVideoName(String.valueOf(name));
				} catch (IOException e) {
					SwingDialogs.error("Error when getting video name", "[Task" + task.getTaskNum() + "|validating] " + e.getClass().getName() + " : %e%", e, true);
				}
			}, br -> {
				try {
					StringBuilder warning = new StringBuilder();
					StringBuilder error = new StringBuilder();
					br.lines().forEach(l -> {
						task.logger.log("[validating] yt-dlp stderr : " + l);
						(l.startsWith("WARNING") ? warning : error).append(l).append("\n");
					});
					if(!warning.isEmpty()) SwingDialogs.warning("Warning when getting video name", "[Task" + task.getTaskNum() + "|validating]\n" + warning.toString(), null, true);
					if(!error.isEmpty()) {
						task.addErrorMessage(error.toString().strip());
						SwingDialogs.error("Error when getting video name", "[Task" + task.getTaskNum() + "|validating]\n" + error.toString(), null, true);
					}
				} catch (UncheckedIOException e1) {
					IOException e = e1.getCause();
					SwingDialogs.error("Error when getting video name", "[Task" + task.getTaskNum() + "|validating] " + e.getClass().getName() + " :  %e%", e, true);
				}
			});
			task.setProcess(p1.getProcess());
			int exit = p1.wait_all();
			Duration diff = Duration.between(startTime, Instant.now());
			task.logger.log("[validating] Video name : " + task.getVideoName());
			task.logger.log("[validating] Terminated with exit code : " + exit);
			task.logger.log("[validating] Elapsed time in validating link and downloading video name : " + String.format("%d min %d.%03d sec",
					diff.toMinutes(), diff.toSecondsPart(), diff.toMillisPart()));
			if (exit != 0) {
				task.failed();
				return false;
			} else { return true; }
		} catch (InterruptedException | IOException | ExecutionException e) {
			SwingDialogs.error("Error when getting video name", "[Task" + task.getTaskNum() + "|validating] " + e.getClass().getName() + " : %e%", e, true);
		}
		return false;
	}
	
	
	public static void download(String url, TaskData task, PlayListOption playListOption, String... additianalOptions)  {

		task.logger.newLine();
		Main.logProperties(task.logger, "[preparing] Current properties :");

		/* download video */
		LinkedList<String> arguments = new LinkedList<>(Arrays.asList(
				ytdlpPath + "yt-dlp", "--newline", "--force-overwrites", playListOption.toCommandArgm(), "--ffmpeg-location", ytdlpPath, "--output", ytdlpQuote + Config.getFileNameFormat() + ytdlpQuote));
		
		if(task.isAudioMode()) {
			arguments.add("--extract-audio");
			arguments.add("--audio-format");
			arguments.add(Config.getFormat());
			arguments.add("--audio-quality");
			arguments.add(Config.getQuality());
		} else {
			arguments.add("-f");
			arguments.add(getVideoFormat(task, url));
		}
		arguments.add(url);
		
		if(additianalOptions.length != 0) arguments.addAll(arguments.size() - 1, Arrays.asList(additianalOptions));

		
		// retrieve command line argument
		task.logger.log("[downloading] Video download command : \"" + arguments.stream().collect(Collectors.joining(" ")) + "\"");

		// start process
		ProcessExecutor.ProcessHandle p = null;
		try {
			p = ProcessExecutor.run(arguments, new File(Config.getSaveto()), br -> {
				try {
					String line = null;
					boolean downloadVideoAndAudioSeparately = false, videoDownloadDone = false;
					
					while ((line = br.readLine()) != null) {
						if(!task.isAudioMode() && downloadFormatPtn.matcher(line).matches()) {
							downloadVideoAndAudioSeparately = line.contains("+");
						}
						
						if(downloadIndexPtn.matcher(line).matches()) {
							task.logger.newLine(); task.logger.newLine();
							Scanner sc = new Scanner(line);
							sc.useDelimiter("[^\\d]+");
							task.setNowVideoNum(sc.nextInt());
							task.setTotalNumVideo(sc.nextInt());
							task.setStatus("Initiating download (" + task.getNowVideoNum() + "/" + task.getTotalNumVideo() + ")"); //Initiating
							sc.close();
						}
						
						if(line.startsWith("[ExtractAudio]")) {
							task.setStatus("Extracting Audio (" + task.getNowVideoNum() + "/" + task.getTotalNumVideo() + ")");
						} else if(line.startsWith("[Merger]")) {
							task.setStatus("Merging Video and Audio (" + task.getNowVideoNum() + "/" + task.getTotalNumVideo() + ")");
						}
						
						Matcher m = percentPtn.matcher(line);
						if (m.find() && line.startsWith("[download]") && !line.contains("Destination: ")) {
							String percentStr = m.group().replace("%", "");
							task.setStatus("Downloading (" + task.getNowVideoNum() + "/" + task.getTotalNumVideo() + ")");
							int percent = (int)Math.round(Double.parseDouble(percentStr));
							if(downloadVideoAndAudioSeparately) {
								task.setProgress(((videoDownloadDone) ? 50 : 0) + percent/2);
							} else {
								task.setProgress(percent);
							}
							if(percent == 100 && !percentStr.contains(".")) videoDownloadDone = true;
						}
						task.logger.log("[downloading] yt-dlp stdout : " + line);
					}
				} catch (IOException e) {
					task.failed();
					SwingDialogs.error("Error [" + task.getVideoName() + "]", "[Task" + task.getTaskNum() + "|downloading] Error when redirecting output of yt-dlp : " + e.getClass().getName() + "\n%e%", e, true);
				}

			}, br -> {
				try {
					String line = null;
					List<String> errors = new ArrayList<>();
					List<String> warnings = new ArrayList<>();
					while ((line = br.readLine()) != null) {
						task.logger.log("[downloading] yt-dlp stderr : " + line);
						(line.startsWith("ERROR") ? errors : warnings).add(line);
					}

					if (Stream.concat(errors.stream(), warnings.stream()).flatMap(YTDLPFallbacks::runFixCommand).reduce(Boolean.FALSE, Boolean::logicalOr)) {
						task.setVideoName(task.getVideoName() + " (problem occurred but fixed, re-trying download)");
						if (task.getTotalNumVideo() == 1) { // not a PlayList?
							download(url, task, playListOption);
						} else {
							download(url, task, playListOption, "--playlist-start", String.valueOf(task.getNowVideoNum()));
						}
						return;
					}
					if (!errors.isEmpty()) {
						SwingDialogs.error("Error [" + task.getVideoName() + "]",
								"[Task" + task.getTaskNum() + "|downloading] There's error(s) in yt-dlp proccess!\n"
										+ errors.stream().collect(Collectors.joining("\n")), null, true);
					}
					if (!warnings.isEmpty()) {
						SwingDialogs.warning("Warning [" + task.getVideoName() + "]",
							"[Task" + task.getTaskNum() + "|downloading] There's warning(s) in yt-dlp proccess!\n"
									+ warnings.stream().collect(Collectors.joining("\n")), null, true);
					}
				} catch (Exception e) {
					task.failed();
					SwingDialogs.error("Error [" + task.getVideoName() + "]", "[Task" + task.getTaskNum() + "|downloading] Error when redirecting error output of yt-dlp : " + e.getClass().getName() + "\n%e%", e, true);
				}

			});
		} catch (IOException e1) {
			task.failed();
			SwingDialogs.error("Error [" + task.getVideoName() + "]", "[Task" + task.getTaskNum() + "|downloading] Couldn't start yt-dlp : " + e1.getClass().getName() + "\n%e%" , e1, true);
			return;
		}
		Instant starttime = p.getProcess().info().startInstant().orElseGet(Instant::now);
		p.getProcess().info().commandLine().ifPresent(line -> 
			task.logger.log("[process info] Executed process command : \"" + line + "\""));
		task.setProcess(p.getProcess());
		task.setStatus("Initiating download");
		task.setProgress(0);
		
		try {
			int errorCode = p.wait_all();
			task.logger.log("[process info] Process exit code : " + errorCode);
			p.getProcess().info().totalCpuDuration()
					.ifPresent(cputime -> task.logger.log("[process info] Process cpu time : " + String.format("%d min %d.%03d sec",
									cputime.toMinutes(), cputime.toSecondsPart(), cputime.toMillisPart())));
			
			Duration diff = Duration.between(starttime, Instant.now());
			if(errorCode != 0) { 
				SwingDialogs.error("Error in yt-dlp", "[Task" + task.getTaskNum() + "|downloading] yt-dlp has ended with error code : " + errorCode, null, true);
				task.logger.log("[downloading] elapsed time in downloading(failed) : " + String.format("%d min %d.%03d sec",
						diff.toMinutes(), diff.toSecondsPart(), diff.toMillisPart()));
				task.failed();
				return;
			} else { 
				task.logger.log("[downloaded] elapsed time in working(succeed) : " + String.format("%d min %d.%03d sec",
						diff.toMinutes(), diff.toSecondsPart(), diff.toMillisPart()));
				task.logger.log("[finished] Finished!\n");
				task.finished();
			}
		} catch (InterruptedException | ExecutionException e) {
			task.failed();
			SwingDialogs.error("Error [" + task.getVideoName() + "]", "[Task" + task.getTaskNum() + "|downloading] Failed to wait yt-dlp process : " + e.getClass().getName() + "\n%e%", e, true);
		}
		
	}


	private static String getVideoFormat(TaskData task, String url) {
		String  height = "", video = "[ext=" + Config.getFormat() + "]", audio = "";
		if("mp4".equals(Config.getFormat())) {
			audio = "[ext=m4a]";
		}
		if(!"best".equals(Config.getQuality())) {
			height = "[height<=" + Config.getQuality().replace("p", "") + "]";
		}
		return "bv" + video + height + "+ba" + audio + "/b" + video + height + " / bv*+ba/b";
	}

}
