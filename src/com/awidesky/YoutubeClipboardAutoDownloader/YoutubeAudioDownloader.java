package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;

public class YoutubeAudioDownloader {


	private static String projectpath = new File(new File(".").getAbsolutePath()).getParent();
	private static String youtubedlpath;
	private static String options = "";
	private static Pattern percentPtn = Pattern.compile("[0-9]+\\.*[0-9]+%");
	private static Pattern versionPtn = Pattern.compile("^\\d{4}\\.\\d{2}\\.\\d{2}$");

	private static Map<String, Runnable> fallBackFix = new HashMap<>();
	
	static {
		fallBackFix.put("ERROR: unable to download video data: HTTP Error 403: Forbidden", () -> {
			runFixCommand("ERROR: unable to download video data: HTTP Error 403: Forbidden", "youtube-dl --rm-cache-dir");
		});
	}

	private static void runFixCommand(String error, String command) {
		
		ProcessBuilder pb = new ProcessBuilder(youtubedlpath + command);

		Main.log("\nFound known error : \"" + error + "\"");
		Main.log("\nTrying to fix error automatically by executing \"" + command + "\"");

		// start process
		try {
			
			Process p = pb.directory(null).start();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				Main.log(br.readLine());
			} catch (IOException e1) { throw e1; }
			
			Main.log("Executing ended with exit code : " + p.waitFor());
			
		} catch (Exception e) {
			
			GUI.error("Error!", "Error when fixing youtube-dl problem!\n%e%", e);
			
		}
		
		Main.log("\n");
	}
	
	
	public static void setArgsOptions(String options) {
		YoutubeAudioDownloader.options = options;
	}
	

	public static void checkYoutubedl() {

		Main.log("\n");
		/* check youtube-dl */
		if (checkYoutubedlPath("youtube-dl")) {
			
			youtubedlpath = "";
			
			
		} else {
			
			youtubedlpath = projectpath + File.separator + "YoutubeAudioAutoDownloader-resources" + File.separator + "ffmpeg" + File.separator + "bin" + File.separator;
			
			if (!checkYoutubedlPath(youtubedlpath + "youtube-dl")) {

				GUI.error("Error!", "youtube-dl does not exist in\n\t" + youtubedlpath + "\tor system %path%!", null);
	 			if (GUI.confirm("Open link in browser?", "Move to download page of youtube-dl?")) Main.webBrowse("http://ytdl-org.github.io/youtube-dl/download.html");
				Main.kill(1);
				
			}

		}
		
		Main.log("[init] projectpath = " + projectpath);
		Main.log("[init] youtubedlpath = " + (youtubedlpath.equals("") ? "system %path%" : youtubedlpath));
		Main.log("\n");

	}
	
	public static void checkFfmpeg() {
		
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
					throw new Exception("ffmpeg does not exist in\n" + (youtubedlpath.equals("") ? "system %path%" : youtubedlpath));
				else Main.log(output);
			} catch (IOException e1) { throw e1; }
			
			Main.log("Executing ffmpeg -version ended with exit code : " + p.waitFor());
			
		} catch (Exception e) {
			
			GUI.error("Error!", "ffmpeg does not exist in\n\t" + youtubedlpath + "\tor system %path%!", null);
	 		if (GUI.confirm("Open link in browser?", "Move to download page of ffmpeg?")) Main.webBrowse("https://ffmpeg.org/download.html");
			Main.kill(1);
			
		}
		
		Main.log("\n");
		
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
							GUI.error("Error when updating youtube-dl", "%e%\nI couldn't update youtube-dl!", e);
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


	public static void download(String url, TaskData task, PlayListOption playListOption) throws Exception {

		Main.log("\n"); Main.log("\n");
		Main.logProperties("[Task" + task.getTaskNum() + "|preparing] Current");


		/* download video */
		Main.log("\n");
		long startTime = System.nanoTime();
		ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "youtube-dl" + options, "--newline",
				"--extract-audio", playListOption.toCommandArgm(), "--audio-format",
				Config.getFormat(), "--output", "\"" + Config.getFileNameFormat() + "\"", "--audio-quality",
				Config.getQuality(), url);

		// retrieve command line argument
		Main.log("[Task" + task.getTaskNum() + "|downloading] Donwloading video by \"" + pb.command().stream().collect(Collectors.joining(" ")) + "\"");

		// start process
		Process p = pb.directory(new File(Config.getSaveto())).start();
		task.setProcess(p);
		task.setStatus("Downloading");
		task.setProgress(0);


		try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));) {

			String line = null;
			while ((line = br.readLine()) != null) {
			  
				if(line.matches("\\[download\\] Downloading video [\\d]+ of [\\d]+"))) {
					Scanner sc = new Scanner(line);
					sc.useDelimiter("[^\\d]+");
					task.setNowVideoNum(sc.nextInt());
					task.setTotalNumVideo(sc.nextInt());
				}
				
				if (line.startsWith("[download]")) {
					if(line.contains(" 0.0%")) task.increaseVideoNum();
					
					Matcher m = percentPtn.matcher(line);
					if (m.find()) task.setProgress((int)Math.round(Double.parseDouble(m.group().replace("%", ""))));
				}

				Main.log("[Task" + task.getTaskNum() + "|downloading] youtube-dl stdout : " + line);

			}

		} catch (IOException e) {

			GUI.error("[Task" + task.getTaskNum() + "|downloading] Error when redirecting output of youtube-dl", "%e%", e);

		}


		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));) {

			String line = null;
			StringBuilder sb1 = new StringBuilder("");
			Runnable fix = null;

			while ((line = br.readLine()) != null) {

				task.setStatus("ERROR");
				sb1.append(line);
				Main.log("[Task" + task.getTaskNum() + "|downloading] youtube-dl stderr : " + line);
				fix = fallBackFix.get(line);
				
			}

			if (!sb1.toString().equals("")) {

				GUI.error("Error in youtube-dl", "[Task" + task.getTaskNum()
					+ "|downloading] There's Error(s) in youtube-dl proccess!", null);

				if(fix != null) fix.run();
			}

		} catch (IOException e) {

			GUI.error("[Task" + task.getTaskNum() + "|downloading] Error when redirecting error output of youtube-dl", "%e%", e);

		}

		int errorCode;
		if( (errorCode = p.waitFor()) != 0) { 
			task.setStatus("ERROR");
			GUI.error("Error in youtube-dl", "[Task" + task.getTaskNum() + "|downloading] youtube-dl has ended with error code : " + errorCode, null);
			Main.log("[Task" + task.getTaskNum() + "|downloading] elapsed time in downloading(failed) : " + ((System.nanoTime() - startTime) / 1e6) + "ms" );
			return;
		} else {
			task.done();
		}

		Main.log("[Task" + task.getTaskNum() + "|downloaded] Finished!\n");

		Main.log("[Task" + task.getTaskNum() + "|downloaded] elapsed time in working(sucessed) : " + ((System.nanoTime() - startTime) / 1e6) + "ms" );
		
	}

	/** get video name */
	public static boolean validateAndSetName(String url, TaskData task, PlayListOption p) {
		
		try {
			Main.log("\n");
			long startTime = System.nanoTime();
			ProcessBuilder pbGetName = new ProcessBuilder(youtubedlpath + "youtube-dl", "--get-filename",
					p.toCommandArgm(), "-o", Config.getFileNameFormat().replace("%(ext)s", Config.getFormat()), url);

			// retrieve command line argument
			Main.log("[Task" + task.getTaskNum() + "|validating] Getting video name by \"" + pbGetName.command().stream().collect(Collectors.joining(" "))	+ "\"");

			// start process
			Process p1 = pbGetName.directory(null).start();
			task.setProcess(p1);
			BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
			String name = br1.readLine();
			
			if ((name.contains("WARNING")) || (name.contains("ERROR")) || (p1.waitFor() != 0)) return false;
			
			if (p == PlayListOption.YES) {
				
				name += " 및 플레이리스트 전체";
				task.setVideoName(name);
				
				int vdnum = 1;
				while(br1.readLine() != null) vdnum++;
				task.setTotalNumVideo(vdnum);
				
			} else { task.setVideoName(name); }
			
			Main.log("[Task" + task.getTaskNum() + "|validating] Video name : " + name);
			Main.log("[Task" + task.getTaskNum() + "|validating] Ended with exit code : " + p1.waitFor());
			
			try {
				if (br1 != null) br1.close();
			} catch (IOException i) {
				GUI.error("[Task" + task.getTaskNum() + "|validating] Error when closing process stream", "%e%", i);
			}
			
			Main.log("[Task" + task.getTaskNum() + "|validating] elapsed time in validating link and downloading video name : "
					+ ((System.nanoTime() - startTime) / 1e6) + "ms");
			
			return true;
		} catch (Exception e) {
			GUI.error("Error in getting video name", "[Task" + task.getTaskNum() + "|validating] %e%", e);
		}
		return false;
	}
	

}
