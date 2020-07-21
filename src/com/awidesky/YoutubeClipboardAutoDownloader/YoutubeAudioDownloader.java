package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class YoutubeAudioDownloader {

	private static final String projectpath = new File(YoutubeAudioDownloader.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
	private static final String youtubedlpath = projectpath + "\\YoutubeAudioAutoDownloader-resources\\ffmpeg\\bin";
	private static File downloadPath;
	
	static void checkFiles() {
		//System.out.println(youtubedlpath);
		if (!new File(youtubedlpath + "\\youtube-dl.exe").exists()) { throw new Error("youtube-dl.exe does not exist!");
			/*
			try {
			
				File file = File.createTempFile("tempfile", ".zip");
			
				InputStream input = YoutubeAudioDownloader.class.getResourceAsStream("/com/awidesky/YoutubeClipboardAutoDownlader/resources.zip");
				OutputStream out = new FileOutputStream(file);
	        	int read;
	        	byte[] bytes = new byte[1024];

	        	while ((read = input.read(bytes)) != -1) {
	        		out.write(bytes, 0, read);
	        	}
	        	out.close();
	        	file.deleteOnExit();
	        
	    
	        	// unzip it
	        	ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
	        	ZipEntry ze = zis.getNextEntry();
	        	while(ze!=null){
	        		String entryName = ze.getName();
	        		System.out.print("Extracting " + entryName + " -> " + projectpath + File.separator +  entryName + "...");
            		File f = new File(projectpath + File.separator +  entryName);
            		//create all folder needed to store in correct relative path.
            		f.getParentFile().mkdirs();
            		FileOutputStream fos = new FileOutputStream(f);
            		int len;
            		byte buffer[] = new byte[1024];
            		while ((len = zis.read(buffer)) > 0) {
            			fos.write(buffer, 0, len);
            		}
            		fos.close();  
            		System.out.println("OK!");
            		ze = zis.getNextEntry();
	        	}
        	
	        	zis.closeEntry();
	        	zis.close();

			} catch (IOException ex) {
	    	
				Main.log(ex);
	        
			}*/
		}
	}
	
	static void download(String url, String path) throws Exception {
		
		downloadPath = new File(path);
		
		try {
			
			//Main.log(downloadPath.getAbsolutePath());
																													// TODO : let user select format and quality!
			ProcessBuilder pb = new ProcessBuilder(youtubedlpath + "\\youtube-dl.exe", "-x", "--no-playlist", "--audio-format", "mp3", "--audio-quality", "0",  url);
			Process p = pb.directory(new File(youtubedlpath)).inheritIO().start();

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
