package com.awidesky.YoutubeClipboardAutoDownloader.util;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

/**
 * An Simple Logger class that prints log to a <code>PrintWriter</code>.
 * 
 * @author Eugene Hong
 * */
public class SimpleLogger extends AbstractLogger {
	
	private PrintWriter logTo;
	
	public SimpleLogger() { 
		this(System.out, true, Charset.defaultCharset());
	}
	
	public SimpleLogger(OutputStream os) {
		this(os, true, Charset.defaultCharset());
	}
	
	public SimpleLogger(OutputStream os, Charset cs) {
		this(os, true, cs);
	}
	
	public SimpleLogger(OutputStream os, boolean autoFlush) {
		this(os, autoFlush, Charset.defaultCharset());
	}
	
	public SimpleLogger(OutputStream os, boolean autoFlush, Charset cs) {
		logTo = new PrintWriter(os, autoFlush, cs);
	}

	/**
	 * Prints a empty line without prefix
	 * */
	@Override
	public void newLine() {
		logTo.println();
	}

	/**
	 * Logs a String.
	 * Each lines will be printed with prefix.
	 * */
	@Override
	public void log(String data) {
		for(String line : data.split("\\R")) {
			printPrefix();
			logTo.println(line);
		}
	}

	@Override
	public void log(Object... objs) {
		Arrays.stream(objs).map(Object::toString).forEach(this::log);
	}
	
	private void printPrefix() {
		if(datePrefix != null) logTo.print("[" + datePrefix.format(new Date()) + "] ");
		if(prefix != null) logTo.print(prefix + " ");
	}

}
