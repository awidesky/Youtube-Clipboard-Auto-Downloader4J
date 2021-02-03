package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class YoutubeAudioDownloader {


	private static String projectpath;
	private static String youtubedlpath;
	private static File downloadPath = null;
	private static String options;
	private static Pattern pattern = Pattern.compile("^[0-9]+%$");

	
	static {
		projectpath = new File(new File(YoutubeAudioDownloader.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath()).getParentFile().getAbsolutePath();
		youtubedlpath = projectpath + File.separator + "YoutubeAudioAutoDownloader-resources" + File.separator + "ffmpeg" + File.separator + "bin";
	}
	
	public static void setDownloadPath(String downloadPath) {
		YoutubeAudioDownloader.downloadPath = new File(downloadPath);
	}

	public static void setArgsOptions(String options) {
		YoutubeAudioDownloader.options = options;
	}

	static void checkFiles() {

		//System.out.println(youtubedlpath);
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

			
			//Main.log(downloadPath.getAbsolutePath());
			
			/* get video name */
			ProcessBuilder pbGetName = new ProcessBuilder(youtubedlpath + "\\youtube-dl.exe", "--get-filename -o \"%(title)s\"", url);
			Process p1 = pbGetName.directory(null).start();
			BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
			task.setVideoName(br1.readLine());
			p1.waitFor();
								
			
			/* download video */                                                                   //TODO: name "-o" + "\"%(title)s.%(ext)s\""
			ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "\\youtube-dl.exe", options, "-x", Main.getProperties().getPlaylistOption(), "--audio-format", Main.getProperties().getFormat(), "--audio-quality", Main.getProperties().getQuality(),  url);
			Process p = pb.directory(downloadPath).start();


			task.setDest(downloadPath.getAbsolutePath());
			task.setStatus("Downloading");
			
			
			Thread stdout = new Thread(() -> {

				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line = null;

				
				try {

					while ((line = br.readLine()) != null) {

						if (line.startsWith("[download]")) {
							
							task.setProgress(Integer.parseInt(pattern.matcher(line).group().replace("%", "")));
							
						}
						
						Main.log(line);

					}

				} catch (IOException e) {

					GUI.error("Error when redirecting output of youtube-dl.exe", e.getMessage());

				}

			});

			
			Thread stderr = new Thread(() -> {

				BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line = null;
				StringBuilder sb = new StringBuilder("");
				
				try {

					while ((line = br.readLine()) != null) {
						
						task.setStatus("ERROR");
						sb.append(line);

					}
					
					if (!sb.toString().equals("")) throw new RuntimeException(sb.toString());

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
