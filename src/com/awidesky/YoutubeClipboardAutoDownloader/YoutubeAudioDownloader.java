package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;

public class YoutubeAudioDownloader {


	private static String projectpath = new File(new File(".").getAbsolutePath()).getParent();;
	private static String youtubedlpath;
	private static String options = "";
	private static Pattern percentPtn = Pattern.compile("[0-9]+\\.*[0-9]+%");
	private static Pattern versionPtn = Pattern.compile("^\\d{4}\\.\\d{2}\\.\\d{2}$");

	

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
		StringBuilder sb = new StringBuilder("");
		pb_ffmpeg.command().stream().forEach((s) -> sb.append(s).append(' '));
		Main.log("\n");
		Main.log("Checking ffmpeg installation by \"" + sb.toString().trim() + "\"");

		// start process
		try {
			
			Process p = pb_ffmpeg.directory(null).start();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String output;
				if (!(output = br.readLine()).startsWith("ffmpeg version"))
					throw new Exception("ffmpeg does not exist in\n" + (youtubedlpath.equals("") ? "system %path%" : youtubedlpath));
				else Main.log(output);
			} catch (IOException e1) { throw e1; }
			
			Main.log("Executing ffmpeg ended with exit code : " + p.waitFor());
			
		} catch (Exception e) {
			
			GUI.error("Error!", "ffmpeg does not exist in\n\t" + youtubedlpath + "\tor system %path%!", null);
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
					if ( (Integer.parseInt(new SimpleDateFormat("yyyyMM").format(new Date())) - Integer.parseInt(line.substring(0, 7).replace(".", ""))) > 1 ) //update if yputube-dl version is older than a month 
						try { new ProcessBuilder(ydlfile, "--update").start().waitFor(); } catch (Exception e) { GUI.error("Error when updating youtube-dl", "%e%\nI couldn't update youtube-dl!", e); }
					else Main.log("youtube-dl version is not older than a month");
					
					Main.log("Executing youtube-dl ended with exit code : " + p.waitFor());
					
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


	public static void download(String url, TaskData task) throws Exception {

		Main.log("\n"); Main.log("\n");
		Main.logProperties("[Task" + task.getTaskNum() + "|preparing] " + "Current");


		/* download video */
		Main.log("\n");
		StringBuilder sb2 = new StringBuilder("");
		long startTime = System.nanoTime();
		ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "youtube-dl" + options, "--newline",
				"--extract-audio", ConfigDTO.getPlaylistOption().toCommandArgm(), "--audio-format",
				ConfigDTO.getFormat(), "--output", "\"" + ConfigDTO.getFileNameFormat() + "\"", "--audio-quality",
				ConfigDTO.getQuality(), url);

		// retrieve command line argument
		pb.command().stream().forEach((s) -> sb2.append(s).append(' '));
		Main.log("[Task" + task.getTaskNum() + "|downloading] " + "Donwloading video name by \"" + sb2.toString().trim() + "\"");

		// start process
		Process p = pb.directory(new File(ConfigDTO.getSaveto())).start();

		task.setStatus("Downloading");
		task.setProgress(0);


		try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));) {

			String line = null;
			while ((line = br.readLine()) != null) {

				if (line.startsWith("[download]")) {
					if(line.contains("0.0%")) task.increaseVideoNum();
					
					Matcher m = percentPtn.matcher(line);
					if (m.find()) task.setProgress((int)Math.round(Double.parseDouble(m.group().replace("%", ""))));
				}

				Main.log("[Task" + task.getTaskNum() + "|downloading] " + "youtube-dl stdout : " + line);

			}

		} catch (IOException e) {

			GUI.error("[Task" + task.getTaskNum() + "|downloading] " + "Error when redirecting output of youtube-dl", "%e%", e);

		}


		try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));) {

			String line = null;
			StringBuilder sb1 = new StringBuilder("");

			while ((line = br.readLine()) != null) {

				task.setStatus("ERROR");
				sb1.append(line);
				Main.log("[Task" + task.getTaskNum() + "|downloading] " + "youtube-dl stderr : " + line);

			}

			if (!sb1.toString().equals("")) {

				GUI.error("Error in youtube-dl",
						"[Task" + task.getTaskNum() + "|downloading] " + "There's Error(s) in youtube-dl proccess!",
						null);

			}

		} catch (IOException e) {

			GUI.error("[Task" + task.getTaskNum() + "|downloading] "
					+ "Error when redirecting error output of youtube-dl", "%e%", e);

		}

		int errorCode;
		if( (errorCode = p.waitFor()) != 0) { 
			task.setStatus("ERROR");
			GUI.error("Error in youtube-dl", "[Task" + task.getTaskNum() + "|downloading] " + " has ended with error code : " + errorCode, null);
			Main.log("[Task" + task.getTaskNum() + "|downloading] " + "elapsed time in downloading(failed) : " + ((System.nanoTime() - startTime) / 1e6) + "ms" );
			return;
		} else {
			task.done();
		}

		Main.log("[Task" + task.getTaskNum() + "|downloading] " + "Finished!\n");

		Main.log("[Task" + task.getTaskNum() + "|downloading] " + "elapsed time in working : " + ((System.nanoTime() - startTime) / 1e6) + "ms" );
		
	}

	/** get video name 
	 * @throws Exception */
	public static boolean validateAndSetName(String url, TaskData task) {
		
		try {
			Main.log("\n");
			StringBuilder sb = new StringBuilder(""); // to retrieve command line argument
			long startTime = System.nanoTime();
			ProcessBuilder pbGetName = new ProcessBuilder(youtubedlpath + "youtube-dl", "--get-filename",
					ConfigDTO.getPlaylistOption().toCommandArgm(), "-o", ConfigDTO.getFileNameFormat().replace("%(ext)s", ConfigDTO.getFormat()), url);

			// retrieve command line argument
			pbGetName.command().stream().forEach((s) -> sb.append(s).append(' '));
			Main.log("[Task" + task.getTaskNum() + "|validating] " + "Getting video name by \"" + sb.toString().trim()
					+ "\"");

			// start process
			Process p1 = pbGetName.directory(null).start();
			BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
			String name = br1.readLine();
			
			if ((name.contains("WARNING")) || (name.contains("ERROR")) || (p1.waitFor() != 0)) return false;
			
			if (ConfigDTO.getPlaylistOption() == PlayListOption.YES) {
				
				name += " 및 플레이리스트 전체";
				task.setVideoName(name);
				
				int vdnum = 1;
				while(br1.readLine() != null) vdnum++;
				task.setTotalNumVideo(vdnum);
				
			} else { task.setVideoName(name); }
			
			Main.log("[Task" + task.getTaskNum() + "|validating] " + "Video name : " + name);
			Main.log("[Task" + task.getTaskNum() + "|validating] " + "Ended with exit code : " + p1.waitFor());
			
			try {
				br1.close();
			} catch (IOException i) {
				GUI.error("[Task" + task.getTaskNum() + "|validating] " + "Error when closing process stream", "%e%", i);
			}
			
			Main.log("[Task" + task.getTaskNum() + "|validating] " + "elapsed time in validating link and downloading video name : "
					+ ((System.nanoTime() - startTime) / 1e6) + "ms");
			
			return true;
		} catch (Exception e) {
			GUI.error("Error in getting video name", "[Task" + task.getTaskNum() + "|validating] " + "%e%", e);
		}
		return false;
	}
	

		
	

}
