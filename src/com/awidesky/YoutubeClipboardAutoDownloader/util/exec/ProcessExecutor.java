package com.awidesky.YoutubeClipboardAutoDownloader.util.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.util.Logger;
import com.awidesky.YoutubeClipboardAutoDownloader.util.SwingDialogs;
import com.awidesky.YoutubeClipboardAutoDownloader.util.workers.TaskThreadPool;

/**
 * Executes third party program & commands
 * */
public class ProcessExecutor {

	public static int runNow(Logger logger, File dir, String... command) throws InterruptedException, IOException {
		return run(Arrays.asList(command), dir, br -> br.lines().forEach(logger::log), br -> br.lines().forEach(logger::log)).waitFor();
	}
	public static Process run(List<String> command, File dir, Consumer<BufferedReader> allOut) throws IOException {
		return run(command, dir, allOut, allOut);
	}
	public static Process run(List<String> command, File dir, Consumer<BufferedReader> stdout, Consumer<BufferedReader> stderr) throws IOException {
		
		ProcessBuilder pb = new ProcessBuilder(command);
		
		// start process
		Process p = pb.directory(dir).start();
		
		TaskThreadPool.submit(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), Main.NATIVECHARSET))) {
				stdout.accept(br);
			} catch (IOException e) {
				SwingDialogs.error("Unable to close process input stream!", "Process : " + command.stream().collect(Collectors.joining(" "))
						+ "\n%e%", e, false);
			}
		});
		TaskThreadPool.submit(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream(), Main.NATIVECHARSET))) {
				stderr.accept(br);
			} catch (IOException e) {
				SwingDialogs.error("Unable to close process input stream!", "Process : " + command.stream().collect(Collectors.joining(" "))
						+ "\n%e%", e, false);
			}
		});

		return p; 
	}
}
