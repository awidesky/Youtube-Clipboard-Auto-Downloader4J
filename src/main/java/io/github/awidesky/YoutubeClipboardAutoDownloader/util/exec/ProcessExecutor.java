package io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec;

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

import io.github.awidesky.YoutubeClipboardAutoDownloader.Main;
import io.github.awidesky.YoutubeClipboardAutoDownloader.util.workers.ProcessIOThreadPool;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;

/**
 * Executes third party program & commands
 * */
public class ProcessExecutor {
	
	public static int runNow(Logger logger, File dir, String... command) throws InterruptedException, ExecutionException, IOException {
		return run(Arrays.asList(command), dir, br -> br.lines().forEach(logger::info), br -> br.lines().forEach(logger::error)).wait_all();
	}
	public static ProcessHandle run(List<String> command, File dir, Consumer<BufferedReader> allOut) throws IOException {
		return run(command, dir, allOut, allOut);
	}
	public static ProcessHandle run(List<String> command, File dir, Consumer<BufferedReader> stdout, Consumer<BufferedReader> stderr) throws IOException {
		
		ProcessBuilder pb = new ProcessBuilder(command);
		// start process
		Process p = pb.directory(dir).start();
		
		Future<?> f1 = ProcessIOThreadPool.submit(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), Main.NATIVECHARSET))) {
				stdout.accept(br);
			} catch (IOException e) {
				SwingDialogs.error("Unable to close process input stream!", "Process : " + command.stream().collect(Collectors.joining(" "))
						+ "\n%e%", e, false);
			}
		});
		Future<?> f2 = ProcessIOThreadPool.submit(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream(), Main.NATIVECHARSET))) {
				stderr.accept(br);
			} catch (IOException e) {
				SwingDialogs.error("Unable to close process input stream!", "Process : " + command.stream().collect(Collectors.joining(" "))
						+ "\n%e%", e, false);
			}
		});
		
		return new ProcessHandle(p, f1, f2); 
	}
	
	public static class ProcessHandle {
		private final Process proc;
		private final Future<?> stdout;
		private final Future<?> stderr;
		
		public ProcessHandle(Process proc, Future<?> stdout, Future<?> stderr) {
			this.proc = proc;
			this.stdout = stdout;
			this.stderr = stderr;
		}
		
		public int waitProcess() throws InterruptedException { return proc.waitFor(); }
		public void wait_output() throws InterruptedException, ExecutionException { stdout.get(); stderr.get(); }
		
		public int wait_all() throws InterruptedException, ExecutionException { wait_output(); return waitProcess(); }
		
		public Process getProcess() { return proc; }
		
	}
}
