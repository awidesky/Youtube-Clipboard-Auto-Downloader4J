package io.github.awidesky.YoutubeAudioAutoDownloader.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.function.Consumer;

public abstract class TaskLogger extends AbstractLogger {

	public volatile boolean isStop = false;
	
	public TaskLogger(boolean verbose, String prefix) {
		this.verbose = verbose;
		this.prefix = prefix;
	}
	

	/**
	 * queue a logging task.
	 * implementation would queue <code>logTask</code> to a worker thread 
	 * */
	abstract void queueLogTask(Consumer<PrintWriter> logTask);
	/**
	 * try to run a logging task <i><b>right away</b></i>.
	 * @return <code>true</code> if succeed to run the <code>logTask</code> right away
	 * */
	abstract boolean runLogTask(Consumer<PrintWriter> logTask);
	
	/**
	 * Prints a empty line without prefix
	 * */
	@Override
	public void newLine() {
		queueLogTask((logTo) -> {
			logTo.println();
		});
	}

	/**
	 * Logs a String.
	 * Each lines will be printed with prefix.
	 * */
	@Override
	public void log(String data) {
		queueLogTask(getLogTask(data));
	}
	
	public boolean logNow(String data) {
		return runLogTask(getLogTask(data));
	}
	public boolean logNow(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return logNow(sw.toString());
	}
	public boolean logNow(Object... objs) {
		return Arrays.stream(objs).map(Object::toString).allMatch(this::logNow);
	}
	
	
	protected Consumer<PrintWriter> getLogTask(String data) {
		return (logTo) -> {
			data.lines().forEach(l -> logTo.println(getPrefix() + l));
		};
	}
	
	
	/**
	 * TaskLogger just queue logs to LoggerThread, no Exception thrown
	 * */
	@Override
	public abstract void close();
}