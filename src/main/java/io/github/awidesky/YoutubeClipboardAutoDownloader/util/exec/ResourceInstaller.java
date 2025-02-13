package io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec;

import static io.github.awidesky.YoutubeClipboardAutoDownloader.util.OSUtil.isLinux;
import static io.github.awidesky.YoutubeClipboardAutoDownloader.util.OSUtil.isMac;
import static io.github.awidesky.YoutubeClipboardAutoDownloader.util.OSUtil.isWindows;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import io.github.awidesky.YoutubeClipboardAutoDownloader.Main;
import io.github.awidesky.YoutubeClipboardAutoDownloader.YoutubeClipboardAutoDownloader;
import io.github.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;
import io.github.awidesky.YoutubeClipboardAutoDownloader.gui.LogTextDialog;
import io.github.awidesky.guiUtil.Logger;

public class ResourceInstaller {

	public static final String YTDLP_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"; 
	private static final String root = YoutubeClipboardAutoDownloader.getAppdataPath();
	
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
		log.log("Installing ffmpeg...");
		showProgress("Downloading ffmpeg");
		
		if(isWindows()) {
			deleteDirectoryRecursion(Paths.get(root, "ffmpeg"));
			String url = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";		
			long filesize = getFileSize(new URL(url));
			log.log("Length of " + url + " : " + filesize);
			
			setLoadingFrameContent("Downloading ffmpeg version " + stripVersionNumbers(new URL("https://www.gyan.dev/ffmpeg/builds/release-version")), filesize);
		
			download(new URL(url), new File(root + File.separator + "ffmpeg.zip"));
			
			unzipFolder(Paths.get(root, "ffmpeg.zip"), Paths.get(root));
			
			for(File ff : new File(root).listFiles(f -> f.getName().startsWith("ffmpeg"))) {
				if(ff.isDirectory()) {
					ff.renameTo(new File(ff.getParentFile().getAbsolutePath() + File.separator + "ffmpeg"));
				}
			}
			
			Files.delete(Paths.get(root, "ffmpeg.zip"));
			Files.delete(Paths.get(root, "ffmpeg", "bin", "ffplay.exe"));
			Files.delete(Paths.get(root, "ffmpeg", "bin", "ffprobe.exe")); //TODO : do not remove
			deleteDirectoryRecursion(Paths.get(root, "ffmpeg", "doc"));
			deleteDirectoryRecursion(Paths.get(root, "ffmpeg", "presets"));
		} else if(isMac()) {
			String[] cmd = {"/bin/bash", "-c", "/opt/homebrew/bin/brew install ffmpeg"};
			setLoadingFrameContent("Installing ffmpeg via \"brew install ffmpeg\"... (Progress bar will stay in 0)", -1);
			LogTextDialog upDiag = new LogTextDialog(cmd, log);
			upDiag.setVisible(true);
			ProcessExecutor.runNow(upDiag.getLogger(), null, cmd);
			upDiag.dispose();
		} else if(isLinux()) {
			deleteDirectoryRecursion(Paths.get(root, "ffmpeg"));
			String arch = getArch();
			String url = "https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-" + arch + "-static.tar.xz";		
			long filesize = getFileSize(new URL(url));
			log.log("Length of " + url + " : " + filesize);
			
			setLoadingFrameContent("Downloading ffmpeg version " + stripVersionNumbers(new URL("https://johnvansickle.com/ffmpeg/release-readme.txt")), filesize);
		
			download(new URL(url), new File(root + File.separator + "ffmpeg.tar.xz"));
			
			List<String> untar = new ArrayList<>();
			if(!new File(root, "ffmpeg").mkdirs())
				untar.addAll(List.of("mkdir", "ffmpeg;"));
			
			untar.addAll(List.of("tar", "-xf", "ffmpeg.tar.xz", "-C", "ffmpeg", "--strip-components", "1"));
			log.log("Unzipping ffmpeg.tar.xz with : " + untar.stream().collect(Collectors.joining(" " )));
			ProcessExecutor.runNow(log, new File(root), untar.toArray(String[]::new));
			
			File ff = new File(root + File.separator + "ffmpeg", "ffmpeg");
			new File(root + File.separator + "ffmpeg" + File.separator + "bin").mkdirs();
			ff.renameTo(new File(ff.getParentFile().getAbsolutePath() + File.separator + "bin" + File.separator + "ffmpeg"));
			Files.delete(Paths.get(root, "ffmpeg.tar.xz"));
			Files.delete(Paths.get(root, "ffmpeg", "ffprobe"));
			Files.delete(Paths.get(root, "ffmpeg", "qt-faststart"));
			deleteDirectoryRecursion(Paths.get(root, "ffmpeg", "manpages"));
		}

		hideProgress();
		log.log("ffmpeg installed!!");
	}
	
	private static String getArch() {
		String arch = System.getProperty("os.arch").toLowerCase();
		return switch (arch) {
		case "aarch64" -> "arm64";
		case "amd64" -> "amd64";
		case "x86_64" -> "amd64";
		case "x86" -> "i686";
		default -> {
			String ret;
			if (arch.contains("amd") || arch.contains("x64"))
				ret = "amd64";
			else if (arch.contains("arm") || arch.contains("aarch"))
				ret = "arm64";
			else {
				log.log("Unable to guess architecture. Please report to https://github.com/awidesky/Youtube-Clipboard-Auto-Downloader4J/issues");
				ret = "amd64";
			}
			log.log("Unrecognized architecture : " + arch + ". Considering it's " + ret);
			yield ret;
		}
		};
	}
	public static void getYtdlp() throws MalformedURLException, IOException, InterruptedException, ExecutionException {
		log.log("Installing yt-dlp...");
		showProgress("Downloading yt-dlp");
		if(isMac()) {
			String[] cmd = {"/bin/bash", "-c", "/opt/homebrew/bin/brew install yt-dlp"};
			setLoadingFrameContent("Installing yt-dlp via \"brew install yt-dlp\"... (Progress bar will stay in 0)", -1);
			LogTextDialog upDiag = new LogTextDialog(cmd, log);
			upDiag.setVisible(true);
			ProcessExecutor.runNow(upDiag.getLogger(), null, cmd);
			upDiag.dispose();
		} else {
			Arrays.stream(Optional.ofNullable(new File(root + File.separator + "ffmpeg" + File.separator + "bin").listFiles()).orElse(new File[] {}))
				.filter(File::isFile).filter(f -> f.getName().startsWith("yt-dlp")).forEach(File::delete);
			
			String url = YTDLP_URL;
			if (isWindows()) url += ".exe";
			else if (isLinux()) {
				switch(getArch()) {
				case "arm64":
					url += "_linux_aarch64";
					break;
				case "amd64":
					url += "_linux";
					break;
				default:
				}
			}


			long filesize = getFileSize(new URL(url));
			log.log("Length of " + url + " : " + filesize);
			String releaseURL = getRedirectedURL(new URL("https://github.com/yt-dlp/yt-dlp/releases/latest"));
			setLoadingFrameContent("Downloading yt-dlp version " + releaseURL.substring(releaseURL.lastIndexOf('/') + 1), filesize);
			download(new URL(url), new File(root + File.separator + "ffmpeg" + File.separator + "bin"  + File.separator + (isWindows() ? "yt-dlp.exe" : "yt-dlp")));
		}
		
		hideProgress();
		log.log("yt-dlp installed!!");
	}

	private static void download(URL url, File dest) throws IOException {
		IOException ee = null;
		if(dest.exists()) { dest.delete(); }
		dest.getParentFile().mkdirs();
		dest.createNewFile();
		if(!dest.canExecute()) dest.setExecutable(true);
		
		try (ReadableByteChannel in = Channels.newChannel(url.openStream());
				FileChannel out = FileChannel.open(dest.toPath(), StandardOpenOption.WRITE)) {

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

	private static String stripVersionNumbers(URL url) {
		try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8.toString())) {
			Pattern ptr = Pattern.compile(".*?(\\d\\.\\d(\\.\\d)?).*?");
			while(scanner.hasNext()) {
				String str = scanner.nextLine();
				Matcher m = ptr.matcher(str);
				if(m.find()) {
					return m.group(1);
				}
			}
			
			log.log("Unable to find pattern \"" + ptr.pattern() + "\" from url : " + url.toString());
			return "Unknown";
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
			loadingFrame.getContentPane().setLayout(new BoxLayout(loadingFrame.getContentPane(), BoxLayout.Y_AXIS));
			loadingFrame.setResizable(false);

			loadingStatus = new JLabel("0.00byte / -");

			GridBagConstraints gbc = new GridBagConstraints();
	        gbc.weightx = 1;
	        gbc.fill = GridBagConstraints.HORIZONTAL;
			
			JPanel loading = new JPanel(new GridBagLayout());
			loading.add(Box.createHorizontalStrut(10));
			loading.add(loadingStatus, gbc);
			
			progress = new JProgressBar();
			progress.setStringPainted(true);
			JPanel init = new JPanel(new GridBagLayout());
			init.add(Box.createHorizontalStrut(10));
			init.add(progress, gbc);
			init.add(Box.createHorizontalStrut(10));
			
			loadingFrame.add(loading);
			loadingFrame.add(init);
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
