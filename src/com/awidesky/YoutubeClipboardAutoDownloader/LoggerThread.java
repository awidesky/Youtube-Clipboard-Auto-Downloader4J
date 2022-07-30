package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;


public class LoggerThread extends Thread {

	private PrintWriter logTo;
	private LinkedBlockingQueue<Runnable> loggerQueue = new LinkedBlockingQueue<>();
	
	private DateFormat datePrefix = null;
	private String prefix = null;
	
	public volatile boolean isStop = false;
	
	
	public static final String version = "v1.2.0";
	
	public LoggerThread() { 
		this(System.out, true, Charset.defaultCharset());
	}
	
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
		this(new PrintWriter(os, autoFlush, cs));
	}
	
	public LoggerThread(Writer wr) { 
		logTo = new PrintWriter(wr);
	}
	
	public void setDatePrefix(DateFormat datePrefix) {
		this.datePrefix = datePrefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void run() {

		logTo.println("LoggerThread " + version + " started at [" + new SimpleDateFormat("yyyy/MM/dd-kk:mm:ss").format(new Date()) + "]");
		
		while (true) {

			if (loggerQueue.isEmpty() && isStop) {
				break;
			}

			try {
				loggerQueue.take().run();
			} catch (InterruptedException e) {
				logTo.println("LoggerThread Interrupted! : " + e.getMessage());
				logTo.println("Closing logger..");
				break;
			}
		}
		
		logTo.close();

	}
	
	public void log(String data) {
		try {
			loggerQueue.put(getLogTask(data));
		} catch (InterruptedException e) {
			if(!isStop) log(e);
		}
	}
	
	public void log(Exception e) {
		try {
			loggerQueue.put(getLogTask(e));
		} catch (InterruptedException e1) {
			if(!isStop) log(e1);
		}
	}
	
	public void log(Object... objs) {
		try {
			loggerQueue.put(() -> {
				for(Object o : objs) getLogTask(o).run();
			});
		} catch (InterruptedException e) {
			if(!isStop) log(e);
		}
	}
	
	
	private Runnable getLogTask(String data) {
		return () -> {
			printPrefix();
			logTo.println(data.replaceAll("\\R", System.lineSeparator()));
		};
	}
	
	private Runnable getLogTask(Exception e) {
		return () -> {
			printPrefix();
			e.printStackTrace(logTo);
		};
	}
	
	private Runnable getLogTask(Object obj) {
		return getLogTask(obj.toString());
	}
	

	
	private void printPrefix() {
		if(datePrefix != null) logTo.print("[" + datePrefix.format(new Date()) + "] ");
		if(prefix != null) logTo.print(prefix);
	}
	
	
	
	/**
	 * Kill LoggerThread in <code>timeOut</code> ms.
	 * */
	public void kill(int timeOut) {
		
		isStop = true;
		
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
