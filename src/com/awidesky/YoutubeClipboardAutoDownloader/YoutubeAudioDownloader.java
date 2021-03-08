package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class YoutubeAudioDownloader {


	private static String projectpath;
	private static String youtubedlpath;
	private static String options;
	private static Pattern pattern = Pattern.compile("^[0-9]+%$");

	
	static {
		projectpath = new File(new File(".").getAbsolutePath()).getParent();
		youtubedlpath = projectpath + "\\YoutubeAudioAutoDownloader-resources\\ffmpeg\\bin";
	}
	

	public static void setArgsOptions(String options) {
		YoutubeAudioDownloader.options = options;
	}

	static void checkFiles() {

		/* check youtube-dl */
		Main.log("projectpath = " + projectpath);
		Main.log("youtubedlpath = " + youtubedlpath);
		
		if (!new File(youtubedlpath + "\\youtube-dl.exe").exists()) {
			
			GUI.error("Error!", "youtube-dl.exe does not exist in\n" + youtubedlpath);
			throw new Error();

		}
		
		
		/* check ffmpeg */
		//ffmpeg -version
		ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "\\ffmpeg.exe", "-version");

		// retrieve command line argument
		StringBuilder sb = new StringBuilder("");
		pb.command().stream().forEach((s) -> sb.append(s).append(' '));
		Main.log("Checking ffmpeg installation by \"" + sb.toString().trim() + "\"");

		// start process
		try {
			
			Process p = pb.directory(null).start();
			
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String output;
				if (!(output = br.readLine()).startsWith("ffmpeg version"))
					throw new Exception("ffmpeg.exe does not exist in\n" + youtubedlpath);
				Main.log("ffmpeg version : " + output);
			} catch(IOException e1) { throw e1; }
			
			p.waitFor();
			
		} catch (Exception e) {
			
			GUI.error("Error when checking ffmpeg.exe : ", e.getMessage());
			
		}
		
	}

	public static String getProjectpath() {
		return projectpath;
	}


	public static void download(String url, TaskStatusViewerModel task) throws Exception {

		Main.logProperties("Current");

		StringBuilder sb = new StringBuilder(""); // to retrieve command line argument

		/* get video name */
		ProcessBuilder pbGetName = new ProcessBuilder(youtubedlpath + "\\youtube-dl.exe", "--get-filename", "-output",
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
		ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "\\youtube-dl.exe", options, "--newline",
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

						task.setProgress(Integer.parseInt(pattern.matcher(line).group().replace("%", "")));

					}

					Main.log("youtube-dl stdout : " + line);

				}

			} catch (IOException e) {

				GUI.error("Error when redirecting output of youtube-dl.exe", e.getMessage());

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

					throw new RuntimeException("Exception in youtube-dl.exe proccess!");

				}

			} catch (IOException e) {

				GUI.error("Error when redirecting error output of youtube-dl.exe", e.getMessage());

			}

		});

		stdout.start();
		stderr.start();

		p.waitFor();

		task.done();


		Main.log("Done!\n");

	}

}
