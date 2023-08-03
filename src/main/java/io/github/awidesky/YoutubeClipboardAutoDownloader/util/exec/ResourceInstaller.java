package io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec;

import static io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec.OSUtil.isLinux;
import static io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec.OSUtil.isMac;
import static io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec.OSUtil.isWindows;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import io.github.awidesky.YoutubeClipboardAutoDownloader.Main;
import io.github.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;
import io.github.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;
import io.github.awidesky.guiUtil.Logger;

public class ResourceInstaller {

	public static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"; 
	private static final String root = YoutubeAudioDownloader.getResourcePath();
	
	private static JLabel loadingStatus;
	private static JProgressBar progress;
	private static JFrame loadingFrame;
	
	private static final int BUFFER_SIZE = 32 * 1024; // It seems URL connection only reads 16KB per one read operation. 32 will be sufficient.
	
	private static final Logger log = Main.getLogger("[util.ResourceInstaller] ");
	
	public static boolean ffmpegAvailable() {
		return isWindows() || isMac() || isLinux();
	}
	public static boolean ytdlpAvailable() {
		return isWindows() || isMac() || isLinux();
	}
	
	
	public static void getFFmpeg() throws MalformedURLException, IOException, InterruptedException, ExecutionException {
		deleteDirectoryRecursion(Paths.get(root, "ffmpeg"));
		
		log.log("Installing ffmpeg...");
		showProgress("Downloading ffmpeg");
		String url = "Unknown_OS!";
		
		if(isWindows()) {
			url = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";		
			long filesize = getFileSize(new URL(url));
			log.log("Length of " + url + " : " + filesize);
			
			setLoadingFrameContent("Downloading ffmpeg version " + getContent(new URL("https://www.gyan.dev/ffmpeg/builds/release-version")), filesize);
		
			download(new URL(url), new File(root + File.separator + "ffmpeg.zip"));
			
			unzipFolder(Paths.get(root, "ffmpeg.zip"), Paths.get(root));
			
			for(File ff : new File(root).listFiles(f -> f.getName().startsWith("ffmpeg"))) {
				if(ff.isDirectory()) {
					ff.renameTo(new File(ff.getParentFile().getAbsolutePath() + File.separator + "ffmpeg"));
				}
			}
			
			Files.delete(Paths.get(root, "ffmpeg.zip"));
			Files.delete(Paths.get(root, "ffmpeg", "bin", "ffplay.exe"));
			Files.delete(Paths.get(root, "ffmpeg", "bin", "ffprobe.exe"));
			Files.delete(Paths.get(root, "ffmpeg", "README.txt"));
			deleteDirectoryRecursion(Paths.get(root, "ffmpeg", "doc"));
			deleteDirectoryRecursion(Paths.get(root, "ffmpeg", "presets"));

			hideProgress();
		} else if(isMac()) {
			setLoadingFrameContent("Downloading ffmpeg via \"brew install ffmpeg\"... (Progress bar will stay in 0)", -1);
			//TODO : big TextArea that shows log outputs(for both windows and linux)
			ProcessExecutor.runNow(log, null, "/bin/bash", "-c", "/opt/homebrew/bin/brew install ffmpeg");
		} else if(isLinux()) {
			setLoadingFrameContent("Downloading ffmpeg via \"sudo apt install ffmpeg\"...", -1);
			ProcessExecutor.runNow(log, null, "sudo", "apt", "install", "ffmpeg");
		}

		log.log("ffmpeg installed!!");
	}
	
	public static void getYtdlp() throws MalformedURLException, IOException {
		Arrays.stream(Optional.ofNullable(new File(root + File.separator + "ffmpeg" + File.separator + "bin").listFiles()).orElse(new File[] {}))
			.filter(File::isFile).filter(f -> f.getName().startsWith("yt-dlp")).forEach(File::delete);
		
		log.log("Installing yt-dlp...");
		showProgress("Downloading yt-dlp");
		String url = YTDLP_URL + (isWindows() ? ".exe" : (isMac() ? "_macos" : "_linux"));
		long filesize = getFileSize(new URL(url));
		log.log("Length of " + url + " : " + filesize);
		String releaseURL = getRedirectedURL(new URL("https://github.com/yt-dlp/yt-dlp/releases/latest"));
		setLoadingFrameContent("Downloading yt-dlp version " + releaseURL.substring(releaseURL.lastIndexOf('/') + 1), filesize);
		download(new URL(url), new File(root + File.separator + "ffmpeg" + File.separator + "bin"  + File.separator + (isWindows() ? "yt-dlp.exe" : "yt-dlp")));
		hideProgress();
		log.log("yt-dlp installed!!");
	}

	private static void download(URL url, File dest) throws IOException {
		IOException ee = null;
		if(dest.exists()) { dest.delete(); }
		dest.getParentFile().mkdirs();
		dest.createNewFile();
		
		try (ReadableByteChannel in = Channels.newChannel(url.openStream());
				FileChannel out = new FileOutputStream(dest).getChannel()) {

			log.log("Downloading from " + url.toString() + " to " + dest.getAbsolutePath());
			log.log("Buffer size : " + formatFileSize(BUFFER_SIZE));
			
			ByteBuffer bytebuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
			long total = getFileSize(url);
			log.log("Target file size : " + formatFileSize(total));
			int written = 0;
			boolean eof = false;
  			while (true) {
  				if(bytebuf.hasRemaining() && !eof) {
  					eof = in.read(bytebuf) == -1;
  				}
  				bytebuf.flip();
				if(bytebuf.hasRemaining()) {
					written += out.write(bytebuf); // write data from ByteBuffer to file
				}
				bytebuf.compact();
				updateUI(written, total);
				if(eof && (bytebuf.remaining() == bytebuf.capacity())) break;
			}
		} catch (IOException e) { ee = e; }
		
		if(ee != null) throw ee;
		
		log.log("Successfully downloaded " + url.toString());
	}
	
	private static long getFileSize(URL url) throws IOException {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("HEAD");
			return conn.getContentLengthLong();
		} finally {
			if(conn != null) conn.disconnect();
		}
	}

	private static String getContent(URL url) {
		try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8.toString())) {
			scanner.useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static String getRedirectedURL(URL url) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			InputStream is = conn.getInputStream();
			String ret = conn.getURL().toExternalForm();
			is.close();
			return ret;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (conn != null) conn.disconnect();
		}
	}

	private static void showProgress(String title) {
		SwingUtilities.invokeLater(() -> {
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			loadingFrame = new JFrame();
			loadingFrame.setTitle(title);
			loadingFrame.setIconImage(GUI.ICON);
			loadingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			loadingFrame.setSize(420, 100);
			loadingFrame.setLocation(dim.width / 2 - loadingFrame.getSize().width / 2,
					dim.height / 2 - loadingFrame.getSize().height / 2);
			loadingFrame.setLayout(null);
			loadingFrame.setResizable(false);

			loadingStatus = new JLabel("0.00byte / -");
			loadingStatus.setBounds(14, 8, 370, 18);

			progress = new JProgressBar();
			progress.setStringPainted(true);
			progress.setBounds(15, 27, 370, 18);

			loadingFrame.add(loadingStatus);
			loadingFrame.add(progress);
			loadingFrame.setVisible(true);
		});
	}
	private static void setLoadingFrameContent(String title, long totalSize) {
		SwingUtilities.invokeLater(() -> {
			loadingFrame.setTitle(title);
			loadingStatus.setText("0.00byte / " + formatFileSize(totalSize));
		});
	}
	private static void updateUI(long now, long total) {
		log.logVerbose("Progress : " + formatFileSize(now) + " / " + formatFileSize(total) + " (" + (total > -1 ? (int)(100.0 * now / total) : -1) + "%)");
		SwingUtilities.invokeLater(() -> {
			loadingStatus.setText(formatFileSize(now) + " / " + formatFileSize(total));
			progress.setValue((total > -1 ? (int)(100.0 * now / total) : -1));
		});
	}
	
	private static void hideProgress() {
		SwingUtilities.invokeLater(() -> {
			loadingFrame.setVisible(false);
			loadingFrame.dispose();
		});
	}
	
	
	public static void deleteDirectoryRecursion(Path path) throws IOException {
		if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
				for (Path entry : entries) {
					if(entry.toFile().isFile()) { entry.toFile().delete(); continue; }
					if(entry.toFile().exists()) deleteDirectoryRecursion(entry);
				}
			}
		}
		if(path.toFile().exists()) Files.delete(path);
	}
	
	private static String formatFileSize(long fileSize) {
		
		if(fileSize < 0L) return "unknown";
		if(fileSize == 0L) return "0.00byte";
		
		switch ((int)(Math.log(fileSize) / Math.log(1024))) {
		
		case 0:
			return String.format("%d", fileSize) + "byte";
		case 1:
			return String.format("%.2f", fileSize / 1024.0) + "KB";
		case 2:
			return String.format("%.2f", fileSize / (1024.0 * 1024)) + "MB";
		case 3:
			return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024)) + "GB";
		}
		return String.format("%.2f", fileSize / (1024.0 * 1024 * 1024 * 1024)) + "TB";
		
	}
	
	/**
	 * Code from https://mkyong.com/java/how-to-decompress-files-from-a-zip-file/
	 * I don't know much about <code>ZipInputStream</code>, <code>ZipEntry</code> and stuff.
	 * So I'll just trust this code for now. It works anyway! :D
	 * */
    private static void unzipFolder(Path source, Path target) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(source.toFile()))) {

        	log.logVerbose("Unzip " + source.toAbsolutePath().toString() + " to " + target.toAbsolutePath().toString());
            // list files in zip
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {

                boolean isDirectory = false;
                // example 1.1
                // some zip stored files and folders separately
                // e.g data/
                //     data/folder/
                //     data/folder/file.txt
                if (zipEntry.isDirectory() || zipEntry.getName().endsWith(File.separator)) {
                    isDirectory = true;
                }

                Path newPath = zipSlipProtect(zipEntry, target);

                if (isDirectory) {
                    Files.createDirectories(newPath);
                } else {

                    // example 1.2
                    // some zip stored file path only, need create parent directories
                    // e.g data/folder/file.txt
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }

                    // copy files, nio
                    log.logVerbose("copy to " + newPath.toAbsolutePath().toString());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);

                    // copy files, classic
                    /*try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }*/
                }

                zipEntry = zis.getNextEntry();

            }
            zis.closeEntry();

        }
        
        log.logVerbose("Successfully Unzipped " + source.toAbsolutePath().toString());
        
    }
    
    // protect zip slip attack
    private static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir)
        throws IOException {

        // test zip slip vulnerability
        // Path targetDirResolved = targetDir.resolve("../../" + zipEntry.getName());

        Path targetDirResolved = targetDir.resolve(zipEntry.getName());

        // make sure normalized file still has targetDir as its prefix
        // else throws exception
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
       }

        return normalizePath;
    }
    
}
