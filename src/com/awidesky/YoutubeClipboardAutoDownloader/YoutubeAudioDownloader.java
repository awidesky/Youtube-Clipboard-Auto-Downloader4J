package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;
import com.awidesky.YoutubeClipboardAutoDownloader.util.Logger;
import com.awidesky.YoutubeClipboardAutoDownloader.util.SwingDialogs;
import com.awidesky.YoutubeClipboardAutoDownloader.util.exec.BinaryInstaller;
import com.awidesky.YoutubeClipboardAutoDownloader.util.exec.ProcessExecutor;
import com.awidesky.YoutubeClipboardAutoDownloader.util.exec.YTDLPFallbacks;

public class YoutubeAudioDownloader {


	private static String projectpath = getProjectRootPath();
	private static String youtubedlpath = projectpath + File.separator + "YoutubeAudioAutoDownloader-resources" + File.separator + "ffmpeg" + File.separator + "bin" + File.separator;
	private static Pattern percentPtn = Pattern.compile("[0-9]+\\.*[0-9]+%");
	private static Pattern versionPtn = Pattern.compile("\\d{4}\\.\\d{2}\\.\\d{2}");
	
	/**
	 * returns String represents the path to the running jar.
	 * */
	private static String getProjectRootPath() {
		String ret = new File("").getAbsolutePath();
		if (System.getProperty("jpackage.app-path") != null) {
			ret += File.separator + "app";
		}
		return ret;
	}
	
	/**
	 * @return Whether the procedure went fine
	 * */
	public static boolean checkFfmpeg() {
		
		/* check ffmpeg */
		Logger log = Main.getLogger("[ffmepg check] ");

		// retrieve command line argument
		log.log("Checking ffmpeg installation by \"" + youtubedlpath + "ffmpeg -version" + "\"");

		// start process
		int ret = -1;
		try {
			ret = ProcessExecutor.runNow(log, null, youtubedlpath + "ffmpeg", "-version");
			log.log("Executing ffmpeg -version ended with exit code : " + ret);
		} catch (Exception e) {
			SwingDialogs.error("ffmpeg Error!", "%e%", e, true);
	 		if (SwingDialogs.confirm("ffmpeg does not exist!", "Install ffmpeg in app resource folder?")) {
	 			try {
					BinaryInstaller.getFFmpeg();
					log.log("ffmpeg installation success. re-checking ffmpeg...");
					return checkFfmpeg();
				} catch (IOException e1) {
					SwingDialogs.error("Failed to install ffmpeg!", "%e%", e1, true);
					return false;
				}
	 		} else {
	 			Main.webBrowse("https://ffmpeg.org/download.html");
	 			return false;
	 		}
		}
		
		log.newLine();
		if(ret != 0) SwingDialogs.error("ffmpeg Error!", "exit code : " + ret, null, true);
		return ret == 0;
		
	}
	
	/**
	 * @return Whether the procedure went fine
	 * */
	public static boolean checkYoutubedl() {
		
		Logger log = Main.getLogger("[Youtube-dl check] ");
		
		/* check youtube-dl */
		if (checkYoutubedlPath("youtube-dl", log)) {
			
			youtubedlpath = "";
			
		} else {
			
			if (!checkYoutubedlPath(youtubedlpath + "youtube-dl", log)) {
				
				SwingDialogs.error("Error!", "youtube-dl does not exist in\n" + youtubedlpath + "\nor system %PATH%", null, true);
				if (SwingDialogs.confirm("youtube-dl does not exist!", "Install youtube-dl in app resource folder?")) {
					try {
						BinaryInstaller.getYtdlp();
						log.log("youtube-dl installation success. re-checking youtube-dl...");
						return checkYoutubedl();
					} catch (IOException e) {
						SwingDialogs.error("Failed to install yt-dlp!", "%e%", e, true);
						return false;
					}

				} else {
					Main.webBrowse("http://ytdl-org.github.io/youtube-dl/download.html");
					return false;
				}
				
			}
			
		}
		
		log.log("projectpath = " + projectpath);
		log.log("youtubedlpath = " + (youtubedlpath.equals("") ? "system %PATH%" : youtubedlpath) + "\n");
		
		return true;
		
	}

	private static boolean checkYoutubedlPath(String ydlfile, Logger log) {

		log.log("Check if youtube-dl path is in " + ydlfile);
		List<String> args = Arrays.asList(ydlfile, "--version");
		log.log("Checking youtube-dl by \"" + args.stream().collect(Collectors.joining(" ")).trim() + "\"");

		StringBuffer stderr = new StringBuffer();
		
		// start process
		try {
			int ret = ProcessExecutor.run(args, null, br -> {
				String line = null;
				try {
					if (versionPtn.matcher(line = br.readLine()).matches()) { // valid path

						log.log("Found valid command to execute youtube-dl : " + ydlfile);
						log.log("youtube-dl version : " + line);

						if ((Integer.parseInt(new SimpleDateFormat("yyyyMM").format(new Date()))
								- Integer.parseInt(line.substring(0, 7).replace(".", ""))) > 1) {
							// update if yputube-dl version isolder than a month
							try {
								int e;
								if ((e = ProcessExecutor.runNow(log, null, ydlfile, "--update")) != 0)
									throw new Exception("Error code : " + e);
							} catch (Exception e) {
								SwingDialogs.warning("Failed to update youtube-dl", "%e%\nCannot update youtube-dl!\nUse version " + line + "instead...",
										e, true);
							}
							
						} else {
							log.log("youtube-dl version is not older than a month");
						}

					} else {
						SwingDialogs.error("Unexpected output from youtube-dl", line, null, true);
					}
				} catch (NumberFormatException e) {
					SwingDialogs.error("Unexpected output from youtube-dl","%e%\nOutput : " + line, null, true);
				} catch (IOException e) {
					SwingDialogs.error("Failed to grab youtube-dl output!","%e%", e, true);
				}
			}, br -> br.lines().forEach(str -> {
				stderr.append(str);
				stderr.append('\n');
				log.log(str);
			})).waitFor();
			
			log.log("Executing " + args.stream().collect(Collectors.joining(" ")).trim() + " ended with exit code : " + ret);
			if(!stderr.isEmpty()) SwingDialogs.error("youtube-dl Error! code : " + ret, stderr.toString(), null, false);
			return ret == 0;
		} catch (InterruptedException | IOException e) {
			log.log("Error when checking youtube-dl\n\t" + e.getMessage());
			return false;
		}

	}
	

	public static String getProjectpath() {
		return projectpath;
	}
	public static String getResourcePath() {
		return projectpath + File.separator + "YoutubeAudioAutoDownloader-resources";
	}
	public static String getYoutubedlPath() {
		return youtubedlpath;
	}



	/** get video name */
	public static boolean validateAndSetName(String url, TaskData task, PlayListOption playListOption) {
		
		try {
			Instant startTime = Instant.now();
			
			LinkedList<String> args = new LinkedList<>(Arrays.asList(new String[] {youtubedlpath + "youtube-dl", "--get-filename", "-o",
					"\"" + Config.getFileNameFormat().replace("%(ext)s", Config.getFormat()) + "\"", url
					}));
			if(playListOption.toCommandArgm() != null) args.add(2, playListOption.toCommandArgm());
			
			// retrieve command line argument
			task.logger.newLine(); task.logger.newLine();
			task.logger.log("[validating] Getting video name by \"" + args.stream().collect(Collectors.joining(" ")).trim() + "\"");

			// start process
			Process p1 = ProcessExecutor.run(args, null, br -> {
				try {
					String name = br.readLine();
					if (playListOption == PlayListOption.YES) {
						name = "\"" + name + "\" and whole playlist";
						int vdnum = 1;
						while (br.readLine() != null)
							vdnum++;
						task.setTotalNumVideo(vdnum);
					}
					task.setVideoName(name);
				} catch (IOException e) {
					SwingDialogs.error("Error in getting video name", "[Task" + task.getTaskNum() + "|validating] %e%", e, true);
				}
			}, br -> {
				String line;
				try {
					while ((line = br.readLine()) != null) {
						task.logger.log("[validating] youtube-dl stderr : " + line);
					}
				} catch (IOException e) {
					SwingDialogs.error("Error in getting video name", "[Task" + task.getTaskNum() + "|validating] %e%", e, true);
				}
			});
			task.setProcess(p1);
			int exit = p1.waitFor();
			Duration diff = Duration.between(startTime, Instant.now());
			task.logger.log("[validating] Video name : " + task.getVideoName());
			task.logger.log("[validating] Ended with exit code : " + exit);
			task.logger.log("[validating] elapsed time in validating link and downloading video name : " + String.format("%d min %d sec", 
                    diff.toMinutes(),
                    diff.toSecondsPart()));
			if (exit != 0) {
				task.failed();
				return false;
			} else { return true; }
		} catch (InterruptedException | IOException e) {
			SwingDialogs.error("Error in getting video name", "[Task" + task.getTaskNum() + "|validating] %e%", e, true);
		}
		return false;
	}
	
	
	public static void download(String url, TaskData task, PlayListOption playListOption, String... additianalOptions)  {

		task.logger.newLine();
		Main.logProperties(task.logger, "[preparing] Current properties :");

		/* download video */
		Instant startTime = Instant.now();
		
		LinkedList<String> arguments = new LinkedList<>(Arrays.asList(
				youtubedlpath + "youtube-dl", "--newline", "--force-overwrites", playListOption.toCommandArgm(), "--output", "\"" + Config.getFileNameFormat() + "\""));
		
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
		
		if(additianalOptions.length != 0) arguments.addAll(1, Arrays.asList(additianalOptions));

		
		// retrieve command line argument
		task.logger.log("[downloading] Downloading video by \"" + arguments.stream().collect(Collectors.joining(" ")) + "\"");

		// start process
		Process p = null;
		try {
			p = ProcessExecutor.run(arguments, new File(Config.getSaveto()), br -> {
				try {

					String line = null;
					boolean downloadVideoAndAudioSeparately = false, videoDownloadDone = false;
					
					while ((line = br.readLine()) != null) {
						
						if(!task.isAudioMode() && line.matches("[\\s|\\S]+Downloading [\\d]+ format\\(s\\)\\:[\\s|\\S]+")) {
							downloadVideoAndAudioSeparately = line.contains("+");
						}
						
						if(line.matches("\\[download\\] Downloading item [\\d]+ of [\\d]+")) {
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
						if (m.find() && line.contains("ETA")) {
							task.setStatus("Downloading (" + task.getNowVideoNum() + "/" + task.getTotalNumVideo() + ")");
							int num = (int)Math.round(Double.parseDouble(m.group().replace("%", "")));
							if(downloadVideoAndAudioSeparately) {
								task.setProgress(((videoDownloadDone) ? 50 : 0) + num/2);
							} else {
								task.setProgress(num);
							}
							if(num == 100) videoDownloadDone = true;
						}
						task.logger.log("[downloading] youtube-dl stdout : " + line);
					}

				} catch (IOException e) {
					task.failed();
					SwingDialogs.error("Error [" + task.getVideoName() + "]", "[Task" + task.getTaskNum() + "|downloading] Error when redirecting output of youtube-dl\n%e%", e, true);
				}

			}, br -> {
				try {

					String line = null;
					List<String> lines = new ArrayList<>();

					while ((line = br.readLine()) != null) {

						task.logger.log("[downloading] youtube-dl stderr : " + line);
						lines.add(line);
						
					}

					if (!lines.isEmpty()) {

						if (lines.stream().map(YTDLPFallbacks::runFixCommand).allMatch(b -> b)) {
							task.setVideoName(task.getVideoName() + " (error occurred but fixed, continuing download)");
							if (task.getTotalNumVideo() == 1) { // not a PlayList?
								download(url, task, playListOption);
							} else {
								download(url, task, playListOption, "--playlist-start", String.valueOf(task.getNowVideoNum()));
							}
							return;
						}

						task.failed();
						SwingDialogs.error("Error [" + task.getVideoName() + "]",
								"[Task" + task.getTaskNum() + "|downloading] There's Error(s) in youtube-dl proccess!\n"
										+ lines.stream().collect(Collectors.joining("\n")), null, true);
					}

				} catch (Exception e) {

					task.failed();
					SwingDialogs.error("Error [" + task.getVideoName() + "]", "[Task" + task.getTaskNum() + "|downloading] Error when redirecting error output of youtube-dl\n%e%", e, true);

				}

			});
		} catch (IOException e1) {
			task.failed();
			SwingDialogs.error("Error [" + task.getVideoName() + "]", "[Task" + task.getTaskNum() + "|downloading] Couldn't start youtube-dl :\n%e%" , e1, true);
			return;
		}
		
		task.setProcess(p);
		task.setStatus("initiating download");
		task.setProgress(0);

		
		try {
			int errorCode = p.waitFor();
			Duration diff = Duration.between(startTime, Instant.now());
			
			if(errorCode != 0) { 
				SwingDialogs.error("Error in youtube-dl", "[Task" + task.getTaskNum() + "|downloading] youtube-dl has ended with error code : " + errorCode, null, true);
				task.logger.log("[downloading] elapsed time in downloading(failed) : " + String.format("%d min %d sec", 
		                diff.toMinutes(),
		                diff.toSecondsPart()));
				task.failed();
				return;
			} else {
				task.logger.log("[downloaded] elapsed time in working(succeed) : " + String.format("%d min %d sec", 
		                diff.toMinutes(),
		                diff.toSecondsPart()));
				task.logger.log("[finished] Finished!\n");
				task.finished();
			}
			
		} catch (InterruptedException e) {
			
			task.failed();
			SwingDialogs.error("Error [" + task.getVideoName() + "]", "[Task" + task.getTaskNum() + "|downloading] Failed to wait youtube-dl process : %e%", e, true);
			
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
		
		return "\"" + "bv" + video + height + "+ba" + audio + "/b" + video + height + " / bv*+ba/b" + "\"";
		
	}

}
