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
		projectpath = new File(new File(YoutubeAudioDownloader.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath()).getParentFile().getAbsolutePath();
		projectpath = projectpath.replaceAll("%20", " ");
		youtubedlpath = projectpath + File.separator + "YoutubeAudioAutoDownloader-resources" + File.separator + "ffmpeg" + File.separator + "bin";
	}
	

	public static void setArgsOptions(String options) {
		YoutubeAudioDownloader.options = options;
	}

	static void checkFiles() {

		Main.log("projectpath = " + projectpath);
		Main.log("youtubedlpath = " + youtubedlpath);
		
		if (!new File(youtubedlpath + "\\youtube-dl.exe").exists()) {
			
			GUI.error("Error!", "youtube-dl.exe does not exist in\n" + youtubedlpath);
			throw new Error();

		}
		//GUI.warning("PathCheck", youtubedlpath);
	}

	public static String getProjectpath() {
		return projectpath;
	}


	public static void download(String url, TaskStatusViewerModel task) throws Exception {


		try {
			
			Main.log(String.format("Current properties :\n downloadpath-%s\n format-%s\n quality-%s\n playlistoption-%s", Main.getProperties().getSaveto(), Main.getProperties().getFormat(), Main.getProperties().getQuality(), Main.getProperties().getPlaylistOption()));
			
			StringBuilder sb = new StringBuilder(""); //to retrieve command line argument
			
			/* get video name */ 
			ProcessBuilder pbGetName = new ProcessBuilder(youtubedlpath + "\\youtube-dl.exe", "--get-filename", "-output", "\"%(title)s\"" , url);
			
			//retrieve command line argument
			pbGetName.command().stream().forEach((s) -> sb.append(s).append(' '));
			Main.log("Getting video name by \"" + sb.toString().trim() + "\"");
			
			//start process
			Process p1 = pbGetName.directory(null).start();
			BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
			String name = br1.readLine();
			task.setVideoName(name);
			Main.log("Video name : " + name);
			p1.waitFor();
								
			
			
			/* download video */
			ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "\\youtube-dl.exe", options, "--newline", "--extract-audio", Main.getProperties().getPlaylistOption(), "--audio-format", Main.getProperties().getFormat(), "--output", "\"%(title)s.%(ext)s\"", "--audio-quality", Main.getProperties().getQuality(),  url);
			
			//retrieve command line argument
			pb.command().stream().forEach((s) -> sb.append(s).append(' '));
			Main.log("Donwloading video name by \"" + sb.toString().trim() + "\"");
			
			//start process
			Process p = pb.directory(new File(Main.getProperties().getSaveto())).start();


			
			task.setDest(Main.getProperties().getSaveto());
			task.setStatus("Downloading");
			task.setProgress(0);
			
			Thread stdout = new Thread(() -> {

				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = null;

				
				try {

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

				BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line = null;
				StringBuilder sb1 = new StringBuilder("");
				
				try {

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

			// Thread.currentThread().sleep(100);
			/*
			 * Main.log("Finding downloaded file...");
			 * 
			 * File[] fileList = new File(youtubedlpath).listFiles(new FilenameFilter() {
			 * 
			 * @Override public boolean accept(File dir, String name) { return
			 * name.endsWith(Main.getProperties().getFormat()); }
			 * 
			 * });
			 * 
			 * if(fileList.length ==0 ) { throw new
			 * RuntimeException("Youtube-dl didn't dowload any files!"); }
			 * 
			 * for(File f : fileList) {
			 * 
			 * Files.copy(f.toPath(), Paths.get(downloadPath.getAbsolutePath() + "\\" +
			 * f.getName()) ,StandardCopyOption.REPLACE_EXISTING); Files.delete(f.toPath());
			 * 
			 * }
			 */
			Main.log("Done!\n");

		} catch (Exception e) {
			throw new Exception(e);
		}

	}

}
