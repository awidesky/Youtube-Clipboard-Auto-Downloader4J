package com.awidesky.YoutubeClipboardAutoDownloader.util;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;


public class LoggerThread extends Thread {

	private PrintWriter logTo;
	private	LinkedBlockingQueue<Consumer<PrintWriter>> loggerQueue = new LinkedBlockingQueue<>();
	private Set<TaskLogger> children = ConcurrentHashMap.newKeySet();
	
	public volatile boolean isStop = false;
	private boolean verbose = false;
	private DateFormat datePrefix = null;

	public static final String version = "v1.7.0";
	
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
		return getLogger(verbose, null);
	} 
	
	public TaskLogger getLogger(String prefix) {
		return getLogger(verbose, prefix);
	}
	public TaskLogger getLogger(boolean verbose, String prefix) {
		TaskLogger newLogger = new TaskLogger(verbose, prefix) {

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
			public void close() {
				children.remove(this);
			}
			
		};
		newLogger.setDatePrefix(datePrefix);
		children.add(newLogger);
		return newLogger;
	}
	
	public TaskBufferedLogger getBufferedLogger(String prefix) {
		return getBufferedLogger(verbose, prefix);
	}
	public TaskBufferedLogger getBufferedLogger(boolean verbose, String prefix) {
		TaskBufferedLogger newLogger = new TaskBufferedLogger(verbose, prefix) {

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
			public void close() {
				flush();
				children.remove(this);
			}
			
		};
		newLogger.setDatePrefix(datePrefix);
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
	
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	public void setVerboseAllChildren(boolean verbose) {
		this.verbose = verbose;
		children.parallelStream().forEach(l -> l.setVerbose(verbose));
	}
	public void setDatePrefix(DateFormat datePrefix) {
		this.datePrefix = datePrefix;
	}
	public void setDatePrefixAllChildren(DateFormat datePrefix) {
		this.datePrefix = datePrefix;
		children.parallelStream().forEach(l -> l.setDatePrefix(datePrefix));
	}
	
	/**
	 * Kill LoggerThread in <code>timeOut</code> ms.
	 * */
	public void kill(int timeOut) {
		
		isStop = true;
		
		children.parallelStream().forEach(TaskLogger::close);
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
