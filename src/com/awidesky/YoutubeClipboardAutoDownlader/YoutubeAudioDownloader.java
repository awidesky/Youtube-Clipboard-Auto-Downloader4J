package com.awidesky.YoutubeClipboardAutoDownlader;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class YoutubeAudioDownloader {

	private static final String youtubedlpath = new File("").getAbsolutePath() + "\\resources\\ffmpeg\\bin"; //Thread.currentThread().getContextClassLoader().getResource("ffmpeg/bin").getPath();
	private static File downloadPath;
	
	static {
		
		if (!new File(youtubedlpath + "\\youtube-dl.exe").exists()) { throw new Error("youtube-dl.exe does not exist!"); }

	}
	
	static void download(String url, String path) throws Exception {
		
		downloadPath = new File(path);
		
		try {
			
			//Main.log(downloadPath.getAbsolutePath());
			ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "\\youtube-dl", "-x", "--audio-format", "mp3", "--audio-quality", "0",  url);
			pb.directory(new File(youtubedlpath));
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			Process p = pb.start();

			/*
			 * Thread stdout = new Thread(() -> {
			 * 
			 * BufferedReader br = new BufferedReader(new
			 * InputStreamReader(p.getInputStream())); String line = null;
			 * 
			 * try {
			 * 
			 * while((line = br.readLine ()) != null) {
			 * 
			 * Main.log(line);
			 * 
			 * }
			 * 
			 * } catch (IOException e) {
			 * 
			 * // TODO Auto-generated catch block Main.log(e.toString());
			 * 
			 * }
			 * 
			 * });
			 * 
			 * Thread stderr = new Thread(() -> {
			 * 
			 * BufferedReader br = new BufferedReader(new
			 * InputStreamReader(p.getErrorStream())); String line = null;
			 * 
			 * try {
			 * 
			 * while((line = br.readLine ()) != null) {
			 * 
			 * Main.log(line);
			 * 
			 * }
			 * 
			 * } catch (IOException e) {
			 * 
			 * // TODO Auto-generated catch block Main.log(e.toString());
			 * 
			 * }
			 * 
			 * });
			 * 
			 * stdout.start(); stderr.start();
			 */
			 
			p.waitFor();
			
			
			//Thread.currentThread().sleep(100);
			
			Main.log("founding downloaded file...");
			
			File[] fileList = new File(youtubedlpath).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					// TODO Auto-generated method stub
					return name.endsWith("mp3");
				}
				
			});
			
			if(fileList.length ==0 ) { throw new RuntimeException("Didn't dowload any files!"); }
			
			for(File f : fileList) {
				
				Files.copy(f.toPath(), Paths.get(downloadPath.getAbsolutePath() + "\\" + f.getName()) ,StandardCopyOption.REPLACE_EXISTING);
				Files.delete(f.toPath());
				
			}
			
			Main.log("Done!");
			
		} catch (Exception e) {
	        throw new RuntimeException(e);
	    }
		
	}
	
}
