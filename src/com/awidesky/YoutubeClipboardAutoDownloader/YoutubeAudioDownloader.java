package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class YoutubeAudioDownloader {

	private static final String projectpath = new File(YoutubeAudioDownloader.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
	private static final String youtubedlpath = projectpath + "\\YoutubeAudioAutoDownloader-resources\\ffmpeg\\bin";
	private static File downloadPath;
	
	static void checkFiles() {
		//System.out.println(youtubedlpath);
		if (!new File(youtubedlpath + "\\youtube-dl.exe").exists()) { throw new Error("youtube-dl.exe does not exist in " + youtubedlpath);

		}
	}
	
	public static String getProjectpath() {
		return projectpath;
	}

	static void download(String url) throws Exception {
		
		downloadPath = new File(Main.getProperties().getSaveto());
		
		try {
			
			//Main.log(downloadPath.getAbsolutePath());
																													
			ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "\\youtube-dl.exe", "-x", "--no-playlist", "--audio-format", Main.getProperties().getFormat(), "--audio-quality", Main.getProperties().getQuality(),  url);
			Process p = pb.directory(new File(youtubedlpath)).start();

			
			  Thread stdout = new Thread(() -> {
			  
			  BufferedReader br = new BufferedReader(new
			  InputStreamReader(p.getInputStream())); String line = null;
			  
			  try {
			  
				  while((line = br.readLine ()) != null) {
			  
					  if (line.startsWith("[download]") && !line.startsWith("[download] 100%")) { continue; }
					  if (line.equals("")) {
						  
						  String temp = line;
						  if ((line = br.readLine()).startsWith("[download]")) { continue; }
						  Main.log(temp);
						  
					  }
					  Main.log(line);
			  
				  }
			  
			  } catch (IOException e) {
			  
			  // Main.log(e.toString());
			  
			  }
			  
			  });
			  
			  Thread stderr = new Thread(() -> {
			  
			  BufferedReader br = new BufferedReader(new
			  InputStreamReader(p.getErrorStream())); String line = null;
			  
			  try {
			  
			  while((line = br.readLine ()) != null) {
			  
			  Main.log(line);
			  
			  }
			  
			  } catch (IOException e) {
			  
			  // Main.log(e.toString());
			  
			  }
			  
			  });
			  
			  stdout.start(); stderr.start();
			 
			 
			p.waitFor();
			
			
			//Thread.currentThread().sleep(100);
			
			Main.log("Finding downloaded file...");
			
			File[] fileList = new File(youtubedlpath).listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(Main.getProperties().getFormat());
				}
				
			});
			
			if(fileList.length ==0 ) { throw new RuntimeException("Youtube-dl didn't dowload any files!"); }
			
			for(File f : fileList) {
				
				Files.copy(f.toPath(), Paths.get(downloadPath.getAbsolutePath() + "\\" + f.getName()) ,StandardCopyOption.REPLACE_EXISTING);
				Files.delete(f.toPath());
				
			}
			
			Main.log("Done!\n");
			
		} catch (Exception e) {
	        throw new RuntimeException(e);
	    }
		
	}
	
}
