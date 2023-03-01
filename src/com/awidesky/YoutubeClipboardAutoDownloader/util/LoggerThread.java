package com.awidesky.YoutubeClipboardAutoDownloader.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;


public class LoggerThread extends Thread {

	private PrintWriter logTo;
	private	LinkedBlockingQueue<Consumer<PrintWriter>> loggerQueue = new LinkedBlockingQueue<>();
	private HashSet<TaskLogger> children = new HashSet<>();
	
	public volatile boolean isStop = false;
	private boolean verbose;
	
	
	public static final String version = "v1.5.0";
	
	public LoggerThread(OutputStream os) {
		this(os, true, Charset.defaultCharset());
	}
	
	public LoggerThread(OutputStream os, Charset cs) {
		this(os, true, cs);
	}
	
	public LoggerThread(OutputStream os, boolean autoFlush) {
		this(os, autoFlush, Charset.defaultCharset());
	}
	
	public LoggerThread(OutputStream os, boolean autoFlush, Charset cs) {
		logTo = new PrintWriter(os, autoFlush, cs);
	}
	
	public TaskLogger getLogger() {
		return getLogger(null);
	} 
	
	public TaskLogger getLogger(String prefix) {
		TaskLogger newLogger = new TaskLogger(verbose) {

			@Override
			public void queueLogTask(Consumer<PrintWriter> logTask) {
				try {
					loggerQueue.put(logTask);
				} catch (InterruptedException e) {
					if (!isStop) log(e);
				}
			}

			@Override
			public boolean runLogTask(Consumer<PrintWriter> logTask) {
				return loggerQueue.offer(logTask);
			}

			@Override
			public void close() throws IOException {
				children.remove(this);
			}
			
		};
		newLogger.setPrefix(prefix);
		children.add(newLogger);
		return newLogger;
	}
	public TaskBufferedLogger getBufferedLogger(String prefix) {
		TaskBufferedLogger newLogger = new TaskBufferedLogger(verbose) {

			@Override
			public void queueLogTask(Consumer<PrintWriter> logTask) {
				try {
					loggerQueue.put(logTask);
				} catch (InterruptedException e) {
					loggerQueue.offer(logTask);
					if (!isStop) log(e);
				}
			}

			@Override
			public void close() throws IOException {
				flush();
				children.remove(this);
			}
			
		};
		newLogger.setPrefix(prefix);
		children.add(newLogger);
		return newLogger;
	}
	
	@Override
	public void run() {

		logTo.println("LoggerThread " + version + " started at [" + new SimpleDateFormat("yyyy/MM/dd-kk:mm:ss").format(new Date()) + "]");
		
		while (true) {

			if (loggerQueue.isEmpty() && isStop) {
				break;	
			}

			try {
				loggerQueue.take().accept(logTo);
			} catch (InterruptedException e) {
				logTo.println("LoggerThread Interrupted! : " + e.getMessage());
				logTo.println("Closing logger..");
				break;
			}
		}
		
		logTo.close();

	}
	
	
	public void setVerboseAllChildren(boolean verbose) {
		this.verbose = verbose;
		children.parallelStream().forEach(l -> l.setVerbose(verbose));
	}
	
	
	/**
	 * Kill LoggerThread in <code>timeOut</code> ms.
	 * */
	public void kill(int timeOut) {
		
		isStop = true;
		
		children.parallelStream().filter(l -> l instanceof TaskBufferedLogger).forEach(l -> ((TaskBufferedLogger)l).flush());
		try {
			this.join(timeOut);
		} catch (InterruptedException e) {
			logTo.println("Failed to join logger thread!");
			e.printStackTrace(logTo);
		}
		
		this.interrupt();
		logTo.close();
		
	}
	
	
}
