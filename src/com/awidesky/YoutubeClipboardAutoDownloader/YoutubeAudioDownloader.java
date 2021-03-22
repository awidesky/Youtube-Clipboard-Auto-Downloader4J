package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskData;

public class YoutubeAudioDownloader {


	private static String projectpath = new File(new File(".").getAbsolutePath()).getParent();;
	private static String youtubedlpath;
	private static String options = "";
	private static Pattern percentPtn = Pattern.compile("^[0-9]+%$");
	private static Pattern versionPtn = Pattern.compile("^\\d{4}\\.\\d{2}\\.\\d{2}$");

	

	public static void setArgsOptions(String options) {
		YoutubeAudioDownloader.options = options;
	}
	

	public static void checkYoutubedl() {

		/* check youtube-dl */
		if (checkYoutubedlPath("youtube-dl")) {
			
			youtubedlpath = "";
			
			
		} else {
			
			youtubedlpath = projectpath + "\\YoutubeAudioAutoDownloader-resources\\ffmpeg\\bin\\";
			
			if (!checkYoutubedlPath(youtubedlpath + "youtube-dl")) {

				GUI.error("Error!", "youtube-dl does not exist in\n\t" + youtubedlpath + "\tor system %path%!");
				Main.setExitcode(1);
				Main.kill();
				
			}

		}
		
		Main.log("[init] projectpath = " + projectpath);
		Main.log("[init] youtubedlpath = " + (youtubedlpath.equals("") ? "system %path%" : youtubedlpath));

	}
	
	public static void checkFfmpeg() {
		
		/* check ffmpeg */
		//ffmpeg -version
		ProcessBuilder pb_ffmpeg = new ProcessBuilder(youtubedlpath + "ffmpeg", "-version");

		// retrieve command line argument
		StringBuilder sb = new StringBuilder("");
		pb_ffmpeg.command().stream().forEach((s) -> sb.append(s).append(' '));
		Main.log("Checking ffmpeg installation by \"" + sb.toString().trim() + "\"");

		// start process
		try {
			
			Process p = pb_ffmpeg.directory(null).start();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String output;
				if (!(output = br.readLine()).startsWith("ffmpeg version"))
					throw new Exception("ffmpeg does not exist in\n" + (youtubedlpath.equals("") ? "system %path%" : youtubedlpath));
				Main.log("ffmpeg version : " + output);
			} catch(IOException e1) { throw e1; }
			
			p.waitFor();
			
		} catch (Exception e) {
			
			GUI.error("Error!", "ffmpeg does not exist in\n\t" + youtubedlpath + "\tor system %path%!");
			Main.setExitcode(1);
			Main.kill();
			
		}
		
	}
	
	private static boolean checkYoutubedlPath(String ydlfile){
		
		Main.log("Check if youtube-dl path is in " + ydlfile);
		ProcessBuilder pb_ydl = new ProcessBuilder(ydlfile, "--version");

		Main.log("Checking youtube-dl path by \"" + ydlfile + " --version\"");


		// start process
		try {
			
			String line = null; 
			Process p = pb_ydl.directory(null).start();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				
				if (versionPtn.matcher(line = br.readLine()).matches()) { // vaild path
					
					Main.log("Found valid command to execute youtube-dl : " + ydlfile);
					if ( (Integer.parseInt(new SimpleDateFormat("yyyyMM").format(new Date())) - Integer.parseInt(line.substring(0, 7).replace(".", ""))) > 1 ) //update if yputube-dl version is older than a month 
						try { new ProcessBuilder(ydlfile, "--update").start().waitFor(); } catch (Exception e) { GUI.error("Error when updating youtube-dl", e.getMessage()); }
					
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

		Main.logProperties("[Task" + task.getTaskNum() + "] " + "Current");

		StringBuilder sb = new StringBuilder(""); // to retrieve command line argument

		/* get video name */
		ProcessBuilder pbGetName = new ProcessBuilder(youtubedlpath + "youtube-dl", "--get-filename", "-output",
				"\"%(title)s\"", url);

		// retrieve command line argument
		pbGetName.command().stream().forEach((s) -> sb.append(s).append(' '));
		Main.log("[Task" + task.getTaskNum() + "] " + "Getting video name by \"" + sb.toString().trim() + "\"");

		// start process
		Process p1 = pbGetName.directory(null).start();
		BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
		String name = br1.readLine();
		task.setVideoName(name);
		Main.log("[Task" + task.getTaskNum() + "] " + "Video name : " + name);
		p1.waitFor();
		try { br1.close(); } catch (IOException i) { GUI.error("[Task" + task.getTaskNum() + "] " + "Error when closing process stream", i.getMessage()); }
		
		
		/* download video */
		ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "youtube-dl" + options, "--newline",
				"--extract-audio", Main.getProperties().getPlaylistOption(), "--audio-format",
				Main.getProperties().getFormat(), "--output", "\"%(title)s.%(ext)s\"", "--audio-quality",
				Main.getProperties().getQuality(), url);

		// retrieve command line argument
		pb.command().stream().forEach((s) -> sb.append(s).append(' '));
		Main.log("[Task" + task.getTaskNum() + "] " + "Donwloading video name by \"" + sb.toString().trim() + "\"");

		// start process
		Process p = pb.directory(new File(Main.getProperties().getSaveto())).start();

		task.setDest(Main.getProperties().getSaveto());
		task.setStatus("Downloading");
		task.setProgress(0);

		Thread stdout = new Thread(() -> {

			try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));) {

				String line = null;
				while ((line = br.readLine()) != null) {

					if (line.startsWith("[download]")) {

						task.setProgress(Integer.parseInt(percentPtn.matcher(line).group().replace("%", "")));

					}

					Main.log("[Task" + task.getTaskNum() + "] " + "youtube-dl stdout : " + line);

				}

			} catch (IOException e) {

				GUI.error("[Task" + task.getTaskNum() + "] " + "Error when redirecting output of youtube-dl", e.getMessage());

			}

		});

		Thread stderr = new Thread(() -> {

			try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));) {
				
				String line = null;
				StringBuilder sb1 = new StringBuilder("");
				
				while ((line = br.readLine()) != null) {

					task.setStatus("ERROR");
					sb1.append(line);
					Main.log("[Task" + task.getTaskNum() + "] " + "youtube-dl stderr : " + line);

				}

				if (!sb1.toString().equals("")) {

					GUI.error("Error in youtube-dl", "[Task" + task.getTaskNum() + "] " + "There's Error(s) in youtube-dl proccess!");

				}

			} catch (IOException e) {

				GUI.error("[Task" + task.getTaskNum() + "] " + "Error when redirecting error output of youtube-dl", e.getMessage());

			}

		});

		stdout.start();
		stderr.start();

		p.waitFor();

		task.done();


		Main.log("[Task" + task.getTaskNum() + "] " + "Done!\n");

	}


	public static boolean checkURL(String url, int taskNum) {


		Main.log("[Task" + taskNum + "] " +"Check if youtube-dl path is in system path");
		ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "youtube-dl", url,"--skip-download");

		Main.log("[Task" + taskNum + "] " +"Checking url validation by \"" + youtubedlpath + "youtube-dl " + url +" --skip-download" + "\"");

		// start process
		try {
			
			return pb.directory(null).start().waitFor() == 0 ? true : false;
			
		} catch (Exception e) {
					
			GUI.error("[Task" + taskNum + "] " +"Error when checking url : ", e.getMessage());
			return false;
			
		}
		
	}

}
