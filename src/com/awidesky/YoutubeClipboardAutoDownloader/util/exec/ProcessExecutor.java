package com.awidesky.YoutubeClipboardAutoDownloader.util.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
	
	//TODO : don't return Process. return custom object that has f1, f2, and Process.
	//		 and use waitProcess, waitOutputs, waitStdout, waitStderr to wait each  

	public static int runNow(Logger logger, File dir, String... command) throws InterruptedException, IOException {
		return run(Arrays.asList(command), dir, br -> br.lines().forEach(logger::log), br -> br.lines().forEach(logger::log), true).waitFor();
	}
	public static Process run(List<String> command, File dir, Consumer<BufferedReader> allOut, boolean waitOutput) throws IOException {
		return run(command, dir, allOut, allOut, waitOutput);
	}
	public static Process run(List<String> command, File dir, Consumer<BufferedReader> stdout, Consumer<BufferedReader> stderr, boolean waitOutput) throws IOException {
		
		ProcessBuilder pb = new ProcessBuilder(command);
		// start process
		Process p = pb.directory(dir).start();
		
		Future<?> f1 = TaskThreadPool.submit(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), Main.NATIVECHARSET))) {
				stdout.accept(br);
			} catch (IOException e) {
				SwingDialogs.error("Unable to close process input stream!", "Process : " + command.stream().collect(Collectors.joining(" "))
						+ "\n%e%", e, false);
			}
		});
		Future<?> f2 = TaskThreadPool.submit(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream(), Main.NATIVECHARSET))) {
				stderr.accept(br);
			} catch (IOException e) {
				SwingDialogs.error("Unable to close process input stream!", "Process : " + command.stream().collect(Collectors.joining(" "))
						+ "\n%e%", e, false);
			}
		});
		
		if(waitOutput) {
			try {
				f1.get();
				f2.get();
			} catch (InterruptedException | ExecutionException e) {
				SwingDialogs.error("Unable to wait process output stream to end!!", "Process : " + command.stream().collect(Collectors.joining(" "))
						+ "\n%e%", e, false);
			}
		}
		
		return p; 
	}
}
