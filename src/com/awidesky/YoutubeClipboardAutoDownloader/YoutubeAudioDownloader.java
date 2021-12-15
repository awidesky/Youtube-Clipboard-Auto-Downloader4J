package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;

public class YoutubeAudioDownloader {


	private static String projectpath = new File(new File(".").getAbsolutePath()).getParent();
	private static String youtubedlpath;
	private static Pattern percentPtn = Pattern.compile("[0-9]+\\.*[0-9]+%");
	private static Pattern versionPtn = Pattern.compile("^\\d{4}\\.\\d{2}\\.\\d{2}$");

	private static Map<String, Callable<Boolean>> fallBackFix = new HashMap<>();
	

	private static int runFixCommand(String error, String... command) {
		
		int ret;
		ProcessBuilder pb = new ProcessBuilder(command);

		Main.log("\nFound known error : \"" + error + "\"");
		Main.log("\nTrying to fix error automatically by executing \"" + Arrays.stream(command).collect(Collectors.joining(" ")) + "\"");

		// start process
		try {
			
			Process p = pb.directory(null).start();
			ret = p.waitFor();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				Main.log(br.readLine());
			} catch (IOException e1) { throw e1; }
			
			Main.log("Executing ended with exit code : " + ret);
			
		} catch (Exception e) {
			
			ret = -1;
			GUI.error("Error!", "Error when fixing youtube-dl problem!\n%e%", e, false);
			
		}
		
		Main.log("\n");
		return ret;
		
	}
	
	

	/**
	 * @return Whether the procedure went fine
	 * */
	public static boolean checkYoutubedl() {

		Main.log("\n");
		/* check youtube-dl */
		if (checkYoutubedlPath("youtube-dl")) {
			
			youtubedlpath = "";
			
		} else {
			
			youtubedlpath = projectpath + File.separator + "YoutubeAudioAutoDownloader-resources" + File.separator + "ffmpeg" + File.separator + "bin" + File.separator;
			
			if (!checkYoutubedlPath(youtubedlpath + "youtube-dl")) {

				GUI.error("Error!", "youtube-dl does not exist in\n" + youtubedlpath + "\nor system %PATH%", null, true);
	 			if (GUI.confirm("youtube-dl does not exist!", "Move to download page of youtube-dl?")) Main.webBrowse("http://ytdl-org.github.io/youtube-dl/download.html");
				return false;
				
			}

		}
		
		Main.log("[init] projectpath = " + projectpath);
		Main.log("[init] youtubedlpath = " + (youtubedlpath.equals("") ? "system %PATH%" : youtubedlpath));
		Main.log("\n");

		fallBackFix.put("ERROR: unable to download video data: HTTP Error 403: Forbidden", () -> {
			 return runFixCommand("ERROR: unable to download video data: HTTP Error 403: Forbidden", youtubedlpath + "youtube-dl", "--rm-cache-dir") == 0;
		});
		
		return true;
		
	}
	
	/**
	 * @return Whether the procedure went fine
	 * */
	public static boolean checkFfmpeg() {
		
		/* check ffmpeg */
		//ffmpeg -version
		ProcessBuilder pb_ffmpeg = new ProcessBuilder(youtubedlpath + "ffmpeg", "-version");

		// retrieve command line argument
		Main.log("\nChecking ffmpeg installation by \"" + pb_ffmpeg.command().stream().collect(Collectors.joining(" ")).trim() + "\"");

		// start process
		try {
			
			Process p = pb_ffmpeg.directory(null).start();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String output;
				if (!(output = br.readLine()).startsWith("ffmpeg version"))
					throw new Exception("ffmpeg does not exist in\n" + (youtubedlpath.equals("") ? "system %PATH%" : youtubedlpath));
				else Main.log(output);
			} catch (IOException e1) { throw e1; }
			
			Main.log("Executing ffmpeg -version ended with exit code : " + p.waitFor());
			
		} catch (Exception e) {
			
			GUI.error("Error!", "ffmpeg does not exist in\n" + youtubedlpath + "\nor system %PATH%", null, true);
	 		if (GUI.confirm("ffmpeg does not exist!", "Move to download page of ffmpeg?")) Main.webBrowse("https://ffmpeg.org/download.html");
			return false;
			
		}
		
		Main.log("\n");
		return true;
		
	}
	

	private static boolean checkYoutubedlPath(String ydlfile) {
		
		Main.log("Check if youtube-dl path is in " + ydlfile);
		ProcessBuilder pb_ydl = new ProcessBuilder(ydlfile, "--version");

		Main.log("Checking youtube-dl path by \"" + ydlfile + " --version\"");

		// start process
		try {
			
			String line = null; 
			Process p = pb_ydl.directory(null).start();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				
				if (versionPtn.matcher(line = br.readLine()).matches()) { // valid path
					
					Main.log("Found valid command to execute youtube-dl : " + ydlfile);
					Main.log("youtube-dl version : " + line);
					
					if ( (Integer.parseInt(new SimpleDateFormat("yyyyMM").format(new Date())) - Integer.parseInt(line.substring(0, 7).replace(".", ""))) > 1 ) { //update if yputube-dl version is older than a month 
						try {
							int e;
							if ((e = new ProcessBuilder(ydlfile, "--update").start().waitFor()) != 0) throw new Exception("Error code : " + e);
						} catch (Exception e) { 
							GUI.error("Error when updating youtube-dl", "%e%\nI couldn't update youtube-dl!", e, true);
						}
					} else {Main.log("youtube-dl version is not older than a month");}
					
					Main.log("Executing youtube-dl --version ended with exit code : " + p.waitFor());
					
					return true;
					
				} else { return false; }
				
			} catch(IOException e1) { throw e1; }
			
		} catch (Exception e) {
					
			Main.log("Error when checking youtube-dl\n\t" + e.getMessage());
			return false;
			
		}
		
		
	}
	

	public static String getProjectpath() {
		return projectpath;
	}



	/** get video name */
	public static boolean validateAndSetName(String url, TaskData task, PlayListOption playListOption) {
		
		try {
			Main.log("\n\n");
			long startTime = System.nanoTime();
			
			LinkedList<String> args = new LinkedList<>(Arrays.asList(new String[] {youtubedlpath + "youtube-dl", "--get-filename", "-o",
					"\"" + Config.getFileNameFormat().replace("%(ext)s", Config.getFormat()) + "\"", url
					}));
			if(playListOption.toCommandArgm() != null) args.add(2, playListOption.toCommandArgm());
			ProcessBuilder pbGetName = new ProcessBuilder(args);
			
			// retrieve command line argument
			Main.log("[Task" + task.getTaskNum() + "|validating] Getting video name by \"" + pbGetName.command().stream().collect(Collectors.joining(" "))	+ "\"");

			// start process
			Process p1 = pbGetName.directory(null).start();
			task.setProcess(p1);
			BufferedReader br = new BufferedReader(new InputStreamReader(p1.getInputStream()));
			String name = br.readLine();
			
			if ((name.contains("WARNING")) || (name.contains("ERROR")) || (p1.waitFor() != 0)) return false;
			
			if (playListOption == PlayListOption.YES) {
				
				name += " and whole playlist";
				task.setVideoName(name);
				
				int vdnum = 1;
				while(br.readLine() != null) vdnum++;
				task.setTotalNumVideo(vdnum);
				
			} else { task.setVideoName(name); }
			
			BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getErrorStream()));
			String line;
			while ((line = br1.readLine()) != null) {
				Main.log("[Task" + task.getTaskNum() + "|validating] youtube-dl stderr : " + line);
			}
			
			Main.log("[Task" + task.getTaskNum() + "|validating] Video name : " + name);
			Main.log("[Task" + task.getTaskNum() + "|validating] Ended with exit code : " + p1.waitFor());
			
			try {
				if (br != null) br.close();
				if (br1 != null) br1.close();
			} catch (IOException i) {
				GUI.error("[Task" + task.getTaskNum() + "|validating] Error when closing process stream", "%e%", i, true);
			}
			
			Main.log("[Task" + task.getTaskNum() + "|validating] elapsed time in validating link and downloading video name : "
					+ ((System.nanoTime() - startTime) / 1e6) + "ms");
			
			return true;
		} catch (Exception e) {
			GUI.error("Error in getting video name", "[Task" + task.getTaskNum() + "|validating] %e%", e, true);
		}
		return false;
	}
	
	
	public static void download(String url, TaskData task, PlayListOption playListOption, String... additianalOptions)  {

		Main.log("\n\n");
		Main.logProperties("[Task" + task.getTaskNum() + "|preparing] Current");


		/* download video */
		Main.log("\n");
		long startTime = System.nanoTime();
		
		LinkedList<String> arguments = new LinkedList<>(Arrays.asList(
				youtubedlpath + "youtube-dl", "--newline", "--force-overwrites", playListOption.toCommandArgm(), "--output", "\"" + Config.getFileNameFormat() + "\""));
		
		if(Main.audioMode) {
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

		
		
		ProcessBuilder pb = new ProcessBuilder(arguments);
		
		// retrieve command line argument
		Main.log("[Task" + task.getTaskNum() + "|downloading] Downloading video by \"" + pb.command().stream().collect(Collectors.joining(" ")) + "\"");

		// start process
		Process p = null;
		try {
			
			p = pb.directory(new File(Config.getSaveto())).start();
			
		} catch (IOException e1) {
			
			task.setStatus("ERROR");
			GUI.error("Error when executing youtube-dl", "[Task" + task.getTaskNum() + "|downloading] Couldn't start youtube-dl : %e%" , e1, true);
			return;
			
		}
		
		task.setProcess(p);
		task.setStatus("Downloading");
		task.setProgress(0);


		try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));) {

			String line = null;
			boolean downloadVideoAndAudioSeparately = false, videoDownloadDone = false;
			
			while ((line = br.readLine()) != null) {
				
				if(!Main.audioMode && line.matches("[\\s|\\S]+Downloading [\\d]+ format\\(s\\)\\:[\\s|\\S]+")) {
					downloadVideoAndAudioSeparately = line.contains("+");
				}
				
				if(line.matches("\\[download\\] Downloading video [\\d]+ of [\\d]+")) {
					Main.log("\n");
					Scanner sc = new Scanner(line);
					sc.useDelimiter("[^\\d]+");
					task.setNowVideoNum(sc.nextInt());
					task.setTotalNumVideo(sc.nextInt());
					sc.close();
				}
				
				if(line.startsWith("[ExtractAudio]")) {
					task.setStatus("Extracting Audio");
				}
				
				Matcher m = percentPtn.matcher(line);
				if (m.find() && line.contains("ETA")) {
					int num = (int)Math.round(Double.parseDouble(m.group().replace("%", "")));
					if(downloadVideoAndAudioSeparately) {
						task.setProgress(((videoDownloadDone) ? 50 : 0) + num/2);
					} else {
						task.setProgress(num);
					}
					if(num == 100) videoDownloadDone = !videoDownloadDone; 
				}
				

				Main.log("[Task" + task.getTaskNum() + "|downloading] youtube-dl stdout : " + line);

			}

		} catch (IOException e) {

			task.setStatus("ERROR");
			GUI.error("[Task" + task.getTaskNum() + "|downloading] Error when redirecting output of youtube-dl", "%e%", e, true);
			
		}


		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));) {

			String line = null;
			StringBuilder sb1 = new StringBuilder("");
			Callable<Boolean> fix = null;

			while ((line = br.readLine()) != null) {

				task.setStatus("ERROR");
				sb1.append(line);
				Main.log("[Task" + task.getTaskNum() + "|downloading] youtube-dl stderr : " + line);
				fix = fallBackFix.get(line);
				
			}

			if (!sb1.toString().equals("")) {

				if (fix != null && fix.call()) {
					task.setVideoName(task.getVideoName() + " (an error occurred but fixed, continuing download)");
					if (task.getTotalNumVideo() == 1) { // not a PlayList?
						download(url, task, playListOption);
					} else {
						download(url, task, playListOption, "--playlist-start", String.valueOf(task.getNowVideoNum()));
					}
					return;
				}

				GUI.error("Error in youtube-dl",
						"[Task" + task.getTaskNum() + "|downloading] There's Error(s) in youtube-dl proccess!\n" + sb1.toString(), null, true);
			}

		} catch (Exception e) {

			task.setStatus("ERROR");
			GUI.error("[Task" + task.getTaskNum() + "|downloading] Error when redirecting error output of youtube-dl", "%e%", e, true);

		}

		int errorCode;
		try {
			
			if( (errorCode = p.waitFor()) != 0) { 
				task.setStatus("ERROR");
				GUI.error("Error in youtube-dl", "[Task" + task.getTaskNum() + "|downloading] youtube-dl has ended with error code : " + errorCode, null, true);
				Main.log("[Task" + task.getTaskNum() + "|downloading] elapsed time in downloading(failed) : " + ((System.nanoTime() - startTime) / 1e6) + "ms" );
				return;
			} else {
				task.done();
			}
			
		} catch (InterruptedException e) {
			
			task.setStatus("ERROR");
			GUI.error("Error when waiting youtube-dl process", "[Task" + task.getTaskNum() + "|downloading] Failed to wait youtube-dl process : %e%", e, true);
			
		}

		Main.log("[Task" + task.getTaskNum() + "|downloaded] Finished!\n");

		Main.log("[Task" + task.getTaskNum() + "|downloaded] elapsed time in working(sucessed) : " + ((System.nanoTime() - startTime) / 1e6) + "ms" );
		
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
