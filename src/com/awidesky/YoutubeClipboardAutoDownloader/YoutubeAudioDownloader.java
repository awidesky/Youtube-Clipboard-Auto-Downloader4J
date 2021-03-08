package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class YoutubeAudioDownloader {


	private static String projectpath = new File(new File(".").getAbsolutePath()).getParent();;
	private static String youtubedlpath;
	private static String options;
	private static Pattern percentPtn = Pattern.compile("^[0-9]+%$");
	private static Pattern versionPtn = Pattern.compile("^\\d{4}\\.\\d{2}\\.\\d{2}$");

	

	public static void setArgsOptions(String options) {
		YoutubeAudioDownloader.options = options;
	}
	

	static void checkFiles() {

		/* check youtube-dl */
		if (checkYoutubedl("youtube-dl")) {
			
			youtubedlpath = "";
			
			
		} else {
			
			youtubedlpath = projectpath + "\\YoutubeAudioAutoDownloader-resources\\ffmpeg\\bin\\";
			
			if (!checkYoutubedl(youtubedlpath + "youtube-dl")) {

				GUI.error("Error!", "youtube-dl does not exist in\n\t" + youtubedlpath + "\tor system %path%!");
				throw new Error();

			}

		}
		
		Main.log("projectpath = " + projectpath);
		Main.log("youtubedlpath = " + (youtubedlpath.equals("") ? "system %path%" : youtubedlpath));
		
		
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
			
			GUI.error("Error when checking ffmpeg : ", e.getMessage());
			
		}
		
	}
	
	private static boolean checkYoutubedl(String ydlfile){
		
		Main.log("Check if youtube-dl path is in system path");
		ProcessBuilder pb_ydl = new ProcessBuilder(ydlfile, "--version");

		Main.log("Checking youtube-dl path by \"" + ydlfile + " --version\"");

		// start process
		try {
			
			Process p = pb_ydl.directory(null).start();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				
				return versionPtn.matcher(br.readLine()).matches();
				
			} catch(IOException e1) { throw e1; }
			
		} catch (Exception e) {
					
			GUI.error("Error when checking youtube-dl : ", e.getMessage());
			return false;
			
		}
		
	}
	

	public static String getProjectpath() {
		return projectpath;
	}


	public static void download(String url, TaskStatusViewerModel task) throws Exception {

		Main.logProperties("Current");

		StringBuilder sb = new StringBuilder(""); // to retrieve command line argument

		/* get video name */
		ProcessBuilder pbGetName = new ProcessBuilder(youtubedlpath + "youtube-dle", "--get-filename", "-output",
				"\"%(title)s\"", url);

		// retrieve command line argument
		pbGetName.command().stream().forEach((s) -> sb.append(s).append(' '));
		Main.log("Getting video name by \"" + sb.toString().trim() + "\"");

		// start process
		Process p1 = pbGetName.directory(null).start();
		BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
		String name = br1.readLine();
		task.setVideoName(name);
		Main.log("Video name : " + name);
		p1.waitFor();
		try { br1.close(); } catch (IOException i) { GUI.error("Error when closing process stream", i.getMessage()); }
		
		
		/* download video */
		ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "youtube-dl", options, "--newline",
				"--extract-audio", Main.getProperties().getPlaylistOption(), "--audio-format",
				Main.getProperties().getFormat(), "--output", "\"%(title)s.%(ext)s\"", "--audio-quality",
				Main.getProperties().getQuality(), url);

		// retrieve command line argument
		pb.command().stream().forEach((s) -> sb.append(s).append(' '));
		Main.log("Donwloading video name by \"" + sb.toString().trim() + "\"");

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

					Main.log("youtube-dl stdout : " + line);

				}

			} catch (IOException e) {

				GUI.error("Error when redirecting output of youtube-dl", e.getMessage());

			}

		});

		Thread stderr = new Thread(() -> {

			try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));) {
				
				String line = null;
				StringBuilder sb1 = new StringBuilder("");
				
				while ((line = br.readLine()) != null) {

					task.setStatus("ERROR");
					sb1.append(line);
					Main.log("youtube-dl stderr : " + line);

				}

				if (!sb1.toString().equals("")) {

					throw new RuntimeException("Exception in youtube-dl proccess!");

				}

			} catch (IOException e) {

				GUI.error("Error when redirecting error output of youtube-dl", e.getMessage());

			}

		});

		stdout.start();
		stderr.start();

		p.waitFor();

		task.done();


		Main.log("Done!\n");

	}

}
